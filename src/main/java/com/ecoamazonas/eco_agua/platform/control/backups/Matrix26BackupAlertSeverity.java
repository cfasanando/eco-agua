package com.ecoamazonas.eco_agua.platform.control.backups;

public enum Matrix26BackupAlertSeverity {
    INFO("Info", "text-bg-info"),
    WARNING("Warning", "text-bg-warning"),
    CRITICAL("Critical", "text-bg-danger");

    private final String label;
    private final String badgeClass;

    Matrix26BackupAlertSeverity(String label, String badgeClass) {
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
