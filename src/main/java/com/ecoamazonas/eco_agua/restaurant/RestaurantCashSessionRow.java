package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RestaurantCashSessionRow(
        Long id,
        LocalDate businessDate,
        String status,
        BigDecimal openingAmount,
        String openedBy,
        LocalDateTime openedAt,
        BigDecimal closingAmount,
        BigDecimal expectedCash,
        BigDecimal differenceAmount,
        String closedBy,
        LocalDateTime closedAt,
        String notes
) {
    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(status);
    }

    public String statusLabel() {
        return isOpen() ? "Abierta" : "Cerrada";
    }

    public BigDecimal safeOpeningAmount() {
        return safe(openingAmount);
    }

    public BigDecimal safeClosingAmount() {
        return safe(closingAmount);
    }

    public BigDecimal safeExpectedCash() {
        return safe(expectedCash);
    }

    public BigDecimal safeDifferenceAmount() {
        return safe(differenceAmount);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
