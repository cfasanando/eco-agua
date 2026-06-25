package com.ecoamazonas.eco_agua.platform.control.operations;

public enum Matrix26RuntimeOperationStatus {
    REQUESTED("Requested", "text-bg-secondary"),
    RUNNING("Running", "text-bg-info"),
    COMPLETED("Completed", "text-bg-success"),
    RECOVERED("Recovered", "text-bg-success"),
    INTERRUPTED("Interrupted", "text-bg-warning"),
    BLOCKED("Blocked", "text-bg-warning"),
    STOP_TIMEOUT("Stop timeout", "text-bg-danger"),
    FAILED("Failed", "text-bg-danger");

    private final String label;
    private final String badgeClass;

    Matrix26RuntimeOperationStatus(String label, String badgeClass) {
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
