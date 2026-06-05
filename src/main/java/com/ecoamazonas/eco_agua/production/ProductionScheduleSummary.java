package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductionScheduleSummary {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final long totalOrders;
    private final long draftOrders;
    private final long confirmedOrders;
    private final long canceledOrders;
    private final long pendingQualityOrders;
    private final long attentionQualityOrders;
    private final long approvedQualityOrders;
    private final long todayOrders;
    private final long nextSevenDaysOrders;
    private final long overdueDraftOrders;
    private final BigDecimal plannedQuantityExpected;
    private final BigDecimal confirmedQuantityProduced;
    private final BigDecimal confirmedQuantityLoss;

    public ProductionScheduleSummary(
            LocalDate startDate,
            LocalDate endDate,
            long totalOrders,
            long draftOrders,
            long confirmedOrders,
            long canceledOrders,
            long pendingQualityOrders,
            long attentionQualityOrders,
            long approvedQualityOrders,
            long todayOrders,
            long nextSevenDaysOrders,
            long overdueDraftOrders,
            BigDecimal plannedQuantityExpected,
            BigDecimal confirmedQuantityProduced,
            BigDecimal confirmedQuantityLoss
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOrders = totalOrders;
        this.draftOrders = draftOrders;
        this.confirmedOrders = confirmedOrders;
        this.canceledOrders = canceledOrders;
        this.pendingQualityOrders = pendingQualityOrders;
        this.attentionQualityOrders = attentionQualityOrders;
        this.approvedQualityOrders = approvedQualityOrders;
        this.todayOrders = todayOrders;
        this.nextSevenDaysOrders = nextSevenDaysOrders;
        this.overdueDraftOrders = overdueDraftOrders;
        this.plannedQuantityExpected = plannedQuantityExpected;
        this.confirmedQuantityProduced = confirmedQuantityProduced;
        this.confirmedQuantityLoss = confirmedQuantityLoss;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public long getTotalOrders() { return totalOrders; }
    public long getDraftOrders() { return draftOrders; }
    public long getConfirmedOrders() { return confirmedOrders; }
    public long getCanceledOrders() { return canceledOrders; }
    public long getPendingQualityOrders() { return pendingQualityOrders; }
    public long getAttentionQualityOrders() { return attentionQualityOrders; }
    public long getApprovedQualityOrders() { return approvedQualityOrders; }
    public long getTodayOrders() { return todayOrders; }
    public long getNextSevenDaysOrders() { return nextSevenDaysOrders; }
    public long getOverdueDraftOrders() { return overdueDraftOrders; }
    public BigDecimal getPlannedQuantityExpected() { return plannedQuantityExpected; }
    public BigDecimal getConfirmedQuantityProduced() { return confirmedQuantityProduced; }
    public BigDecimal getConfirmedQuantityLoss() { return confirmedQuantityLoss; }

    public boolean isEmpty() {
        return totalOrders <= 0;
    }
}
