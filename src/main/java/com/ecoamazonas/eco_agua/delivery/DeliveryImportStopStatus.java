package com.ecoamazonas.eco_agua.delivery;

public enum DeliveryImportStopStatus {
    PENDING("Pendiente", "text-bg-secondary"),
    IN_ROUTE("En ruta", "text-bg-primary"),
    DELIVERED("Entregado", "text-bg-success"),
    NOT_DELIVERED("No entregado", "text-bg-danger"),
    RESCHEDULED("Reprogramado", "text-bg-warning");

    private final String label;
    private final String badgeClass;

    DeliveryImportStopStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
