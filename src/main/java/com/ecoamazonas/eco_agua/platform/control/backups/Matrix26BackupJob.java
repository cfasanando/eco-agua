package com.ecoamazonas.eco_agua.platform.control.backups;

import java.time.LocalDateTime;

public record Matrix26BackupJob(
        Long id,
        String publicId,
        Long instanceId,
        String instanceCode,
        String instanceName,
        String databaseName,
        String backupType,
        Matrix26BackupStatus status,
        String requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String backupRoot,
        String backupDirectory,
        String toolPath,
        String toolVersion,
        String databaseHost,
        Integer databasePort,
        Long databaseSizeBytes,
        Long dumpSizeBytes,
        Long compressedSizeBytes,
        Integer tableCount,
        String sha256,
        String manifestPath,
        String reportPath,
        String verificationSummary,
        String lastError
) {
    public boolean isCompleted() {
        return status == Matrix26BackupStatus.COMPLETED;
    }
}
