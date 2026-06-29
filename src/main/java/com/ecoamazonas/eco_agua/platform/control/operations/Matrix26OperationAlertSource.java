package com.ecoamazonas.eco_agua.platform.control.operations;

public enum Matrix26OperationAlertSource {
    RUNTIME("Runtime", "bi-cpu"),
    BACKUP("Backup", "bi-database-exclamation"),
    RESTORE("Restore", "bi-arrow-counterclockwise"),
    LIFECYCLE("Lifecycle", "bi-pause-circle"),
    PURGE("Purge", "bi-search-heart"),
    ARCHIVE("Archive", "bi-safe2"),
    SYSTEM("System", "bi-command");

    private final String label;
    private final String iconClass;

    Matrix26OperationAlertSource(String label, String iconClass) {
        this.label = label;
        this.iconClass = iconClass;
    }

    public String getLabel() {
        return label;
    }

    public String getIconClass() {
        return iconClass;
    }
}
