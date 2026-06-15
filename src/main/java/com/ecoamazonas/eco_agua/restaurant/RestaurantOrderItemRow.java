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
}
