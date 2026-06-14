package com.ecoamazonas.eco_agua.delivery;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DeliveryDashboardZoneRow {
    private final String name;
    private long totalOrders;
    private long pendingCount;
    private long inRouteCount;
    private long deliveredCount;
    private long issueCount;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public DeliveryDashboardZoneRow(String name) {
        this.name = name;
    }

    public void addOrder(DeliveryDailyRow row) {
        totalOrders++;
        if (row.getDeliveryStatus() == DeliveryStatus.PENDING) {
            pendingCount++;
        } else if (row.getDeliveryStatus() == DeliveryStatus.IN_ROUTE) {
            inRouteCount++;
        } else if (row.getDeliveryStatus() == DeliveryStatus.DELIVERED) {
            deliveredCount++;
        } else if (row.getDeliveryStatus() == DeliveryStatus.NOT_DELIVERED
                || row.getDeliveryStatus() == DeliveryStatus.RESCHEDULED
                || row.getDeliveryStatus() == DeliveryStatus.CANCELED) {
            issueCount++;
        }
        totalAmount = totalAmount.add(row.getTotalAmount() != null ? row.getTotalAmount() : BigDecimal.ZERO);
    }

    public String getName() { return name; }
    public long getTotalOrders() { return totalOrders; }
    public long getPendingCount() { return pendingCount; }
    public long getInRouteCount() { return inRouteCount; }
    public long getDeliveredCount() { return deliveredCount; }
    public long getIssueCount() { return issueCount; }
    public BigDecimal getTotalAmount() { return totalAmount.setScale(2, RoundingMode.HALF_UP); }

    public int getDeliveredProgressPercent() {
        if (totalOrders <= 0) {
            return 0;
        }
        return (int) Math.round((deliveredCount * 100D) / totalOrders);
    }
}
