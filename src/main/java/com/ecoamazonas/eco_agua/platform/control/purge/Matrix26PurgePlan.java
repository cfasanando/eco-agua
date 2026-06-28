package com.ecoamazonas.eco_agua.platform.control.purge;

import java.time.LocalDateTime;

public record Matrix26PurgePlan(
        Long id,
        String publicId,
        Long archiveRecordId,
        String archivePublicId,
        Long decommissionJobId,
        Long instanceId,
        String instanceCode,
        String instanceName,
        String databaseName,
        String runtimeProfile,
        Integer runtimePort,
        Long finalBackupJobId,
        String finalBackupPublicId,
        String finalBackupSha256,
        LocalDateTime retentionUntil,
        String retentionStatus,
        Matrix26PurgeStatus status,
        Integer runNumber,
        boolean eligibleForFuturePurge,
        Integer blockersCount,
        Integer wouldDeleteCount,
        Integer wouldKeepCount,
        Integer protectedCount,
        Integer reviewCount,
        Integer notFoundCount,
        String reason,
        String requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime evaluatedAt,
        String lastError
) {
    public boolean ready() {
        return status == Matrix26PurgeStatus.DRY_RUN_READY
                || status == Matrix26PurgeStatus.READY_TO_PURGE
                || status == Matrix26PurgeStatus.PURGING
                || status == Matrix26PurgeStatus.PARTIALLY_PURGED
                || status == Matrix26PurgeStatus.PURGED
                || status == Matrix26PurgeStatus.BLOCKED;
    }

    public boolean dryRunReady() {
        return status == Matrix26PurgeStatus.DRY_RUN_READY;
    }

    public boolean readyToPurge() {
        return status == Matrix26PurgeStatus.READY_TO_PURGE;
    }

    public boolean purged() {
        return status == Matrix26PurgeStatus.PURGED;
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

    public int missingResources() {
        return notFoundCount == null ? 0 : notFoundCount;
    }
}
