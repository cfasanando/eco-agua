package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RestaurantTableBoardRow(
        Long id,
        String code,
        String name,
        String area,
        int seats,
        String status,
        boolean active,
        String notes,
        Long orderId,
        String orderCode,
        String orderStatus,
        BigDecimal orderSubtotal,
        String customerName,
        String customerPhone,
        LocalDateTime orderCreatedAt,
        Integer orderMinutes
) {
    public boolean hasActiveOrder() {
        return orderId != null;
    }

    public String statusLabel() {
        return switch (safeStatus()) {
            case "OCCUPIED" -> "Ocupada";
            case "RESERVED" -> "Reservada";
            case "DISABLED" -> "Fuera de servicio";
            default -> "Libre";
        };
    }

    public String orderStatusLabel() {
        return switch (safeOrderStatus()) {
            case "NEW" -> "Nueva";
            case "IN_KITCHEN" -> "En cocina";
            case "READY" -> "Lista";
            case "SERVED" -> "Servida";
            case "PAID" -> "Pagada";
            case "CANCELLED" -> "Anulada";
            default -> "Sin comanda";
        };
    }

    public String statusBadge() {
        return switch (safeStatus()) {
            case "OCCUPIED" -> "text-bg-danger";
            case "RESERVED" -> "text-bg-warning";
            case "DISABLED" -> "text-bg-secondary";
            default -> "text-bg-success";
        };
    }

    public String orderStatusBadge() {
        return switch (safeOrderStatus()) {
            case "IN_KITCHEN" -> "text-bg-warning";
            case "READY" -> "text-bg-info";
            case "SERVED" -> "text-bg-primary";
            case "PAID" -> "text-bg-success";
            case "CANCELLED" -> "text-bg-secondary";
            default -> "text-bg-light text-dark";
        };
    }

    public String cardBorderClass() {
        if (hasActiveOrder()) {
            return "border-danger";
        }
        return switch (safeStatus()) {
            case "RESERVED" -> "border-warning";
            case "DISABLED" -> "border-secondary";
            default -> "border-success";
        };
    }

    public String waitingLabel() {
        int minutes = orderMinutes == null ? 0 : Math.max(orderMinutes, 0);
        if (minutes < 60) {
            return minutes + " min";
        }
        int hours = minutes / 60;
        int remaining = minutes % 60;
        return remaining == 0 ? hours + " h" : hours + " h " + remaining + " min";
    }

    public BigDecimal safeOrderSubtotal() {
        return orderSubtotal == null ? BigDecimal.ZERO : orderSubtotal;
    }

    private String safeStatus() {
        return status == null ? "FREE" : status.toUpperCase();
    }

    private String safeOrderStatus() {
        return orderStatus == null ? "" : orderStatus.toUpperCase();
    }
}
