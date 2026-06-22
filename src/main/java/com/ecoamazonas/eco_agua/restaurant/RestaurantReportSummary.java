package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RestaurantReportSummary(
        LocalDate fromDate,
        LocalDate toDate,
        int paidOrders,
        BigDecimal totalSales,
        BigDecimal estimatedCost,
        BigDecimal grossProfit,
        BigDecimal grossMarginPercent,
        int cancelledOrders,
        int approvedQrOrders,
        int rejectedQrOrders,
        BigDecimal averageKitchenMinutes
) {
    public BigDecimal safeTotalSales() { return safe(totalSales); }
    public BigDecimal safeEstimatedCost() { return safe(estimatedCost); }
    public BigDecimal safeGrossProfit() { return safe(grossProfit); }
    public BigDecimal safeGrossMarginPercent() { return safe(grossMarginPercent); }
    public BigDecimal safeAverageKitchenMinutes() { return safe(averageKitchenMinutes); }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
