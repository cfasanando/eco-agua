package com.ecoamazonas.eco_agua.delivery;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.client.ClientRepository;
import com.ecoamazonas.eco_agua.client.DocumentType;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.ReceivableService;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderPayment;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import com.ecoamazonas.eco_agua.order.SalesChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryImportService {

    private final DeliveryImportBatchRepository batchRepository;
    private final DeliveryImportStopRepository stopRepository;
    private final ClientRepository clientRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final ReceivableService receivableService;

    public DeliveryImportService(
            DeliveryImportBatchRepository batchRepository,
            DeliveryImportStopRepository stopRepository,
            ClientRepository clientRepository,
            SaleOrderRepository saleOrderRepository,
            ReceivableService receivableService
    ) {
        this.batchRepository = batchRepository;
        this.stopRepository = stopRepository;
        this.clientRepository = clientRepository;
        this.saleOrderRepository = saleOrderRepository;
        this.receivableService = receivableService;
    }

    @Transactional(readOnly = true)
    public List<DeliveryImportBatch> findRecentBatches() {
        return batchRepository.findTop20ByOrderByRouteDateDescIdDesc();
    }

    @Transactional(readOnly = true)
    public DeliveryImportBatch findBatch(Long id) {
        return batchRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Import batch not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<DeliveryImportStop> findStops(Long batchId) {
        return stopRepository.findByBatchIdOrderByRouteOrderIndexAscIdAsc(batchId);
    }

    @Transactional
    public DeliveryImportBatch importCsv(MultipartFile file, LocalDate routeDate, String title, String deliveryPerson) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo CSV.");
        }

        List<DeliveryImportStop> parsedStops = parseCsv(file);
        if (parsedStops.isEmpty()) {
            throw new IllegalArgumentException("El archivo no contiene paradas válidas.");
        }

        DeliveryImportBatch batch = new DeliveryImportBatch();
        batch.setRouteDate(routeDate != null ? routeDate : LocalDate.now());
        batch.setTitle(clean(title) != null ? clean(title) : "Ruta importada " + LocalDate.now());
        batch.setDeliveryPerson(clean(deliveryPerson));
        batch.setSourceFilename(file.getOriginalFilename());

        int index = 1;
        int located = 0;
        for (DeliveryImportStop stop : parsedStops) {
            stop.setRouteOrderIndex(index++);
            if (stop.hasLocation()) {
                located++;
            }
            batch.addStop(stop);
        }
        batch.setTotalStops(parsedStops.size());
        batch.setLocatedStops(located);
        batch.setMissingLocationStops(parsedStops.size() - located);
        return batchRepository.save(batch);
    }

    @Transactional
    public DeliveryImportStop updateStopStatus(Long stopId, DeliveryImportStopStatus status, String observation) {
        DeliveryImportStop stop = stopRepository.findById(stopId).orElseThrow(() -> new IllegalArgumentException("Parada no encontrada: " + stopId));
        if (status == null) {
            throw new IllegalArgumentException("Debe seleccionar un estado.");
        }
        stop.setStatus(status);
        stop.setStatusObservation(clean(observation));
        stop.setUpdatedAt(LocalDateTime.now());
        syncLinkedOrderDeliveryStatus(stop);
        return stopRepository.save(stop);
    }

    @Transactional
    public DeliveryImportStop linkOrCreateClient(Long stopId, boolean createIfMissing, boolean updateExisting) {
        DeliveryImportStop stop = findStopForWrite(stopId);
        Client client = resolveOrCreateClient(stop, createIfMissing, updateExisting);
        if (client == null) {
            throw new IllegalArgumentException("No se encontró cliente existente. Active la opción de crear cliente nuevo.");
        }
        stop.setClient(client);
        markIntegrated(stop, "Cliente vinculado: " + client.getName());
        return stopRepository.save(stop);
    }

    @Transactional
    public int linkOrCreateClientsForBatch(Long batchId, boolean createIfMissing, boolean updateExisting) {
        List<DeliveryImportStop> stops = findStops(batchId);
        int updated = 0;
        for (DeliveryImportStop stop : stops) {
            Client client = resolveOrCreateClient(stop, createIfMissing, updateExisting);
            if (client == null) {
                continue;
            }
            stop.setClient(client);
            markIntegrated(stop, "Cliente vinculado desde integración masiva.");
            stopRepository.save(stop);
            updated++;
        }
        return updated;
    }

    @Transactional
    public DeliveryImportStop createOrderFromStop(Long stopId, OrderStatus requestedStatus) {
        DeliveryImportStop stop = findStopForWrite(stopId);
        if (stop.getSaleOrder() != null) {
            return stop;
        }

        Client client = stop.getClient();
        if (client == null) {
            client = resolveOrCreateClient(stop, true, true);
            stop.setClient(client);
        }

        LocalDate routeDate = stop.getBatch() != null && stop.getBatch().getRouteDate() != null
                ? stop.getBatch().getRouteDate()
                : LocalDate.now();

        SaleOrder order = new SaleOrder();
        order.setClient(client);
        order.setOrderDate(routeDate);
        order.setOrderNumber((int) (saleOrderRepository.countByOrderDate(routeDate) + 1));
        order.setSalesChannel(SalesChannel.OTHER);
        order.setStatus(resolveOrderStatus(requestedStatus, stop.getAmount()));
        order.setDeliveryPerson(stop.getBatch() != null ? clean(stop.getBatch().getDeliveryPerson()) : null);
        order.setDeliveryOrderIndex(stop.getRouteOrderIndex());
        order.setDeliveryStatus(mapDeliveryStatus(stop.getStatus()));
        order.setTotalAmount(safeAmount(stop.getAmount()));
        order.setComment(buildOrderComment(stop));
        order.setDeliveryObservation(buildDeliveryObservation(stop));
        if (stop.getStatus() == DeliveryImportStopStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        SaleOrder savedOrder = saleOrderRepository.save(order);
        stop.setSaleOrder(savedOrder);
        markIntegrated(stop, "Pedido creado desde parada importada.");
        return stopRepository.save(stop);
    }

    @Transactional
    public int createOrdersForBatch(Long batchId, OrderStatus requestedStatus) {
        List<DeliveryImportStop> stops = findStops(batchId);
        int created = 0;
        for (DeliveryImportStop stop : stops) {
            if (stop.getSaleOrder() != null) {
                continue;
            }
            createOrderFromStop(stop.getId(), requestedStatus);
            created++;
        }
        return created;
    }

    @Transactional
    public DeliveryImportStop registerPaymentForStop(Long stopId, BigDecimal amount, String paymentMethod, String paymentReference) {
        DeliveryImportStop stop = findStopForWrite(stopId);
        if (stop.getSaleOrder() == null) {
            stop = createOrderFromStop(stopId, OrderStatus.CREDIT);
        }

        SaleOrder order = saleOrderRepository.findById(stop.getSaleOrder().getId())
                .orElseThrow(() -> new IllegalArgumentException("Pedido vinculado no encontrado."));

        if (order.getStatus() == OrderStatus.REQUESTED || order.getStatus() == OrderStatus.QUOTED) {
            order.setStatus(OrderStatus.CREDIT);
            saleOrderRepository.save(order);
        }

        BigDecimal amountToRegister = amount != null ? amount : stop.getAmount();
        if (amountToRegister == null || amountToRegister.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debe ingresar un monto mayor a cero.");
        }

        SaleOrderPayment payment = receivableService.registerPayment(
                order.getId(),
                LocalDate.now(),
                amountToRegister.setScale(2, RoundingMode.HALF_UP),
                clean(paymentMethod),
                clean(paymentReference),
                "Cobro registrado desde ruta importada #" + (stop.getBatch() != null ? stop.getBatch().getId() : "")
        );

        stop.setStatus(DeliveryImportStopStatus.DELIVERED);
        stop.setStatusObservation("Cobrado " + payment.getPaymentMethod() + " S/ " + payment.getAmount());
        markIntegrated(stop, "Cobro vinculado al pedido #" + order.getId());
        return stopRepository.save(stop);
    }

    public List<String> findPaymentMethods() {
        return List.of("EFECTIVO", "YAPE", "PLIN", "TRANSFERENCIA", "OTRO");
    }

    public long countClientLinkedStops(List<DeliveryImportStop> stops) {
        return stops == null ? 0 : stops.stream().filter(DeliveryImportStop::isClientLinked).count();
    }

    public long countOrderLinkedStops(List<DeliveryImportStop> stops) {
        return stops == null ? 0 : stops.stream().filter(DeliveryImportStop::isOrderLinked).count();
    }

    public BigDecimal calculateLinkedOrderAmount(List<DeliveryImportStop> stops) {
        if (stops == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return stops.stream()
                .filter(DeliveryImportStop::isOrderLinked)
                .map(DeliveryImportStop::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public String buildOpenStreetMapRouteUrl(List<DeliveryImportStop> stops) {
        if (stops == null) {
            return null;
        }
        List<DeliveryImportStop> located = stops.stream().filter(DeliveryImportStop::hasLocation).toList();
        if (located.size() < 2) {
            return null;
        }
        StringBuilder route = new StringBuilder();
        for (DeliveryImportStop stop : located) {
            if (!route.isEmpty()) {
                route.append(';');
            }
            route.append(stop.getLatitude()).append(',').append(stop.getLongitude());
        }
        return "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=" + route;
    }

    public BigDecimal calculateRouteDistance(List<DeliveryImportStop> stops) {
        if (stops == null || stops.size() < 2) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        double totalKm = 0D;
        DeliveryImportStop previous = null;
        for (DeliveryImportStop stop : stops) {
            if (stop == null || !stop.hasLocation()) {
                continue;
            }
            if (previous != null && previous.hasLocation()) {
                totalKm += haversineKm(
                        previous.getLatitude().doubleValue(),
                        previous.getLongitude().doubleValue(),
                        stop.getLatitude().doubleValue(),
                        stop.getLongitude().doubleValue()
                );
            }
            previous = stop;
        }
        return BigDecimal.valueOf(totalKm).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalAmount(List<DeliveryImportStop> stops) {
        if (stops == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return stops.stream()
                .map(DeliveryImportStop::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private DeliveryImportStop findStopForWrite(Long stopId) {
        return stopRepository.findById(stopId)
                .orElseThrow(() -> new IllegalArgumentException("Parada no encontrada: " + stopId));
    }

    private Client resolveOrCreateClient(DeliveryImportStop stop, boolean createIfMissing, boolean updateExisting) {
        Optional<Client> existing = findBestClientMatch(stop);
        if (existing.isPresent()) {
            Client client = existing.get();
            if (updateExisting) {
                updateClientFromStop(client, stop);
                client = clientRepository.save(client);
            }
            return client;
        }

        if (!createIfMissing) {
            return null;
        }

        Client client = new Client();
        client.setName(requiredClientName(stop));
        client.setDocType(DocumentType.NONE);
        client.setDocNumber("S/N");
        client.setActive(true);
        updateClientFromStop(client, stop);
        return clientRepository.save(client);
    }

    private Optional<Client> findBestClientMatch(DeliveryImportStop stop) {
        List<Client> clients = clientRepository.findByActiveTrueOrderByNameAsc();
        String stopPhone = normalizePhone(stop.getPhone());
        String stopName = normalizeText(stop.getClientName());
        String stopAddress = normalizeText(stop.getAddress());

        if (stopPhone != null) {
            Optional<Client> byPhone = clients.stream()
                    .filter(client -> stopPhone.equals(normalizePhone(client.getPhone())))
                    .findFirst();
            if (byPhone.isPresent()) {
                return byPhone;
            }
        }

        if (stopName != null) {
            Optional<Client> byName = clients.stream()
                    .filter(client -> stopName.equals(normalizeText(client.getName())))
                    .findFirst();
            if (byName.isPresent()) {
                return byName;
            }
        }

        if (stopName != null && stopAddress != null) {
            return clients.stream()
                    .filter(client -> stopName.equals(normalizeText(client.getName())))
                    .filter(client -> stopAddress.equals(normalizeText(client.getAddress())))
                    .findFirst();
        }

        return Optional.empty();
    }

    private void updateClientFromStop(Client client, DeliveryImportStop stop) {
        if (client == null || stop == null) {
            return;
        }
        String clientName = clean(stop.getClientName());
        if (client.getName() == null || client.getName().isBlank()) {
            client.setName(clientName);
        }
        if (clean(stop.getPhone()) != null) {
            client.setPhone(clean(stop.getPhone()));
        }
        if (clean(stop.getAddress()) != null) {
            client.setAddress(clean(stop.getAddress()));
        }
        if (clean(stop.getReference()) != null) {
            client.setReference(clean(stop.getReference()));
        }
        if (stop.getLatitude() != null && stop.getLongitude() != null) {
            client.setLatitude(stop.getLatitude());
            client.setLongitude(stop.getLongitude());
        }
    }

    private OrderStatus resolveOrderStatus(OrderStatus requestedStatus, BigDecimal amount) {
        if (requestedStatus != null && requestedStatus != OrderStatus.QUOTED && requestedStatus != OrderStatus.CANCELED) {
            return requestedStatus;
        }
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0 ? OrderStatus.CREDIT : OrderStatus.REQUESTED;
    }

    private DeliveryStatus mapDeliveryStatus(DeliveryImportStopStatus status) {
        if (status == null) {
            return DeliveryStatus.PENDING;
        }
        return switch (status) {
            case IN_ROUTE -> DeliveryStatus.IN_ROUTE;
            case DELIVERED -> DeliveryStatus.DELIVERED;
            case NOT_DELIVERED -> DeliveryStatus.NOT_DELIVERED;
            case RESCHEDULED -> DeliveryStatus.RESCHEDULED;
            case PENDING -> DeliveryStatus.PENDING;
        };
    }

    private void syncLinkedOrderDeliveryStatus(DeliveryImportStop stop) {
        if (stop == null || stop.getSaleOrder() == null || stop.getSaleOrder().getId() == null) {
            return;
        }
        SaleOrder order = saleOrderRepository.findById(stop.getSaleOrder().getId()).orElse(null);
        if (order == null) {
            return;
        }
        order.setDeliveryStatus(mapDeliveryStatus(stop.getStatus()));
        order.setDeliveryObservation(clean(stop.getStatusObservation()));
        if (stop.getStatus() == DeliveryImportStopStatus.DELIVERED && order.getDeliveredAt() == null) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        saleOrderRepository.save(order);
    }

    private void markIntegrated(DeliveryImportStop stop, String observation) {
        stop.setIntegratedAt(LocalDateTime.now());
        stop.setUpdatedAt(LocalDateTime.now());
        stop.setIntegrationObservation(clean(observation));
    }

    private String requiredClientName(DeliveryImportStop stop) {
        String name = clean(stop != null ? stop.getClientName() : null);
        if (name == null) {
            throw new IllegalArgumentException("La parada no tiene nombre de cliente.");
        }
        return name;
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildOrderComment(DeliveryImportStop stop) {
        StringBuilder builder = new StringBuilder("Pedido generado desde ruta importada");
        if (stop.getBatch() != null && stop.getBatch().getId() != null) {
            builder.append(" #").append(stop.getBatch().getId());
        }
        if (clean(stop.getObservation()) != null) {
            builder.append(". ").append(clean(stop.getObservation()));
        }
        return builder.toString();
    }

    private String buildDeliveryObservation(DeliveryImportStop stop) {
        StringBuilder builder = new StringBuilder();
        if (clean(stop.getReference()) != null) {
            builder.append("Referencia: ").append(clean(stop.getReference()));
        }
        if (clean(stop.getStatusObservation()) != null) {
            if (!builder.isEmpty()) {
                builder.append(". ");
            }
            builder.append(clean(stop.getStatusObservation()));
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private List<DeliveryImportStop> parseCsv(MultipartFile file) {
        List<DeliveryImportStop> stops = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    if (looksLikeHeader(line)) {
                        continue;
                    }
                }
                DeliveryImportStop stop = parseLine(line);
                if (stop != null) {
                    stops.add(stop);
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo CSV.");
        }
        return stops;
    }

    private DeliveryImportStop parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        List<String> columns = splitCsvLine(line);
        if (columns.isEmpty()) {
            return null;
        }
        String clientName = value(columns, 0);
        if (clientName == null) {
            return null;
        }

        DeliveryImportStop stop = new DeliveryImportStop();
        stop.setClientName(clientName);
        stop.setPhone(value(columns, 1));
        stop.setAddress(value(columns, 2));
        stop.setReference(value(columns, 3));
        stop.setAmount(parseAmount(value(columns, 4)));
        stop.setObservation(value(columns, 5));
        stop.setLatitude(parseCoordinate(value(columns, 6)));
        stop.setLongitude(parseCoordinate(value(columns, 7)));
        stop.setStatus(DeliveryImportStopStatus.PENDING);
        return stop;
    }

    private boolean looksLikeHeader(String line) {
        String normalized = line == null ? "" : line.toLowerCase();
        return normalized.contains("cliente") || normalized.contains("client") || normalized.contains("telefono") || normalized.contains("phone");
    }

    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if ((c == ',' || c == ';') && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private String value(List<String> values, int index) {
        if (values == null || values.size() <= index) {
            return null;
        }
        return clean(values.get(index));
    }

    private BigDecimal parseAmount(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.replace("S/", "").replace(",", ".").trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseCoordinate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", ".").trim()).setScale(7, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double earthRadiusKm = 6371D;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2D) * Math.sin(dLat / 2D)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2D) * Math.sin(dLng / 2D);
        double c = 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
        return earthRadiusKm * c;
    }

    private String normalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String digits = rawPhone.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private String normalizeText(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
