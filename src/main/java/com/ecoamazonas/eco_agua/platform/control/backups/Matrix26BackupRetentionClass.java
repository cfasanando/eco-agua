package com.ecoamazonas.eco_agua.platform.control.backups;

public enum Matrix26BackupRetentionClass {
    DAILY("Daily", "Daily recovery point"),
    WEEKLY("Weekly", "Weekly recovery point"),
    MONTHLY("Monthly", "Monthly recovery point"),
    FINAL("Final archive", "Protected final customer archive");

    private final String label;
    private final String description;

    Matrix26BackupRetentionClass(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static Matrix26BackupRetentionClass from(String value) {
        if (value == null || value.isBlank()) {
            return DAILY;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DAILY;
        }
    }
}
