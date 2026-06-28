package com.ecoamazonas.eco_agua.platform.control.lifecycle.archive;

import java.time.LocalDateTime;

public record Matrix26ArchiveRecord(
        Long id,
        String publicId,
        Long decommissionJobId,
        String decommissionPublicId,
        Long instanceId,
        String instanceCode,
        String instanceName,
        String instanceStatus,
        Long finalBackupJobId,
        String finalBackupPublicId,
        String finalBackupSha256,
        String finalBackupKeyId,
        LocalDateTime finalBackupVerifiedAt,
        LocalDateTime retentionUntil,
        String archiveStatus,
        String retentionStatus,
        Integer runtimeFileCount,
        Integer dataFileCount,
        String inventorySummary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastVerifiedAt,
        String lastError
) {
    public boolean restorableAsClone() {
        return "READY".equalsIgnoreCase(archiveStatus)
                && "DECOMMISSIONED".equalsIgnoreCase(instanceStatus)
                && finalBackupJobId != null;
    }
}
