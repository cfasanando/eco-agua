package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record RestaurantIngredientMovementRow(
        Long id,
        Long ingredientId,
        String ingredientName,
        String unitCode,
        String movementType,
        BigDecimal quantityChange,
        BigDecimal balanceAfter,
        Long orderId,
        Long orderItemId,
        String notes,
        LocalDateTime createdAt
) {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public BigDecimal safeQuantityChange() {
        return quantityChange == null ? BigDecimal.ZERO : quantityChange;
    }

    public BigDecimal safeBalanceAfter() {
        return balanceAfter == null ? BigDecimal.ZERO : balanceAfter;
    }

    public String quantityDisplay() {
        BigDecimal absolute = safeQuantityChange().abs();
        String prefix = safeQuantityChange().signum() > 0 ? "+" : safeQuantityChange().signum() < 0 ? "-" : "";
        return prefix + RestaurantDecimalFormat.quantity(absolute);
    }

    public String balanceDisplay() {
        return RestaurantDecimalFormat.quantity(safeBalanceAfter());
    }

    public String movementLabel() {
        return switch (movementType == null ? "ADJUSTMENT" : movementType.toUpperCase()) {
            case "OPENING" -> "Stock inicial";
            case "REPLENISHMENT" -> "Reposición";
            case "CONSUMPTION" -> "Consumo por pedido";
            case "RETURN" -> "Devolución";
            default -> "Ajuste manual";
        };
    }

    public String movementBadge() {
        return switch (movementType == null ? "ADJUSTMENT" : movementType.toUpperCase()) {
            case "CONSUMPTION" -> "text-bg-danger";
            case "RETURN" -> "text-bg-info";
            case "REPLENISHMENT", "OPENING" -> "text-bg-success";
            default -> "text-bg-secondary";
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

    public String referenceLabel() {
        return orderId == null ? "Movimiento manual" : "Pedido #" + orderId;
    }

    public String createdAtDisplay() {
        return createdAt == null ? "-" : createdAt.format(DATE_TIME_FORMAT);
    }

    public String notesLabel() {
        return notes == null || notes.isBlank() ? "Sin observaciones" : notes;
    }
}
