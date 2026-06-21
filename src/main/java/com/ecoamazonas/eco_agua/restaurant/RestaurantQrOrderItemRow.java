package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantQrOrderItemRow(
        Long id,
        Long qrOrderId,
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public BigDecimal safeUnitPrice() {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice;
    }

    public BigDecimal safeLineTotal() {
        return lineTotal == null ? BigDecimal.ZERO : lineTotal;
    }
}
