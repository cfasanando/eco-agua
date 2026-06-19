package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantOrderItemRow(
        Long id,
        Long orderId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String kitchenStatus
) {
    public String kitchenStatusLabel() {
        return switch (safeKitchenStatus()) {
            case "READY" -> "Listo";
            case "CANCELLED" -> "Anulado";
            default -> "Pendiente";
        };
    }

    private String safeKitchenStatus() {
        return kitchenStatus == null ? "PENDING" : kitchenStatus.toUpperCase();
    }
}
