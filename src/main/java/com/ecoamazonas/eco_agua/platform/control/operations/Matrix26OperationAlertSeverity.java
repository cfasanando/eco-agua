package com.ecoamazonas.eco_agua.platform.control.operations;

public enum Matrix26OperationAlertSeverity {
    CRITICAL("Critical", "text-bg-danger", "bi-exclamation-octagon"),
    HIGH("High", "text-bg-danger", "bi-exclamation-triangle-fill"),
    MEDIUM("Medium", "text-bg-warning", "bi-exclamation-triangle"),
    LOW("Low", "text-bg-info", "bi-info-circle"),
    INFO("Info", "text-bg-secondary", "bi-info-circle");

    private final String label;
    private final String badgeClass;
    private final String iconClass;

    Matrix26OperationAlertSeverity(String label, String badgeClass, String iconClass) {
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

    public static Matrix26OperationAlertSeverity fromDashboard(String value) {
        if (value == null || value.isBlank()) {
            return INFO;
        }
        return switch (value.trim().toUpperCase()) {
            case "CRITICAL" -> CRITICAL;
            case "WARNING" -> MEDIUM;
            case "SUCCESS" -> INFO;
            case "HIGH" -> HIGH;
            case "LOW" -> LOW;
            default -> INFO;
        };
    }
}
