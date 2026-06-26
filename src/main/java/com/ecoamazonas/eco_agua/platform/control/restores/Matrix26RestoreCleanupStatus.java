package com.ecoamazonas.eco_agua.platform.control.restores;

public enum Matrix26RestoreCleanupStatus {
    PREVIEW_READY("Preview ready", "text-bg-info"),
    BLOCKED("Blocked", "text-bg-danger"),
    APPROVED("Approved", "text-bg-warning"),
    RUNNING("Running", "text-bg-primary"),
    PARTIALLY_CLEANED("Partially cleaned", "text-bg-warning"),
    CLEANED("Cleaned", "text-bg-success"),
    FAILED("Failed", "text-bg-danger"),
    CANCELLED("Cancelled", "text-bg-secondary");

    private final String label;
    private final String badgeClass;

    Matrix26RestoreCleanupStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }
    public boolean executable() {
        return this == APPROVED || this == PARTIALLY_CLEANED || this == FAILED;
    }
}
