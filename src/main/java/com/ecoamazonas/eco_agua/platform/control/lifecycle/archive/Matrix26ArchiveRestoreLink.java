package com.ecoamazonas.eco_agua.platform.control.lifecycle.archive;

import java.time.LocalDateTime;

public record Matrix26ArchiveRestoreLink(
        Long id,
        Long archiveRecordId,
        Long restoreJobId,
        String restorePublicId,
        String targetInstanceCode,
        String targetDatabaseName,
        String targetRuntimeProfile,
        Integer targetRuntimePort,
        String status,
        String requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime completedAt
) {}
