package com.ecoamazonas.eco_agua.platform.control.backups;

public enum Matrix26BackupVerificationState {
    NOT_VERIFIED("Not verified", "text-bg-secondary"),
    VERIFYING("Verifying", "text-bg-info"),
    VERIFIED("Verified", "text-bg-success"),
    VERIFICATION_FAILED("Verification failed", "text-bg-danger"),
    KEY_UNAVAILABLE("Key unavailable", "text-bg-warning"),
    CORRUPTED("Corrupted", "text-bg-danger");

    private final String label;
    private final String badgeClass;

    Matrix26BackupVerificationState(String label, String badgeClass) {
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
