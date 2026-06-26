package com.ecoamazonas.eco_agua.platform.control.restores;

public enum Matrix26RestoreValidationStatus {
    RUNNING("Running", "text-bg-info"),
    VERIFIED("Restore verified", "text-bg-success"),
    VERIFIED_WITH_WARNINGS("Verified with warnings", "text-bg-warning"),
    MISMATCH("Restore mismatch", "text-bg-danger"),
    FAILED("Verification failed", "text-bg-danger");

    private final String label;
    private final String badgeClass;

    Matrix26RestoreValidationStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }
}
