package com.ecoamazonas.eco_agua.platform.control.backups;

import java.time.LocalDateTime;

public record Matrix26BackupCalendarEvent(
        LocalDateTime scheduledAt,
        String instanceCode,
        String instanceName,
        String scheduleName,
        String eventType,
        String status,
        String badgeClass,
        Long executionId,
        Long backupJobId
) {
}
