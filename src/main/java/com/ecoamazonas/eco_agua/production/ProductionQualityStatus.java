package com.ecoamazonas.eco_agua.production;

public enum ProductionQualityStatus {
    PENDING("Pendiente", "bg-warning text-dark"),
    APPROVED("Aprobado", "bg-success"),
    OBSERVED("Observado", "bg-info text-dark"),
    REJECTED("Rechazado", "bg-danger");

    private final String label;
    private final String badgeClass;

    ProductionQualityStatus(String label, String badgeClass) {
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
