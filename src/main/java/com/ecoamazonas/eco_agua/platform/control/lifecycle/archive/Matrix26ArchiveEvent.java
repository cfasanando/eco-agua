package com.ecoamazonas.eco_agua.platform.control.lifecycle.archive;

import java.time.LocalDateTime;

public record Matrix26ArchiveEvent(
        Long id,
        Long archiveRecordId,
        String eventType,
        String status,
        String actor,
        String detail,
        LocalDateTime createdAt
) {}
