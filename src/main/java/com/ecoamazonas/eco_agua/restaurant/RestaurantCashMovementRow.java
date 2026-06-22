package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RestaurantCashMovementRow(
        Long id,
        Long cashSessionId,
        String movementType,
        BigDecimal amount,
        String description,
        String createdBy,
        LocalDateTime createdAt
) {
    public String movementLabel() {
        return "EXPENSE".equalsIgnoreCase(movementType) ? "Egreso" : "Ingreso";
    }

    public boolean isExpense() {
        return "EXPENSE".equalsIgnoreCase(movementType);
    }

    public BigDecimal safeAmount() {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
