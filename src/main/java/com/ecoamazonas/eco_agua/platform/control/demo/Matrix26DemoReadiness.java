package com.ecoamazonas.eco_agua.platform.control.demo;

public enum Matrix26DemoReadiness {
    READY("Listo para demo", "text-bg-success", "bi-check-circle-fill"),
    PREPARE_DATA("Falta data demo", "text-bg-warning", "bi-database-add"),
    INTERNAL_ONLY("Uso interno", "text-bg-info", "bi-shield-lock"),
    REVIEW("Revisar antes de vender", "text-bg-secondary", "bi-clipboard2-check"),
    HIDDEN("Oculto", "text-bg-dark", "bi-eye-slash");

    private final String label;
    private final String badgeClass;
    private final String iconClass;

    Matrix26DemoReadiness(String label, String badgeClass, String iconClass) {
        this.label = label;
        this.badgeClass = badgeClass;
        this.iconClass = iconClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getIconClass() {
        return iconClass;
    }
}
