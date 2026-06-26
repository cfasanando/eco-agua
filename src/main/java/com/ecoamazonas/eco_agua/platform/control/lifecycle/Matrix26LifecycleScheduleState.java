package com.ecoamazonas.eco_agua.platform.control.lifecycle;

import java.time.LocalDateTime;

public record Matrix26LifecycleScheduleState(
        Long id,
        Long lifecycleJobId,
        Long scheduleId,
        String scheduleName,
        boolean wasEnabled,
        LocalDateTime previousNextRunAt,
        boolean restored,
        LocalDateTime restoredAt
) {
}
