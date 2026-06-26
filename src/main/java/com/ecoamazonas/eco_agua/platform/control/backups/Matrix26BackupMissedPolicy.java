package com.ecoamazonas.eco_agua.platform.control.backups;

public enum Matrix26BackupMissedPolicy {
    RUN_ON_STARTUP("Run latest missed backup"),
    SKIP("Skip missed backup"),
    MARK_AS_MISSED("Record as missed");

    private final String label;

    Matrix26BackupMissedPolicy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Matrix26BackupMissedPolicy from(String value) {
        if (value == null || value.isBlank()) {
            return RUN_ON_STARTUP;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return RUN_ON_STARTUP;
        }
    }
}
