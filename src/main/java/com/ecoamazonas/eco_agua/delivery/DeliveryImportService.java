package com.ecoamazonas.eco_agua.delivery;

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

@Service
public class DeliveryImportService {

    private final DeliveryImportBatchRepository batchRepository;
    private final DeliveryImportStopRepository stopRepository;

    public DeliveryImportService(DeliveryImportBatchRepository batchRepository, DeliveryImportStopRepository stopRepository) {
        this.batchRepository = batchRepository;
        this.stopRepository = stopRepository;
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
        return stopRepository.save(stop);
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

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
