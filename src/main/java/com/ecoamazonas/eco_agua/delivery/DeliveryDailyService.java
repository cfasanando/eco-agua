package com.ecoamazonas.eco_agua.delivery;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.ReceivableService;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderPayment;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeliveryDailyService {

    private final SaleOrderRepository saleOrderRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;
    private final SaleOrderDeliveryEventRepository deliveryEventRepository;
    private final ReceivableService receivableService;

    public DeliveryDailyService(
            SaleOrderRepository saleOrderRepository,
            DeliveryZoneRepository deliveryZoneRepository,
            SaleOrderDeliveryEventRepository deliveryEventRepository,
            ReceivableService receivableService
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.deliveryZoneRepository = deliveryZoneRepository;
        this.deliveryEventRepository = deliveryEventRepository;
        this.receivableService = receivableService;
    }

    @Transactional(readOnly = true)
    public List<DeliveryDailyRow> findRows(LocalDate date, String deliveryPerson, DeliveryStatus deliveryStatus) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        List<SaleOrder> orders = saleOrderRepository.findDeliveryOrdersForDate(effectiveDate);

        return orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELED && order.getStatus() != OrderStatus.QUOTED)
                .filter(order -> deliveryPerson == null || deliveryPerson.isBlank() || safeEqualsIgnoreCase(order.getDeliveryPerson(), deliveryPerson))
                .filter(order -> deliveryStatus == null || order.getDeliveryStatus() == deliveryStatus)
                .sorted(Comparator.comparing(SaleOrder::getDeliveryOrderIndex, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SaleOrder::getOrderNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SaleOrder::getId))
                .map(order -> new DeliveryDailyRow(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getOrderDate(),
                        order.getClient() != null ? order.getClient().getName() : "-",
                        order.getClient() != null ? order.getClient().getPhone() : null,
                        order.getClient() != null ? order.getClient().getAddress() : null,
                        order.getClient() != null ? order.getClient().getReference() : null,
                        order.getDeliveryZone() != null ? order.getDeliveryZone().getName() : null,
                        order.getDeliveryPerson(),
                        order.getDeliveryStatus(),
                        order.getTotalAmount(),
                        order.getPaidAmount(),
                        order.getPendingAmount(),
                        order.getStatusLabel(),
                        order.getBorrowedBottles(),
                        order.getClient() != null ? order.getClient().getLatitude() : null,
                        order.getClient() != null ? order.getClient().getLongitude() : null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SaleOrder findDetailedOrder(Long id) {
        return saleOrderRepository.findDeliveryOrderById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<SaleOrderDeliveryEvent> findEvents(Long orderId) {
        return deliveryEventRepository.findBySaleOrderIdOrderByEventDateDescIdDesc(orderId);
    }

    @Transactional(readOnly = true)
    public List<DeliveryZone> findZones() {
        return deliveryZoneRepository.findAllByOrderByNameAsc();
    }

    public List<DeliveryDailyRow> filterRouteRows(List<DeliveryDailyRow> rows, DeliveryStatus selectedDeliveryStatus) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
                .filter(DeliveryDailyRow::hasLocation)
                .filter(row -> selectedDeliveryStatus != null || row.getDeliveryStatus() == DeliveryStatus.PENDING || row.getDeliveryStatus() == DeliveryStatus.IN_ROUTE)
                .toList();
    }

    public List<DeliveryDailyRow> buildSuggestedRoute(List<DeliveryDailyRow> rows) {
        List<DeliveryDailyRow> locatedRows = rows == null
                ? List.of()
                : rows.stream().filter(DeliveryDailyRow::hasLocation).collect(Collectors.toCollection(ArrayList::new));

        if (locatedRows.size() < 3) {
            return locatedRows;
        }

        List<DeliveryDailyRow> pending = new ArrayList<>(locatedRows);
        List<DeliveryDailyRow> ordered = new ArrayList<>();

        double currentLat = -3.743673;
        double currentLng = -73.251632;

        while (!pending.isEmpty()) {
            DeliveryDailyRow next = findNearest(currentLat, currentLng, pending);
            ordered.add(next);
            pending.remove(next);
            currentLat = next.getLatitude().doubleValue();
            currentLng = next.getLongitude().doubleValue();
        }

        return ordered;
    }

    public DeliveryRouteSummary buildRouteSummary(List<DeliveryDailyRow> allRows, List<DeliveryDailyRow> routeRows) {
        int totalStops = routeRows != null ? routeRows.size() : 0;
        int locatedStops = routeRows != null ? (int) routeRows.stream().filter(DeliveryDailyRow::hasLocation).count() : 0;
        int missingStops = allRows != null ? (int) allRows.stream().filter(row -> !row.hasLocation()).count() : 0;
        BigDecimal distance = calculateRouteDistance(routeRows);
        int estimatedMinutes = estimateRouteMinutes(distance, locatedStops);
        return new DeliveryRouteSummary(totalStops, locatedStops, missingStops, distance, estimatedMinutes);
    }

    public BigDecimal calculateRouteDistance(List<DeliveryDailyRow> rows) {
        if (rows == null || rows.size() < 2) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        double totalKm = 0D;
        DeliveryDailyRow previous = null;
        for (DeliveryDailyRow row : rows) {
            if (row == null || !row.hasLocation()) {
                continue;
            }
            if (previous != null && previous.hasLocation()) {
                totalKm += haversineKm(
                        previous.getLatitude().doubleValue(),
                        previous.getLongitude().doubleValue(),
                        row.getLatitude().doubleValue(),
                        row.getLongitude().doubleValue()
                );
            }
            previous = row;
        }

        return BigDecimal.valueOf(totalKm).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public int saveRouteOrder(List<Long> orderIds, String deliveryPerson) {
        if (orderIds == null || orderIds.isEmpty()) {
            return 0;
        }

        Map<Long, SaleOrder> ordersById = new HashMap<>();
        saleOrderRepository.findAllById(orderIds).forEach(order -> ordersById.put(order.getId(), order));

        int index = 1;
        int updated = 0;
        String cleanDeliveryPerson = clean(deliveryPerson);

        for (Long orderId : orderIds) {
            SaleOrder order = ordersById.get(orderId);
            if (order == null) {
                continue;
            }

            order.setDeliveryOrderIndex(index++);
            if (cleanDeliveryPerson != null) {
                order.setDeliveryPerson(cleanDeliveryPerson);
            }
            saleOrderRepository.save(order);
            registerEvent(order, DeliveryEventType.NOTE, "Orden de ruta diaria actualizado.");
            updated++;
        }

        return updated;
    }

    @Transactional
    public SaleOrder updateRoute(Long orderId, Long deliveryZoneId, Integer deliveryOrderIndex, String deliveryPerson) {
        SaleOrder order = saleOrderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        DeliveryZone zone = null;
        if (deliveryZoneId != null) {
            zone = deliveryZoneRepository.findById(deliveryZoneId).orElseThrow(() -> new IllegalArgumentException("Delivery zone not found: " + deliveryZoneId));
        }

        order.setDeliveryZone(zone);
        order.setDeliveryOrderIndex(deliveryOrderIndex);
        order.setDeliveryPerson(clean(deliveryPerson));
        saleOrderRepository.save(order);
        registerEvent(order, DeliveryEventType.NOTE, "Asignación de ruta actualizada.");
        return order;
    }

    public List<String> findPaymentMethods() {
        return List.of("EFECTIVO", "YAPE", "PLIN", "TRANSFERENCIA", "OTRO");
    }

    public List<String> findIncidentReasons() {
        return List.of(
                "Cliente no contestó",
                "Cliente ausente",
                "Dirección incorrecta",
                "Cliente reprogramó",
                "Pedido rechazado",
                "Falta de stock",
                "Zona no atendida",
                "Otro"
        );
    }

    @Transactional public SaleOrder markInRoute(Long orderId, String observation) { return updateDeliveryStatus(orderId, DeliveryStatus.IN_ROUTE, DeliveryEventType.IN_ROUTE, observation, false); }
    @Transactional public SaleOrder markDelivered(Long orderId, String observation) { return updateDeliveryStatus(orderId, DeliveryStatus.DELIVERED, DeliveryEventType.DELIVERED, observation, true); }
    @Transactional public SaleOrder markNotDelivered(Long orderId, String observation) { return updateDeliveryStatus(orderId, DeliveryStatus.NOT_DELIVERED, DeliveryEventType.NOT_DELIVERED, observation, false); }
    @Transactional public SaleOrder markRescheduled(Long orderId, String observation) { return updateDeliveryStatus(orderId, DeliveryStatus.RESCHEDULED, DeliveryEventType.RESCHEDULED, observation, false); }
    @Transactional public SaleOrder markCanceled(Long orderId, String observation) { return updateDeliveryStatus(orderId, DeliveryStatus.CANCELED, DeliveryEventType.CANCELED, observation, false); }

    @Transactional
    public SaleOrder registerDeliveryOutcome(
            Long orderId,
            DeliveryStatus deliveryStatus,
            String observation,
            String incidentReason,
            String proofReference,
            BigDecimal paymentAmount,
            String paymentMethod,
            String paymentReference
    ) {
        DeliveryEventType eventType = eventTypeForStatus(deliveryStatus);
        boolean setDeliveredAt = deliveryStatus == DeliveryStatus.DELIVERED;
        SaleOrder order = updateDeliveryStatusWithDetails(
                orderId,
                deliveryStatus,
                eventType,
                observation,
                setDeliveredAt,
                incidentReason,
                proofReference,
                paymentAmount,
                paymentMethod,
                paymentReference
        );

        if (hasPositivePayment(paymentAmount)) {
            BigDecimal normalizedAmount = paymentAmount.setScale(2, RoundingMode.HALF_UP);
            SaleOrderPayment payment = receivableService.registerPayment(
                    orderId,
                    LocalDate.now(),
                    normalizedAmount,
                    paymentMethod,
                    paymentReference,
                    buildPaymentObservation(observation, proofReference)
            );
            registerEvent(
                    order,
                    DeliveryEventType.PAYMENT,
                    "Cobro registrado en ruta.",
                    null,
                    proofReference,
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    payment.getReference()
            );
        }

        return saleOrderRepository.findDeliveryOrderById(orderId).orElse(order);
    }

    public String buildWhatsappUrl(SaleOrder order) {
        if (order == null || order.getClient() == null) {
            return null;
        }

        String normalizedPhone = normalizePhone(order.getClient().getPhone());
        if (normalizedPhone == null) {
            return null;
        }

        String message = "Hola, te escribimos para coordinar la entrega de tu pedido #"
                + (order.getOrderNumber() != null ? order.getOrderNumber() : order.getId())
                + ".";
        return "https://wa.me/" + normalizedPhone + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    public String buildGoogleMapsUrl(SaleOrder order) {
        if (order == null || order.getClient() == null) {
            return null;
        }

        Client client = order.getClient();
        if (client.getLatitude() == null || client.getLongitude() == null) {
            return null;
        }

        return "https://www.google.com/maps/search/?api=1&query=" + client.getLatitude() + "," + client.getLongitude();
    }

    public String buildOpenStreetMapUrl(SaleOrder order) {
        if (order == null || order.getClient() == null) {
            return null;
        }

        Client client = order.getClient();
        if (client.getLatitude() == null || client.getLongitude() == null) {
            return null;
        }

        return "https://www.openstreetmap.org/?mlat=" + client.getLatitude()
                + "&mlon=" + client.getLongitude()
                + "#map=18/" + client.getLatitude() + "/" + client.getLongitude();
    }

    public String buildOpenStreetMapRouteUrl(List<DeliveryDailyRow> rows) {
        if (rows == null) {
            return null;
        }

        List<DeliveryDailyRow> locatedRows = rows.stream()
                .filter(DeliveryDailyRow::hasLocation)
                .toList();

        if (locatedRows.size() < 2) {
            return null;
        }

        String route = locatedRows.stream()
                .map(row -> row.getLatitude() + "," + row.getLongitude())
                .collect(Collectors.joining(";"));

        return "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=" + route;
    }

    private SaleOrder updateDeliveryStatus(Long orderId, DeliveryStatus deliveryStatus, DeliveryEventType eventType, String observation, boolean setDeliveredAt) {
        return updateDeliveryStatusWithDetails(orderId, deliveryStatus, eventType, observation, setDeliveredAt, null, null, null, null, null);
    }

    private SaleOrder updateDeliveryStatusWithDetails(
            Long orderId,
            DeliveryStatus deliveryStatus,
            DeliveryEventType eventType,
            String observation,
            boolean setDeliveredAt,
            String incidentReason,
            String proofReference,
            BigDecimal paymentAmount,
            String paymentMethod,
            String paymentReference
    ) {
        SaleOrder order = saleOrderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setDeliveryStatus(deliveryStatus);
        order.setDeliveryObservation(clean(observation));

        if (setDeliveredAt) {
            order.setDeliveredAt(LocalDateTime.now());
        } else if (deliveryStatus != DeliveryStatus.DELIVERED) {
            order.setDeliveredAt(null);
        }

        saleOrderRepository.save(order);
        registerEvent(order, eventType, observation, incidentReason, proofReference, paymentAmount, paymentMethod, paymentReference);
        return order;
    }

    private void registerEvent(SaleOrder order, DeliveryEventType eventType, String observation) {
        registerEvent(order, eventType, observation, null, null, null, null, null);
    }

    private void registerEvent(
            SaleOrder order,
            DeliveryEventType eventType,
            String observation,
            String incidentReason,
            String proofReference,
            BigDecimal paymentAmount,
            String paymentMethod,
            String paymentReference
    ) {
        SaleOrderDeliveryEvent event = new SaleOrderDeliveryEvent();
        event.setSaleOrderId(order.getId());
        event.setEventDate(LocalDateTime.now());
        event.setEventType(eventType);
        event.setObservation(clean(observation));
        event.setIncidentReason(clean(incidentReason));
        event.setProofReference(clean(proofReference));
        event.setPaymentAmount(hasPositivePayment(paymentAmount) ? paymentAmount.setScale(2, RoundingMode.HALF_UP) : null);
        event.setPaymentMethod(clean(paymentMethod));
        event.setPaymentReference(clean(paymentReference));
        event.setContainersDeliveredSnapshot(0);
        event.setContainersReturnedSnapshot(0);
        event.setDeliveryPersonSnapshot(order.getDeliveryPerson());
        deliveryEventRepository.save(event);
    }

    private DeliveryEventType eventTypeForStatus(DeliveryStatus deliveryStatus) {
        if (deliveryStatus == null) {
            throw new IllegalArgumentException("Delivery status is required.");
        }

        return switch (deliveryStatus) {
            case IN_ROUTE -> DeliveryEventType.IN_ROUTE;
            case DELIVERED -> DeliveryEventType.DELIVERED;
            case NOT_DELIVERED -> DeliveryEventType.NOT_DELIVERED;
            case RESCHEDULED -> DeliveryEventType.RESCHEDULED;
            case CANCELED -> DeliveryEventType.CANCELED;
            case PENDING -> throw new IllegalArgumentException("Pending status cannot be registered as a delivery outcome.");
        };
    }

    private boolean hasPositivePayment(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private String buildPaymentObservation(String observation, String proofReference) {
        String cleanObservation = clean(observation);
        String cleanProof = clean(proofReference);
        if (cleanProof == null) {
            return cleanObservation != null ? cleanObservation : "Cobro registrado en ruta.";
        }

        String base = cleanObservation != null ? cleanObservation : "Cobro registrado en ruta.";
        return base + " Evidencia: " + cleanProof;
    }

    private DeliveryDailyRow findNearest(double latitude, double longitude, List<DeliveryDailyRow> rows) {
        return rows.stream()
                .min(Comparator.comparing(row -> haversineKm(latitude, longitude, row.getLatitude().doubleValue(), row.getLongitude().doubleValue())))
                .orElse(rows.get(0));
    }

    private int estimateRouteMinutes(BigDecimal distanceKm, int stopCount) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0 || stopCount <= 0) {
            return 0;
        }

        double drivingMinutes = distanceKm.doubleValue() / 20D * 60D;
        double serviceMinutes = stopCount * 4D;
        return (int) Math.ceil(drivingMinutes + serviceMinutes);
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

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private boolean safeEqualsIgnoreCase(String left, String right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return left.equalsIgnoreCase(right);
    }

    private String normalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }

        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() == 9) {
            return "51" + digits;
        }

        return digits;
    }
}
