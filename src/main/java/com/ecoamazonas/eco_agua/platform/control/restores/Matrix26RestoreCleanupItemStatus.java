package com.ecoamazonas.eco_agua.platform.control.restores;

public enum Matrix26RestoreCleanupItemStatus {
    PENDING("Pending", "text-bg-secondary"),
    APPROVED("Approved", "text-bg-warning"),
    RUNNING("Running", "text-bg-primary"),
    COMPLETED("Completed", "text-bg-success"),
    SKIPPED("Skipped", "text-bg-secondary"),
    BLOCKED("Blocked", "text-bg-danger"),
    FAILED("Failed", "text-bg-danger");

    private final String label;
    private final String badgeClass;

    Matrix26RestoreCleanupItemStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }
    public boolean finished() { return this == COMPLETED || this == SKIPPED; }
}
