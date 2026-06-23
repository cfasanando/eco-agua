package com.ecoamazonas.eco_agua.platform.control;

import java.time.LocalDateTime;

public record Matrix26ControlSummary(
        long totalInstances,
        long onlineInstances,
        long offlineInstances,
        long protectedInstances,
        long totalModules,
        LocalDateTime lastCheckedAt
) {
}
