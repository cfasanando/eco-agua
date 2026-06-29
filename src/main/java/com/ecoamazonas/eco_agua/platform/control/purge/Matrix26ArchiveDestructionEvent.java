package com.ecoamazonas.eco_agua.platform.control.purge;

import java.time.LocalDateTime;

public record Matrix26ArchiveDestructionEvent(
        Long id,
        Long destructionPlanId,
        String eventType,
        String status,
        String actor,
        String detail,
        LocalDateTime createdAt
) {}
