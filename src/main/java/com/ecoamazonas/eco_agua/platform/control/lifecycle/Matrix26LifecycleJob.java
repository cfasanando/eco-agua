package com.ecoamazonas.eco_agua.platform.control.lifecycle;

import java.time.LocalDateTime;

public record Matrix26LifecycleJob(
        Long id,
        String publicId,
        Long instanceId,
        String instanceCode,
        String instanceName,
        Matrix26LifecycleAction action,
        Matrix26LifecycleStatus status,
        String reason,
        String requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String previousInstanceStatus,
        String resultingInstanceStatus,
        boolean runtimeWasRunning,
        Integer pausedScheduleCount,
        Long verifiedBackupJobId,
        String verifiedBackupPublicId,
        LocalDateTime verifiedBackupCompletedAt,
        Long relatedLifecycleJobId,
        String lastError
) {
}
