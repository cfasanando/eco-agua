package com.ecoamazonas.eco_agua.platform.control.backups;

public record Matrix26BackupScheduleSummary(
        long activeSchedules,
        long openAlerts,
        long completedExecutions,
        long failedExecutions,
        long missedExecutions
) {
}
