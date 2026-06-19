package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RestaurantCashOrderRow(
        Long id,
        String orderCode,
        String serviceType,
        Long tableId,
        String tableName,
        String customerName,
        String customerPhone,
        String status,
        BigDecimal subtotal,
        String paymentMethod,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        int itemCount
) {
    public String serviceLabel() {
        return switch (safeServiceType()) {
            case "TAKEAWAY" -> "Para llevar";
            case "DELIVERY" -> "Delivery";
            default -> "Mesa / salón";
        };
    }

    public String serviceReference() {
        if (tableName != null && !tableName.isBlank()) {
            return tableName;
        }
        if (customerName != null && !customerName.isBlank()) {
            return customerName;
        }
        return switch (safeServiceType()) {
            case "TAKEAWAY" -> "Pedido para llevar";
            case "DELIVERY" -> "Pedido delivery";
            default -> "Sin mesa";
        };
    }

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

    public BigDecimal safeSubtotal() {
        return subtotal == null ? BigDecimal.ZERO : subtotal;
    }

    private String safeServiceType() {
        return serviceType == null ? "DINE_IN" : serviceType.toUpperCase();
    }

    private String safePaymentMethod() {
        return paymentMethod == null ? "OTHER" : paymentMethod.toUpperCase();
    }
}
