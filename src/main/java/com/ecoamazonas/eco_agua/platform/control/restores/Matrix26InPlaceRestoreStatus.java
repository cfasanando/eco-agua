package com.ecoamazonas.eco_agua.platform.control.restores;

public enum Matrix26InPlaceRestoreStatus {
    DRAFT("Draft", "text-bg-secondary"),
    PRECHECKING("Prechecking", "text-bg-info"),
    SAFETY_BACKUP_RUNNING("Safety backup", "text-bg-info"),
    SAFETY_BACKUP_VERIFIED("Safety backup verified", "text-bg-info"),
    PREPARING_STAGING_DATABASE("Preparing staging database", "text-bg-info"),
    IMPORTING_STAGING_DATABASE("Importing staging database", "text-bg-info"),
    VERIFYING_STAGING_DATABASE("Verifying staging database", "text-bg-info"),
    PREPARING_STAGING_FILES("Preparing staging files", "text-bg-info"),
    READY_TO_SWITCH("Ready to switch", "text-bg-warning"),
    STOPPING_RUNTIME("Stopping runtime", "text-bg-warning"),
    SWITCHING_DATABASE("Switching database", "text-bg-warning"),
    SWITCHING_FILES("Switching files", "text-bg-warning"),
    STARTING_RUNTIME("Starting runtime", "text-bg-info"),
    HEALTH_CHECKING("Health checking", "text-bg-info"),
    AWAITING_CONFIRMATION("Awaiting confirmation", "text-bg-warning"),
    COMPLETED("Completed", "text-bg-success"),
    ROLLBACK_RUNNING("Rollback running", "text-bg-warning"),
    ROLLED_BACK("Rolled back", "text-bg-secondary"),
    FAILED("Failed", "text-bg-danger"),
    MANUAL_RECOVERY_REQUIRED("Manual recovery required", "text-bg-danger"),
    CANCELLED("Cancelled", "text-bg-secondary");

    private final String label;
    private final String badgeClass;
    Matrix26InPlaceRestoreStatus(String label, String badgeClass) { this.label = label; this.badgeClass = badgeClass; }
    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }
    public boolean canSwitch() { return this == READY_TO_SWITCH; }
    public boolean canConfirm() { return this == AWAITING_CONFIRMATION; }
    public boolean canRollback() { return this == AWAITING_CONFIRMATION || this == COMPLETED || this == MANUAL_RECOVERY_REQUIRED; }
    public boolean terminal() { return this == COMPLETED || this == ROLLED_BACK || this == FAILED || this == CANCELLED; }
}
