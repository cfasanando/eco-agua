package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public record RestaurantQrOrderRow(
        Long id,
        Long tableId,
        String tableName,
        String tableArea,
        String customerNote,
        String status,
        BigDecimal subtotal,
        Long approvedOrderId,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        int itemCount
) {
    public String tableLabel() {
        if (tableName != null && !tableName.isBlank()) {
            return tableName;
        }
        return tableId == null ? "Mesa no identificada" : "Mesa " + tableId;
    }

    public String areaLabel() {
        return tableArea == null || tableArea.isBlank() ? "Sin área" : tableArea;
    }

    public String safeStatus() {
        return status == null || status.isBlank() ? "PENDING" : status.toUpperCase();
    }

    public String statusLabel() {
        return switch (safeStatus()) {
            case "APPROVED" -> "Aprobado";
            case "REJECTED" -> "Rechazado";
            default -> "Pendiente";
        };
    }

    public String statusBadge() {
        return switch (safeStatus()) {
            case "APPROVED" -> "text-bg-success";
            case "REJECTED" -> "text-bg-secondary";
            default -> "text-bg-warning";
        };
    }

    public boolean isPending() {
        return "PENDING".equals(safeStatus());
    }

    public BigDecimal safeSubtotal() {
        return subtotal == null ? BigDecimal.ZERO : subtotal;
    }

    public String customerNoteLabel() {
        return customerNote == null || customerNote.isBlank() ? "Sin nota" : customerNote;
    }

    public String ageLabel() {
        if (createdAt == null) {
            return "sin fecha";
        }
        long minutes = Math.max(0, Duration.between(createdAt, LocalDateTime.now()).toMinutes());
        if (minutes < 1) {
            return "ahora";
        }
        if (minutes < 60) {
            return "hace " + minutes + " min";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return "hace " + hours + " h";
        }
        long days = hours / 24;
        return "hace " + days + " día" + (days == 1 ? "" : "s");
    }
}
