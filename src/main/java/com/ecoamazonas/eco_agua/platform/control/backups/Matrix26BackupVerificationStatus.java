package com.ecoamazonas.eco_agua.platform.control.backups;

public enum Matrix26BackupVerificationStatus {
    PASSED("Passed", "text-bg-success"),
    FAILED("Failed", "text-bg-danger"),
    WARNING("Warning", "text-bg-warning"),
    SKIPPED("Skipped", "text-bg-secondary");

    private final String label;
    private final String badgeClass;

    Matrix26BackupVerificationStatus(String label, String badgeClass) {
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
