package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantIngredientRow(
        Long id,
        String name,
        String unitCode,
        BigDecimal unitCost,
        BigDecimal stock,
        BigDecimal minimumStock,
        boolean active,
        String notes
) {
    public BigDecimal safeUnitCost() {
        return unitCost == null ? BigDecimal.ZERO : unitCost;
    }

    public BigDecimal safeStock() {
        return stock == null ? BigDecimal.ZERO : stock;
    }

    public BigDecimal safeMinimumStock() {
        return minimumStock == null ? BigDecimal.ZERO : minimumStock;
    }

    public String unitCostDisplay() {
        return RestaurantDecimalFormat.preciseMoney(safeUnitCost());
    }

    public String stockDisplay() {
        return RestaurantDecimalFormat.quantity(safeStock());
    }

    public String minimumStockDisplay() {
        return RestaurantDecimalFormat.quantity(safeMinimumStock());
    }

    public String unitLabel() {
        return switch (unitCode == null ? "UNIT" : unitCode.toUpperCase()) {
            case "KG" -> "Kilogramo";
            case "G" -> "Gramo";
            case "L" -> "Litro";
            case "ML" -> "Mililitro";
            case "PORTION" -> "Porción";
            default -> "Unidad";
        };
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

    public boolean isOutOfStock() {
        return safeStock().compareTo(BigDecimal.ZERO) <= 0;
    }

    public boolean isLowStock() {
        return !isOutOfStock()
                && safeMinimumStock().compareTo(BigDecimal.ZERO) > 0
                && safeStock().compareTo(safeMinimumStock()) <= 0;
    }

    public String stockStatusLabel() {
        if (!active) {
            return "Inactivo";
        }
        if (isOutOfStock()) {
            return "Agotado";
        }
        if (isLowStock()) {
            return "Stock bajo";
        }
        return "Stock OK";
    }

    public String stockStatusBadge() {
        if (!active) {
            return "text-bg-secondary";
        }
        if (isOutOfStock()) {
            return "text-bg-danger";
        }
        if (isLowStock()) {
            return "text-bg-warning";
        }
        return "text-bg-success";
    }

    public String notesLabel() {
        return notes == null || notes.isBlank() ? "Sin observaciones" : notes;
    }
}
