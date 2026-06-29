package com.ecoamazonas.eco_agua.platform.control.purge;

import java.time.LocalDateTime;

public record Matrix26ArchiveDestructionCandidate(
        Long archiveRecordId,
        String archivePublicId,
        String instanceCode,
        String instanceName,
        String archiveStatus,
        String instanceStatus,
        String finalBackupPublicId,
        LocalDateTime retentionUntil,
        String retentionStatus,
        int cloneCount,
        boolean allowed
) {
    public boolean hasCloneLinks() {
        return cloneCount > 0;
    }
}
