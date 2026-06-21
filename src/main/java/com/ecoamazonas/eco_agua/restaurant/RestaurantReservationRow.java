package com.ecoamazonas.eco_agua.restaurant;

import java.time.LocalDateTime;

public record RestaurantReservationRow(
        Long id,
        String reservationCode,
        Long tableId,
        String tableName,
        String tableArea,
        String customerName,
        String customerPhone,
        LocalDateTime reservationAt,
        int durationMinutes,
        int partySize,
        String status,
        String notes,
        Long orderId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public String safeStatus() {
        return status == null || status.isBlank() ? "PENDING" : status.toUpperCase();
    }

    public String statusLabel() {
        return switch (safeStatus()) {
            case "CONFIRMED" -> "Confirmada";
            case "ATTENDED" -> "Atendida";
            case "CANCELLED" -> "Cancelada";
            case "NO_SHOW" -> "No asistió";
            default -> "Pendiente";
        };
    }

    public String statusBadge() {
        return switch (safeStatus()) {
            case "CONFIRMED" -> "text-bg-primary";
            case "ATTENDED" -> "text-bg-success";
            case "CANCELLED" -> "text-bg-secondary";
            case "NO_SHOW" -> "text-bg-dark";
            default -> "text-bg-warning";
        };
    }

    public String tableLabel() {
        if (tableName != null && !tableName.isBlank()) {
            return tableName;
        }
        return tableId == null ? "Mesa sin asignar" : "Mesa " + tableId;
    }

    public String areaLabel() {
        return tableArea == null || tableArea.isBlank() ? "Sin área" : tableArea;
    }

    public String phoneLabel() {
        return customerPhone == null || customerPhone.isBlank() ? "Sin teléfono" : customerPhone;
    }

    public String notesLabel() {
        return notes == null || notes.isBlank() ? "Sin observaciones" : notes;
    }

    public String durationLabel() {
        if (durationMinutes < 60) {
            return durationMinutes + " min";
        }
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        return minutes == 0 ? hours + " h" : hours + " h " + minutes + " min";
    }

    public LocalDateTime endAt() {
        return reservationAt == null ? null : reservationAt.plusMinutes(Math.max(durationMinutes, 0));
    }

    public boolean isPending() {
        return "PENDING".equals(safeStatus());
    }

    public boolean isConfirmed() {
        return "CONFIRMED".equals(safeStatus());
    }

    public boolean isClosed() {
        return switch (safeStatus()) {
            case "ATTENDED", "CANCELLED", "NO_SHOW" -> true;
            default -> false;
        };
    }

    public boolean canEdit() {
        return !isClosed() && orderId == null;
    }

    public boolean canOpenOrder() {
        return orderId == null && (isPending() || isConfirmed());
    }

    public boolean hasOrder() {
        return orderId != null;
    }
}
