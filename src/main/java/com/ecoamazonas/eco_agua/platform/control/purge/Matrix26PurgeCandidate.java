package com.ecoamazonas.eco_agua.platform.control.purge;

import java.time.LocalDateTime;

public record Matrix26PurgeCandidate(
        Long archiveRecordId,
        String archivePublicId,
        String instanceCode,
        String instanceName,
        String archiveStatus,
        String instanceStatus,
        String finalBackupPublicId,
        String retentionStatus,
        LocalDateTime retentionUntil,
        boolean allowed
) {}
