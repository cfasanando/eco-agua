package com.ecoamazonas.eco_agua.platform.control.backups;

public enum Matrix26BackupScheduleExecutionStatus {
    SCHEDULED("Scheduled", "text-bg-secondary"),
    QUEUED("Queued", "text-bg-info"),
    RUNNING("Running", "text-bg-primary"),
    COMPLETED("Completed", "text-bg-success"),
    FAILED("Failed", "text-bg-danger"),
    RETRY_WAITING("Retry waiting", "text-bg-warning"),
    MISSED("Missed", "text-bg-dark"),
    SKIPPED("Skipped", "text-bg-secondary"),
    CANCELLED("Cancelled", "text-bg-secondary");

    private final String label;
    private final String badgeClass;

    Matrix26BackupScheduleExecutionStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isFinalState() {
        return this == COMPLETED || this == FAILED || this == MISSED || this == SKIPPED || this == CANCELLED;
    }
}
