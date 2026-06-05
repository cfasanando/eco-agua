package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductionTraceabilitySummary {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final long totalOrders;
    private final long confirmedOrders;
    private final long draftOrders;
    private final long canceledOrders;
    private final long approvedOrders;
    private final long pendingQualityOrders;
    private final long attentionQualityOrders;
    private final BigDecimal quantityExpected;
    private final BigDecimal quantityProduced;
    private final BigDecimal quantityLoss;
    private final BigDecimal lossRatePercent;

    public ProductionTraceabilitySummary(
            LocalDate startDate,
            LocalDate endDate,
            long totalOrders,
            long confirmedOrders,
            long draftOrders,
            long canceledOrders,
            long approvedOrders,
            long pendingQualityOrders,
            long attentionQualityOrders,
            BigDecimal quantityExpected,
            BigDecimal quantityProduced,
            BigDecimal quantityLoss,
            BigDecimal lossRatePercent
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOrders = totalOrders;
        this.confirmedOrders = confirmedOrders;
        this.draftOrders = draftOrders;
        this.canceledOrders = canceledOrders;
        this.approvedOrders = approvedOrders;
        this.pendingQualityOrders = pendingQualityOrders;
        this.attentionQualityOrders = attentionQualityOrders;
        this.quantityExpected = quantityExpected;
        this.quantityProduced = quantityProduced;
        this.quantityLoss = quantityLoss;
        this.lossRatePercent = lossRatePercent;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public long getTotalOrders() { return totalOrders; }
    public long getConfirmedOrders() { return confirmedOrders; }
    public long getDraftOrders() { return draftOrders; }
    public long getCanceledOrders() { return canceledOrders; }
    public long getApprovedOrders() { return approvedOrders; }
    public long getPendingQualityOrders() { return pendingQualityOrders; }
    public long getAttentionQualityOrders() { return attentionQualityOrders; }
    public BigDecimal getQuantityExpected() { return quantityExpected; }
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public BigDecimal getQuantityLoss() { return quantityLoss; }
    public BigDecimal getLossRatePercent() { return lossRatePercent; }
}
