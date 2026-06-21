package com.ecoamazonas.eco_agua.restaurant;

public record RestaurantOrderItemStock(
        Long id,
        Long orderId,
        Long productId,
        String productName,
        int quantity,
        String stockControlMode
) {
    public String safeStockControlMode() {
        String clean = stockControlMode == null ? "PRODUCT" : stockControlMode.trim().toUpperCase();
        return switch (clean) {
            case "RECIPE", "NONE" -> clean;
            default -> "PRODUCT";
        };
    }
}
