package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantReportValueRow(
        String key,
        String label,
        int orderCount,
        BigDecimal quantity,
        BigDecimal amount,
        BigDecimal estimatedCost,
        BigDecimal grossProfit
) {
    public BigDecimal safeQuantity() { return safe(quantity); }
    public BigDecimal safeAmount() { return safe(amount); }
    public BigDecimal safeEstimatedCost() { return safe(estimatedCost); }
    public BigDecimal safeGrossProfit() { return safe(grossProfit); }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
