package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

import java.time.LocalDateTime;

public record Matrix26DecommissionJob(
        Long id,
        String publicId,
        Long instanceId,
        String instanceCode,
        String instanceName,
        Matrix26DecommissionStatus status,
        String reason,
        String administrativeNotes,
        String requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer retentionDays,
        LocalDateTime retentionUntil,
        String previousInstanceStatus,
        String resultingInstanceStatus,
        Long finalBackupJobId,
        String finalBackupPublicId,
        LocalDateTime finalBackupCompletedAt,
        LocalDateTime finalBackupVerifiedAt,
        String finalBackupKeyId,
        String finalBackupSha256,
        Integer disabledScheduleCount,
        String lastError
) {
    public boolean readyToExecute() {
        return status == Matrix26DecommissionStatus.READY_TO_DECOMMISSION;
    }
}
