package com.ecoamazonas.eco_agua.platform.control.backups;

public enum Matrix26BackupScheduleFrequency {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String label;

    Matrix26BackupScheduleFrequency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Matrix26BackupScheduleFrequency from(String value) {
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
