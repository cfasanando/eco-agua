package com.ecoamazonas.eco_agua.restaurant;

public record RestaurantOrderItemStock(
        Long id,
        Long orderId,
        Long productId,
        String productName,
        int quantity
) {
}
