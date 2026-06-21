package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RestaurantExternalOrderRow(
        Long id,
        String orderCode,
        String serviceType,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        String deliveryReference,
        LocalDateTime scheduledAt,
        BigDecimal deliveryFee,
        String status,
        BigDecimal subtotal,
        String paymentMethod,
        LocalDateTime paidAt,
        String notes,
        LocalDateTime createdAt,
        int itemCount
) {
    public String serviceLabel() {
        return isDelivery() ? "Delivery" : "Para llevar";
    }

    public String statusLabel() {
        if (isPaid() && "DELIVERED".equals(safeStatus())) {
            return "Entregado y pagado";
        }
        return switch (safeStatus()) {
            case "NEW" -> "Pendiente";
            case "CONFIRMED" -> "Confirmado";
            case "IN_KITCHEN" -> "En preparación";
            case "READY" -> "Listo";
            case "OUT_FOR_DELIVERY" -> "En reparto";
            case "DELIVERED" -> "Entregado";
            case "PAID" -> "Pagado";
            case "CANCELLED" -> "Cancelado";
            default -> "Pendiente";
        };
    }

    public String statusBadge() {
        return switch (safeStatus()) {
            case "CONFIRMED" -> "text-bg-primary";
            case "IN_KITCHEN" -> "text-bg-warning";
            case "READY" -> "text-bg-info";
            case "OUT_FOR_DELIVERY" -> "text-bg-dark";
            case "DELIVERED", "PAID" -> "text-bg-success";
            case "CANCELLED" -> "text-bg-secondary";
            default -> "text-bg-light text-dark";
        };
    }

    public String paymentLabel() {
        return switch (safePaymentMethod()) {
            case "CASH" -> "Efectivo";
            case "CARD" -> "Tarjeta";
            case "YAPE" -> "Yape";
            case "PLIN" -> "Plin";
            case "TRANSFER" -> "Transferencia";
            case "OTHER" -> "Otro";
            default -> "Pendiente";
        };
    }

    public boolean isDelivery() {
        return "DELIVERY".equals(safeServiceType());
    }

    public boolean isTakeaway() {
        return "TAKEAWAY".equals(safeServiceType());
    }

    public boolean canConfirm() {
        return "NEW".equals(safeStatus());
    }

    public boolean canSendToKitchen() {
        return "CONFIRMED".equals(safeStatus());
    }

    public boolean canMarkReady() {
        return "IN_KITCHEN".equals(safeStatus());
    }

    public boolean canDispatch() {
        return isDelivery() && "READY".equals(safeStatus());
    }

    public boolean canDeliver() {
        return (isTakeaway() && "READY".equals(safeStatus()))
                || (isDelivery() && "OUT_FOR_DELIVERY".equals(safeStatus()));
    }

    public boolean canPay() {
        if (isPaid()) {
            return false;
        }
        return "READY".equals(safeStatus())
                || "OUT_FOR_DELIVERY".equals(safeStatus())
                || "DELIVERED".equals(safeStatus());
    }

    public boolean isPaid() {
        return paidAt != null;
    }

    public boolean canCancel() {
        return !isPaid() && switch (safeStatus()) {
            case "NEW", "CONFIRMED", "IN_KITCHEN", "READY" -> true;
            default -> false;
        };
    }

    public boolean isClosed() {
        return "PAID".equals(safeStatus())
                || "CANCELLED".equals(safeStatus())
                || ("DELIVERED".equals(safeStatus()) && isPaid());
    }

    public BigDecimal safeSubtotal() {
        return subtotal == null ? BigDecimal.ZERO : subtotal;
    }

    public BigDecimal safeDeliveryFee() {
        return deliveryFee == null ? BigDecimal.ZERO : deliveryFee;
    }

    public BigDecimal safeTotal() {
        return safeSubtotal().add(safeDeliveryFee());
    }

    public String safeStatus() {
        return status == null ? "NEW" : status.toUpperCase();
    }

    private String safeServiceType() {
        return serviceType == null ? "TAKEAWAY" : serviceType.toUpperCase();
    }

    private String safePaymentMethod() {
        return paymentMethod == null ? "" : paymentMethod.toUpperCase();
    }
}
