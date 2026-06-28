package com.ecoamazonas.eco_agua.platform.control.purge;

import java.time.LocalDateTime;

public record Matrix26PurgeEvent(
        Long id,
        Long purgePlanId,
        String eventType,
        String status,
        String actor,
        String detail,
        LocalDateTime createdAt
) {}
