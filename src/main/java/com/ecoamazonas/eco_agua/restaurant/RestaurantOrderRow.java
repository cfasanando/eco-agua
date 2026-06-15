package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RestaurantOrderRow(
        Long id,
        String orderCode,
        String serviceType,
        String tableName,
        String customerName,
        String customerPhone,
        String status,
        BigDecimal subtotal,
        String notes,
        LocalDateTime createdAt,
        int itemCount
) {
    public String serviceLabel() {
        return switch (safeServiceType()) {
            case "TAKEAWAY" -> "Para llevar";
            case "DELIVERY" -> "Delivery";
            default -> "Mesa / salón";
        };
    }

    public String statusLabel() {
        return switch (safeStatus()) {
            case "NEW" -> "Nueva";
            case "IN_KITCHEN" -> "En cocina";
            case "READY" -> "Lista";
            case "SERVED" -> "Servida";
            case "PAID" -> "Pagada";
            case "CANCELLED" -> "Anulada";
            default -> "Nueva";
        };
    }

    public String statusBadge() {
        return switch (safeStatus()) {
            case "IN_KITCHEN" -> "text-bg-warning";
            case "READY" -> "text-bg-info";
            case "SERVED" -> "text-bg-primary";
            case "PAID" -> "text-bg-success";
            case "CANCELLED" -> "text-bg-secondary";
            default -> "text-bg-light text-dark";
        };
    }

    private String safeStatus() {
        return status == null ? "NEW" : status.toUpperCase();
    }

    private String safeServiceType() {
        return serviceType == null ? "DINE_IN" : serviceType.toUpperCase();
    }
}
