package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RestaurantOrderRow(
        Long id,
        String orderCode,
        String serviceType,
        Long tableId,
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

    public boolean isClosed() {
        return "PAID".equals(safeStatus()) || "CANCELLED".equals(safeStatus());
    }

    public boolean canEdit() {
        return !isClosed();
    }

    public boolean canSendToKitchen() {
        return "NEW".equals(safeStatus());
    }

    public boolean canMarkReady() {
        return "NEW".equals(safeStatus()) || "IN_KITCHEN".equals(safeStatus());
    }

    public boolean canMarkServed() {
        return "READY".equals(safeStatus());
    }

    public boolean canPay() {
        return "SERVED".equals(safeStatus()) || "READY".equals(safeStatus()) || "IN_KITCHEN".equals(safeStatus());
    }

    public boolean canCancel() {
        return !isClosed();
    }

    public BigDecimal safeSubtotal() {
        return subtotal == null ? BigDecimal.ZERO : subtotal;
    }

    public String safeStatus() {
        return status == null ? "NEW" : status.toUpperCase();
    }

    private String safeServiceType() {
        return serviceType == null ? "DINE_IN" : serviceType.toUpperCase();
    }
}
