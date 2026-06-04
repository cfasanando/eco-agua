package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductionOverviewSummary {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final long totalOrders;
    private final long confirmedOrders;
    private final long draftOrders;
    private final long canceledOrders;
    private final BigDecimal confirmedQuantityProduced;
    private final BigDecimal confirmedInputCost;
    private final BigDecimal averageUnitCost;

    public ProductionOverviewSummary(
            LocalDate startDate,
            LocalDate endDate,
            long totalOrders,
            long confirmedOrders,
            long draftOrders,
            long canceledOrders,
            BigDecimal confirmedQuantityProduced,
            BigDecimal confirmedInputCost,
            BigDecimal averageUnitCost
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOrders = totalOrders;
        this.confirmedOrders = confirmedOrders;
        this.draftOrders = draftOrders;
        this.canceledOrders = canceledOrders;
        this.confirmedQuantityProduced = confirmedQuantityProduced != null ? confirmedQuantityProduced : BigDecimal.ZERO;
        this.confirmedInputCost = confirmedInputCost != null ? confirmedInputCost : BigDecimal.ZERO;
        this.averageUnitCost = averageUnitCost != null ? averageUnitCost : BigDecimal.ZERO;
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

    public long getConfirmedOrders() {
        return confirmedOrders;
    }

    public long getDraftOrders() {
        return draftOrders;
    }

    public long getCanceledOrders() {
        return canceledOrders;
    }

    public BigDecimal getConfirmedQuantityProduced() {
        return confirmedQuantityProduced;
    }

    public BigDecimal getConfirmedInputCost() {
        return confirmedInputCost;
    }

    public BigDecimal getAverageUnitCost() {
        return averageUnitCost;
    }
}
