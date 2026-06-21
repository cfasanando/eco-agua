package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RestaurantRecipeItemRow(
        Long id,
        Long productId,
        Long ingredientId,
        String ingredientName,
        String unitCode,
        BigDecimal unitCost,
        BigDecimal quantity,
        boolean ingredientActive
) {
    public BigDecimal safeUnitCost() {
        return unitCost == null ? BigDecimal.ZERO : unitCost;
    }

    public BigDecimal safeQuantity() {
        return quantity == null ? BigDecimal.ZERO : quantity;
    }

    public BigDecimal lineCost() {
        return safeUnitCost().multiply(safeQuantity()).setScale(4, RoundingMode.HALF_UP);
    }

    public String unitCostDisplay() {
        return RestaurantDecimalFormat.preciseMoney(safeUnitCost());
    }

    public String quantityDisplay() {
        return RestaurantDecimalFormat.quantity(safeQuantity());
    }

    public String lineCostDisplay() {
        return RestaurantDecimalFormat.preciseMoney(lineCost());
    }

    public String unitAbbreviation() {
        return switch (unitCode == null ? "UNIT" : unitCode.toUpperCase()) {
            case "KG" -> "kg";
            case "G" -> "g";
            case "L" -> "L";
            case "ML" -> "ml";
            case "PORTION" -> "porción";
            default -> "unid.";
        };
    }

    public boolean hasCostIssue() {
        return safeUnitCost().compareTo(BigDecimal.ZERO) <= 0;
    }

    public boolean isIncomplete() {
        return !ingredientActive || safeQuantity().compareTo(BigDecimal.ZERO) <= 0 || hasCostIssue();
    }
}
