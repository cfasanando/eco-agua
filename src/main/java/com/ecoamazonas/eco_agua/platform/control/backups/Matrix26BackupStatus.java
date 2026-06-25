package com.ecoamazonas.eco_agua.platform.control.backups;

public enum Matrix26BackupStatus {
    PENDING("Pending", "text-bg-secondary"),
    VALIDATING("Validating", "text-bg-info"),
    RUNNING("Exporting", "text-bg-primary"),
    COMPRESSING("Compressing", "text-bg-primary"),
    VERIFYING("Verifying", "text-bg-warning"),
    COMPLETED("Completed", "text-bg-success"),
    FAILED("Failed", "text-bg-danger"),
    CANCELLED("Cancelled", "text-bg-secondary");

    private final String label;
    private final String badgeClass;

    Matrix26BackupStatus(String label, String badgeClass) {
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
