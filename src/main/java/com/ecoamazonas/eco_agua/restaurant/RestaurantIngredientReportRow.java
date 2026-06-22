package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantIngredientReportRow(
        Long ingredientId,
        String ingredientName,
        String unitCode,
        BigDecimal consumedQuantity,
        BigDecimal returnedQuantity,
        BigDecimal estimatedCost
) {
    public BigDecimal safeConsumedQuantity() { return safe(consumedQuantity); }
    public BigDecimal safeReturnedQuantity() { return safe(returnedQuantity); }
    public BigDecimal safeEstimatedCost() { return safe(estimatedCost); }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
