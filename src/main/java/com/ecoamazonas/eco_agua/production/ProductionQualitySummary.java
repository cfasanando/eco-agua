package com.ecoamazonas.eco_agua.production;

import java.time.LocalDate;

public class ProductionQualitySummary {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final long totalOrders;
    private final long pendingOrders;
    private final long approvedOrders;
    private final long observedOrders;
    private final long rejectedOrders;

    public ProductionQualitySummary(
            LocalDate startDate,
            LocalDate endDate,
            long totalOrders,
            long pendingOrders,
            long approvedOrders,
            long observedOrders,
            long rejectedOrders
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOrders = totalOrders;
        this.pendingOrders = pendingOrders;
        this.approvedOrders = approvedOrders;
        this.observedOrders = observedOrders;
        this.rejectedOrders = rejectedOrders;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public long getApprovedOrders() {
        return approvedOrders;
    }

    public long getObservedOrders() {
        return observedOrders;
    }

    public long getRejectedOrders() {
        return rejectedOrders;
    }

    public long getReviewedOrders() {
        return approvedOrders + observedOrders + rejectedOrders;
    }

    public boolean isEmpty() {
        return totalOrders <= 0;
    }
}
