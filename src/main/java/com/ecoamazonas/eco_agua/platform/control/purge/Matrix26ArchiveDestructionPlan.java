package com.ecoamazonas.eco_agua.platform.control.purge;

import java.time.LocalDateTime;

public record Matrix26ArchiveDestructionPlan(
        Long id,
        String publicId,
        Long archiveRecordId,
        String archivePublicId,
        Long instanceId,
        String instanceCode,
        String instanceName,
        Long finalBackupJobId,
        String finalBackupPublicId,
        String finalBackupSha256,
        String backupDirectory,
        String packagePath,
        LocalDateTime retentionUntil,
        String retentionStatus,
        Matrix26ArchiveDestructionStatus status,
        Integer runNumber,
        Integer blockersCount,
        Integer wouldDeleteCount,
        Integer wouldKeepCount,
        Integer protectedCount,
        Integer reviewCount,
        String reason,
        String requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime evaluatedAt,
        String lastError
) {
    public boolean readyForReview() {
        return status == Matrix26ArchiveDestructionStatus.READY_FOR_REVIEW;
    }

    public int blockers() {
        return blockersCount == null ? 0 : blockersCount;
    }

    public int wouldDelete() {
        return wouldDeleteCount == null ? 0 : wouldDeleteCount;
    }

    public int wouldKeep() {
        return wouldKeepCount == null ? 0 : wouldKeepCount;
    }

    public int protectedResources() {
        return protectedCount == null ? 0 : protectedCount;
    }

    public int reviewResources() {
        return reviewCount == null ? 0 : reviewCount;
    }
}
