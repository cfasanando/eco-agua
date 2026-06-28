package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

public enum Matrix26DecommissionStatus {
    REQUESTED("Requested", "text-bg-secondary"),
    PRECHECKING("Prechecking", "text-bg-info"),
    FINAL_BACKUP_RUNNING("Final backup running", "text-bg-warning"),
    FINAL_BACKUP_VERIFYING("Final backup verifying", "text-bg-warning"),
    READY_TO_DECOMMISSION("Ready to decommission", "text-bg-primary"),
    DECOMMISSIONING("Decommissioning", "text-bg-warning"),
    DECOMMISSIONED("Decommissioned", "text-bg-dark"),
    FAILED("Failed", "text-bg-danger"),
    MANUAL_REVIEW_REQUIRED("Manual review required", "text-bg-danger"),
    CANCELLED("Cancelled", "text-bg-secondary");

    private final String label;
    private final String badgeClass;

    Matrix26DecommissionStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isActive() {
        return this == REQUESTED || this == PRECHECKING || this == FINAL_BACKUP_RUNNING
                || this == FINAL_BACKUP_VERIFYING || this == READY_TO_DECOMMISSION
                || this == DECOMMISSIONING;
    }
}
