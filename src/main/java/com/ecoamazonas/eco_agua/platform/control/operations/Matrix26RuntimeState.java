package com.ecoamazonas.eco_agua.platform.control.operations;

public enum Matrix26RuntimeState {
    ONLINE("Online", "text-bg-success", "bi-check-circle-fill"),
    OFFLINE("Offline", "text-bg-secondary", "bi-stop-circle"),
    PORT_OCCUPIED("Port occupied", "text-bg-danger", "bi-exclamation-octagon-fill"),
    PROCESS_FOUND("Process detected", "text-bg-info", "bi-cpu-fill"),
    PROCESS_NOT_FOUND("Process not detected", "text-bg-secondary", "bi-cpu"),
    CONFIGURATION_MISSING("Configuration missing", "text-bg-warning", "bi-file-earmark-excel-fill"),
    RUNTIME_MISSING("Runtime missing", "text-bg-warning", "bi-folder-x"),
    LOG_MISSING("Log unavailable", "text-bg-warning", "bi-file-earmark-x"),
    DEGRADED("Degraded", "text-bg-warning", "bi-exclamation-triangle-fill"),
    UNKNOWN("Unknown", "text-bg-dark", "bi-question-circle-fill");

    private final String label;
    private final String badgeClass;
    private final String iconClass;

    Matrix26RuntimeState(String label, String badgeClass, String iconClass) {
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
