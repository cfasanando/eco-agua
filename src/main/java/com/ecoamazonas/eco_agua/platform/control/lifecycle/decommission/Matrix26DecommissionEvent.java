package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

import java.time.LocalDateTime;

public record Matrix26DecommissionEvent(
        Long id,
        Long decommissionJobId,
        String eventType,
        String status,
        String actor,
        String detail,
        LocalDateTime createdAt
) {
}
