package com.ecoamazonas.eco_agua.platform.control.backups;

import java.time.LocalDateTime;

public record Matrix26BackupAlert(
        Long id,
        Long instanceId,
        String instanceCode,
        Long scheduleId,
        Long executionId,
        String alertCode,
        Matrix26BackupAlertSeverity severity,
        Matrix26BackupAlertStatus status,
        String title,
        String message,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String resolvedBy
) {
}
