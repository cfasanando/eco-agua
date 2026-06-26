package com.ecoamazonas.eco_agua.platform.control.backups;

import java.time.LocalDateTime;

public record Matrix26BackupScheduleExecution(
        Long id,
        Long scheduleId,
        Long instanceId,
        String instanceCode,
        String instanceName,
        String scheduleName,
        LocalDateTime plannedAt,
        Matrix26BackupScheduleExecutionStatus status,
        Integer attemptCount,
        Integer maxAttempts,
        LocalDateTime queuedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime nextRetryAt,
        Long backupJobId,
        String backupPublicId,
        String triggerType,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
