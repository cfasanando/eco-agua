package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class ProductionOverviewSummary {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final long totalOrders;
    private final long confirmedOrders;
    private final long draftOrders;
    private final long canceledOrders;
    private final BigDecimal confirmedQuantityExpected;
    private final BigDecimal confirmedQuantityProduced;
    private final BigDecimal confirmedQuantityLoss;
    private final BigDecimal confirmedInputCost;
    private final BigDecimal averageUnitCost;

    public ProductionOverviewSummary(
            LocalDate startDate,
            LocalDate endDate,
            long totalOrders,
            long confirmedOrders,
            long draftOrders,
            long canceledOrders,
            BigDecimal confirmedQuantityExpected,
            BigDecimal confirmedQuantityProduced,
            BigDecimal confirmedQuantityLoss,
            BigDecimal confirmedInputCost,
            BigDecimal averageUnitCost
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOrders = totalOrders;
        this.confirmedOrders = confirmedOrders;
        this.draftOrders = draftOrders;
        this.canceledOrders = canceledOrders;
        this.confirmedQuantityExpected = confirmedQuantityExpected != null ? confirmedQuantityExpected : BigDecimal.ZERO;
        this.confirmedQuantityProduced = confirmedQuantityProduced != null ? confirmedQuantityProduced : BigDecimal.ZERO;
        this.confirmedQuantityLoss = confirmedQuantityLoss != null ? confirmedQuantityLoss : BigDecimal.ZERO;
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

    public BigDecimal getConfirmedQuantityExpected() {
        return confirmedQuantityExpected;
    }

    public BigDecimal getConfirmedQuantityProduced() {
        return confirmedQuantityProduced;
    }

    public BigDecimal getConfirmedQuantityLoss() {
        return confirmedQuantityLoss;
    }

    public BigDecimal getConfirmedInputCost() {
        return confirmedInputCost;
    }

    public BigDecimal getAverageUnitCost() {
        return averageUnitCost;
    }

    public BigDecimal getLossRatePercent() {
        if (confirmedQuantityExpected == null || confirmedQuantityExpected.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return confirmedQuantityLoss.multiply(BigDecimal.valueOf(100))
                .divide(confirmedQuantityExpected, 2, RoundingMode.HALF_UP);
    }
}
