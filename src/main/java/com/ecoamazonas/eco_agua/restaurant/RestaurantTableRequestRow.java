package com.ecoamazonas.eco_agua.restaurant;

import java.time.Duration;
import java.time.LocalDateTime;

public record RestaurantTableRequestRow(
        Long id,
        Long tableId,
        String tableName,
        String tableArea,
        String requestType,
        String customerNote,
        String status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public String safeRequestType() {
        return requestType == null || requestType.isBlank() ? "ATTENTION" : requestType;
    }

    public String safeStatus() {
        return status == null || status.isBlank() ? "PENDING" : status;
    }

    public String tableLabel() {
        if (tableName != null && !tableName.isBlank()) {
            return tableName;
        }
        return tableId == null ? "Mesa no identificada" : "Mesa " + tableId;
    }

    public String areaLabel() {
        return tableArea == null || tableArea.isBlank() ? "Sin área" : tableArea;
    }

    public String typeLabel() {
        return switch (safeRequestType()) {
            case "BILL" -> "Pidió la cuenta";
            case "PAID_NOTICE" -> "Avisó que ya pagó";
            case "WAITER" -> "Llamó al mozo";
            case "NOTE" -> "Envió una nota";
            default -> "Solicita atención";
        };
    }

    public String typeBadge() {
        return switch (safeRequestType()) {
            case "BILL" -> "text-bg-success";
            case "PAID_NOTICE" -> "text-bg-primary";
            case "WAITER" -> "text-bg-warning";
            case "NOTE" -> "text-bg-info";
            default -> "text-bg-danger";
        };
    }

    public String iconClass() {
        return switch (safeRequestType()) {
            case "BILL" -> "bi-receipt";
            case "PAID_NOTICE" -> "bi-check2-circle";
            case "WAITER" -> "bi-person-raised-hand";
            case "NOTE" -> "bi-chat-left-text";
            default -> "bi-bell";
        };
    }

    public String statusLabel() {
        return "RESOLVED".equals(safeStatus()) ? "Atendido" : "Pendiente";
    }

    public String statusBadge() {
        return "RESOLVED".equals(safeStatus()) ? "text-bg-secondary" : "text-bg-danger";
    }

    public boolean isPending() {
        return "PENDING".equals(safeStatus());
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
