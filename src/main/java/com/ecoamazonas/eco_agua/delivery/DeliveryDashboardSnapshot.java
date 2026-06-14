package com.ecoamazonas.eco_agua.delivery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class DeliveryDashboardSnapshot {
    private final LocalDate date;
    private final List<DeliveryDailyRow> rows;
    private final List<DeliveryDailyRow> locatedRows;
    private final List<DeliveryDailyRow> routeRows;
    private final DeliveryRouteSummary routeSummary;
    private final List<DeliveryDashboardPersonRow> personRows;
    private final List<DeliveryDashboardZoneRow> zoneRows;
    private final List<DeliveryDailyRow> issueRows;
    private final List<DeliveryDashboardActivityRow> recentActivities;
    private final long totalOrders;
    private final long pendingCount;
    private final long inRouteCount;
    private final long deliveredCount;
    private final long notDeliveredCount;
    private final long rescheduledCount;
    private final long canceledCount;
    private final long locatedCount;
    private final long missingLocationCount;
    private final BigDecimal totalAmount;
    private final BigDecimal pendingAmount;
    private final BigDecimal collectedInRouteAmount;

    public DeliveryDashboardSnapshot(
            LocalDate date,
            List<DeliveryDailyRow> rows,
            List<DeliveryDailyRow> locatedRows,
            List<DeliveryDailyRow> routeRows,
            DeliveryRouteSummary routeSummary,
            List<DeliveryDashboardPersonRow> personRows,
            List<DeliveryDashboardZoneRow> zoneRows,
            List<DeliveryDailyRow> issueRows,
            List<DeliveryDashboardActivityRow> recentActivities,
            long totalOrders,
            long pendingCount,
            long inRouteCount,
            long deliveredCount,
            long notDeliveredCount,
            long rescheduledCount,
            long canceledCount,
            long locatedCount,
            long missingLocationCount,
            BigDecimal totalAmount,
            BigDecimal pendingAmount,
            BigDecimal collectedInRouteAmount
    ) {
        this.date = date;
        this.rows = rows != null ? rows : List.of();
        this.locatedRows = locatedRows != null ? locatedRows : List.of();
        this.routeRows = routeRows != null ? routeRows : List.of();
        this.routeSummary = routeSummary;
        this.personRows = personRows != null ? personRows : List.of();
        this.zoneRows = zoneRows != null ? zoneRows : List.of();
        this.issueRows = issueRows != null ? issueRows : List.of();
        this.recentActivities = recentActivities != null ? recentActivities : List.of();
        this.totalOrders = totalOrders;
        this.pendingCount = pendingCount;
        this.inRouteCount = inRouteCount;
        this.deliveredCount = deliveredCount;
        this.notDeliveredCount = notDeliveredCount;
        this.rescheduledCount = rescheduledCount;
        this.canceledCount = canceledCount;
        this.locatedCount = locatedCount;
        this.missingLocationCount = missingLocationCount;
        this.totalAmount = money(totalAmount);
        this.pendingAmount = money(pendingAmount);
        this.collectedInRouteAmount = money(collectedInRouteAmount);
    }

    public LocalDate getDate() { return date; }
    public List<DeliveryDailyRow> getRows() { return rows; }
    public List<DeliveryDailyRow> getLocatedRows() { return locatedRows; }
    public List<DeliveryDailyRow> getRouteRows() { return routeRows; }
    public DeliveryRouteSummary getRouteSummary() { return routeSummary; }
    public List<DeliveryDashboardPersonRow> getPersonRows() { return personRows; }
    public List<DeliveryDashboardZoneRow> getZoneRows() { return zoneRows; }
    public List<DeliveryDailyRow> getIssueRows() { return issueRows; }
    public List<DeliveryDashboardActivityRow> getRecentActivities() { return recentActivities; }
    public long getTotalOrders() { return totalOrders; }
    public long getPendingCount() { return pendingCount; }
    public long getInRouteCount() { return inRouteCount; }
    public long getDeliveredCount() { return deliveredCount; }
    public long getNotDeliveredCount() { return notDeliveredCount; }
    public long getRescheduledCount() { return rescheduledCount; }
    public long getCanceledCount() { return canceledCount; }
    public long getLocatedCount() { return locatedCount; }
    public long getMissingLocationCount() { return missingLocationCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public BigDecimal getCollectedInRouteAmount() { return collectedInRouteAmount; }

    public long getOpenCount() {
        return pendingCount + inRouteCount + rescheduledCount;
    }

    public long getIssueCount() {
        return notDeliveredCount + rescheduledCount + canceledCount;
    }

    public int getDeliveredProgressPercent() {
        if (totalOrders <= 0) {
            return 0;
        }
        return (int) Math.round((deliveredCount * 100D) / totalOrders);
    }

    public int getLocatedProgressPercent() {
        if (totalOrders <= 0) {
            return 0;
        }
        return (int) Math.round((locatedCount * 100D) / totalOrders);
    }

    public int getCollectionProgressPercent() {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal collected = totalAmount.subtract(pendingAmount).max(BigDecimal.ZERO);
        return collected.multiply(BigDecimal.valueOf(100))
                .divide(totalAmount, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    public boolean hasRows() {
        return totalOrders > 0;
    }

    public boolean hasIssues() {
        return getIssueCount() > 0;
    }

    private BigDecimal money(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
