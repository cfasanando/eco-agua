package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantOrderItemIngredientRow(
        Long id,
        Long orderItemId,
        Long ingredientId,
        String ingredientName,
        String unitCode,
        BigDecimal quantityPerUnit,
        BigDecimal quantityReserved
) {
    public BigDecimal safeQuantityPerUnit() {
        return quantityPerUnit == null ? BigDecimal.ZERO : quantityPerUnit;
    }

    public BigDecimal safeQuantityReserved() {
        return quantityReserved == null ? BigDecimal.ZERO : quantityReserved;
    }
}
