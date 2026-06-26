package com.ecoamazonas.eco_agua.platform.control.lifecycle;

import java.time.LocalDateTime;

public record Matrix26LifecycleEvent(
        Long id,
        Long lifecycleJobId,
        String eventType,
        String status,
        String actor,
        String detail,
        LocalDateTime createdAt
) {
}
