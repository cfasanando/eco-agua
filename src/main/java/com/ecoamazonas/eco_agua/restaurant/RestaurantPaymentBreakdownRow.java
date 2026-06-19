package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantPaymentBreakdownRow(
        String paymentMethod,
        int orderCount,
        BigDecimal totalAmount
) {
    public String paymentLabel() {
        return switch (safePaymentMethod()) {
            case "CASH" -> "Efectivo";
            case "CARD" -> "Tarjeta";
            case "YAPE" -> "Yape";
            case "PLIN" -> "Plin";
            case "TRANSFER" -> "Transferencia";
            default -> "Otro";
        };
    }

    public String badgeClass() {
        return switch (safePaymentMethod()) {
            case "CASH" -> "text-bg-success";
            case "YAPE", "PLIN" -> "text-bg-primary";
            case "CARD" -> "text-bg-info";
            case "TRANSFER" -> "text-bg-warning";
            default -> "text-bg-secondary";
        };
    }

    public BigDecimal safeTotalAmount() {
        return totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    private String safePaymentMethod() {
        return paymentMethod == null ? "OTHER" : paymentMethod.toUpperCase();
    }
}
