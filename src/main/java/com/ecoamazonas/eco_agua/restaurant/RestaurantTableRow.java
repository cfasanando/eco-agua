package com.ecoamazonas.eco_agua.restaurant;

public record RestaurantTableRow(
        Long id,
        String code,
        String name,
        String area,
        int seats,
        String status,
        boolean active,
        String notes
) {
    public String statusLabel() {
        return switch (safeStatus()) {
            case "OCCUPIED" -> "Ocupada";
            case "RESERVED" -> "Reservada";
            case "DISABLED" -> "Fuera de servicio";
            default -> "Libre";
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

    private String safeStatus() {
        return status == null ? "FREE" : status.toUpperCase();
    }
}
