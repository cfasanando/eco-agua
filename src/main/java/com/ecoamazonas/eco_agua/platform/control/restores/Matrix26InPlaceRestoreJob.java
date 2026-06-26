package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26InPlaceRestoreJob(
        Long id, String publicId, Long backupJobId, String backupPublicId,
        Long sourceInstanceId, String sourceInstanceCode, String sourceInstanceName,
        String sourceDatabaseName, String stageDatabaseName, String rollbackDatabaseName,
        String sourceRuntimeProfile, Integer sourceRuntimePort, String sourcePublicUrl,
        Long safetyBackupJobId, String safetyBackupPublicId,
        Matrix26InPlaceRestoreStatus status, String requestedBy, LocalDateTime requestedAt,
        LocalDateTime startedAt, LocalDateTime switchedAt, LocalDateTime confirmedAt,
        LocalDateTime rollbackExpiresAt, LocalDateTime completedAt,
        String workDirectory, String stageDataDirectory, String rollbackDataDirectory,
        String lastError
) {
    public boolean canSwitch() { return status.canSwitch(); }
    public boolean canConfirm() { return status.canConfirm(); }
    public boolean canRollback() { return status.canRollback() && (status != Matrix26InPlaceRestoreStatus.MANUAL_RECOVERY_REQUIRED || switchedAt != null); }
}
