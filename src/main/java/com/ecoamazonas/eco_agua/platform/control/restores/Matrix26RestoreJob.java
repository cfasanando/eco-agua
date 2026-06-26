package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26RestoreJob(
        Long id,
        String publicId,
        Long backupJobId,
        String backupPublicId,
        Long sourceInstanceId,
        String sourceInstanceCode,
        String sourceInstanceName,
        String sourceDatabaseName,
        Long targetInstanceId,
        String targetInstanceCode,
        String targetInstanceName,
        String targetDatabaseName,
        String targetRuntimeProfile,
        Integer targetRuntimePort,
        String targetPublicUrl,
        Matrix26RestoreStatus status,
        boolean startAfterRestore,
        String requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String temporaryDirectory,
        String lastError
) {
    public boolean completed() { return status == Matrix26RestoreStatus.COMPLETED; }
    public boolean failed() { return status == Matrix26RestoreStatus.FAILED || status == Matrix26RestoreStatus.CLEANUP_REQUIRED; }
}
