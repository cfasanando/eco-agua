package com.ecoamazonas.eco_agua.platform.control.restores;

public enum Matrix26InPlaceRestoreStepStatus {
    PENDING("Pending", "text-bg-secondary"),
    RUNNING("Running", "text-bg-info"),
    COMPLETED("Completed", "text-bg-success"),
    FAILED("Failed", "text-bg-danger"),
    SKIPPED("Skipped", "text-bg-secondary");
    private final String label;
    private final String badgeClass;
    Matrix26InPlaceRestoreStepStatus(String label, String badgeClass) { this.label = label; this.badgeClass = badgeClass; }
    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }
}
