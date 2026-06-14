package com.ecoamazonas.eco_agua.delivery;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeliveryDashboardService {

    private final DeliveryDailyService deliveryDailyService;
    private final SaleOrderDeliveryEventRepository deliveryEventRepository;

    public DeliveryDashboardService(
            DeliveryDailyService deliveryDailyService,
            SaleOrderDeliveryEventRepository deliveryEventRepository
    ) {
        this.deliveryDailyService = deliveryDailyService;
        this.deliveryEventRepository = deliveryEventRepository;
    }

    @Transactional(readOnly = true)
    public DeliveryDashboardSnapshot buildSnapshot(LocalDate date, String deliveryPerson) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        List<DeliveryDailyRow> rows = deliveryDailyService.findRows(effectiveDate, deliveryPerson, null);
        List<DeliveryDailyRow> locatedRows = rows.stream()
                .filter(DeliveryDailyRow::hasLocation)
                .toList();
        List<DeliveryDailyRow> routeRows = deliveryDailyService.buildSuggestedRoute(deliveryDailyService.filterRouteRows(rows, null));
        DeliveryRouteSummary routeSummary = deliveryDailyService.buildRouteSummary(rows, routeRows);
        List<SaleOrderDeliveryEvent> events = findEventsForDate(effectiveDate);

        long pendingCount = countByStatus(rows, DeliveryStatus.PENDING);
        long inRouteCount = countByStatus(rows, DeliveryStatus.IN_ROUTE);
        long deliveredCount = countByStatus(rows, DeliveryStatus.DELIVERED);
        long notDeliveredCount = countByStatus(rows, DeliveryStatus.NOT_DELIVERED);
        long rescheduledCount = countByStatus(rows, DeliveryStatus.RESCHEDULED);
        long canceledCount = countByStatus(rows, DeliveryStatus.CANCELED);

        List<DeliveryDashboardPersonRow> personRows = buildPersonRows(rows, events);
        List<DeliveryDashboardZoneRow> zoneRows = buildZoneRows(rows);
        List<DeliveryDailyRow> issueRows = rows.stream()
                .filter(this::isIssueStatus)
                .limit(10)
                .toList();
        List<DeliveryDashboardActivityRow> recentActivities = events.stream()
                .limit(15)
                .map(DeliveryDashboardActivityRow::new)
                .toList();

        return new DeliveryDashboardSnapshot(
                effectiveDate,
                rows,
                locatedRows,
                routeRows,
                routeSummary,
                personRows,
                zoneRows,
                issueRows,
                recentActivities,
                rows.size(),
                pendingCount,
                inRouteCount,
                deliveredCount,
                notDeliveredCount,
                rescheduledCount,
                canceledCount,
                locatedRows.size(),
                rows.size() - locatedRows.size(),
                sumTotalAmount(rows),
                sumPendingAmount(rows),
                sumCollectedInRoute(events)
        );
    }

    private List<SaleOrderDeliveryEvent> findEventsForDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return deliveryEventRepository.findDashboardEventsBetween(start, end);
    }

    private List<DeliveryDashboardPersonRow> buildPersonRows(List<DeliveryDailyRow> rows, List<SaleOrderDeliveryEvent> events) {
        Map<String, DeliveryDashboardPersonRow> people = new LinkedHashMap<>();
        rows.forEach(row -> people.computeIfAbsent(personKey(row.getDeliveryPerson()), DeliveryDashboardPersonRow::new).addOrder(row));
        events.stream()
                .filter(event -> event.getEventType() == DeliveryEventType.PAYMENT)
                .filter(SaleOrderDeliveryEvent::hasPaymentInfo)
                .forEach(event -> people.computeIfAbsent(personKey(event.getDeliveryPersonSnapshot()), DeliveryDashboardPersonRow::new)
                        .addCollectedAmount(event.getPaymentAmount()));

        return people.values().stream()
                .sorted(Comparator.comparing(DeliveryDashboardPersonRow::getTotalOrders).reversed()
                        .thenComparing(DeliveryDashboardPersonRow::getName))                
                .toList();
    }

    private List<DeliveryDashboardZoneRow> buildZoneRows(List<DeliveryDailyRow> rows) {
        Map<String, DeliveryDashboardZoneRow> zones = new LinkedHashMap<>();
        rows.forEach(row -> zones.computeIfAbsent(zoneKey(row.getZoneName()), DeliveryDashboardZoneRow::new).addOrder(row));
        return zones.values().stream()
                .sorted(Comparator.comparing(DeliveryDashboardZoneRow::getTotalOrders).reversed()
                        .thenComparing(DeliveryDashboardZoneRow::getName))
                .toList();
    }

    private long countByStatus(List<DeliveryDailyRow> rows, DeliveryStatus status) {
        return rows.stream().filter(row -> row.getDeliveryStatus() == status).count();
    }

    private boolean isIssueStatus(DeliveryDailyRow row) {
        return row.getDeliveryStatus() == DeliveryStatus.NOT_DELIVERED
                || row.getDeliveryStatus() == DeliveryStatus.RESCHEDULED
                || row.getDeliveryStatus() == DeliveryStatus.CANCELED;
    }

    private BigDecimal sumTotalAmount(List<DeliveryDailyRow> rows) {
        return rows.stream()
                .map(DeliveryDailyRow::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPendingAmount(List<DeliveryDailyRow> rows) {
        return rows.stream()
                .map(DeliveryDailyRow::getPendingAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumCollectedInRoute(List<SaleOrderDeliveryEvent> events) {
        return events.stream()
                .filter(event -> event.getEventType() == DeliveryEventType.PAYMENT)
                .map(SaleOrderDeliveryEvent::getPaymentAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String personKey(String name) {
        return name != null && !name.isBlank() ? name.trim() : "Sin responsable";
    }

    private String zoneKey(String name) {
        return name != null && !name.isBlank() ? name.trim() : "Sin zona";
    }
}
