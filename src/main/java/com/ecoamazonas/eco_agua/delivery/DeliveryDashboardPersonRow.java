package com.ecoamazonas.eco_agua.delivery;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DeliveryDashboardPersonRow {
    private final String name;
    private long totalOrders;
    private long pendingCount;
    private long inRouteCount;
    private long deliveredCount;
    private long issueCount;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal pendingAmount = BigDecimal.ZERO;
    private BigDecimal collectedInRouteAmount = BigDecimal.ZERO;

    public DeliveryDashboardPersonRow(String name) {
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
        totalAmount = totalAmount.add(safe(row.getTotalAmount()));
        pendingAmount = pendingAmount.add(safe(row.getPendingAmount()));
    }

    public void addCollectedAmount(BigDecimal amount) {
        collectedInRouteAmount = collectedInRouteAmount.add(safe(amount));
    }

    public String getName() { return name; }
    public long getTotalOrders() { return totalOrders; }
    public long getPendingCount() { return pendingCount; }
    public long getInRouteCount() { return inRouteCount; }
    public long getDeliveredCount() { return deliveredCount; }
    public long getIssueCount() { return issueCount; }
    public BigDecimal getTotalAmount() { return money(totalAmount); }
    public BigDecimal getPendingAmount() { return money(pendingAmount); }
    public BigDecimal getCollectedInRouteAmount() { return money(collectedInRouteAmount); }

    public int getDeliveredProgressPercent() {
        if (totalOrders <= 0) {
            return 0;
        }
        return (int) Math.round((deliveredCount * 100D) / totalOrders);
    }

    private BigDecimal safe(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal money(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
