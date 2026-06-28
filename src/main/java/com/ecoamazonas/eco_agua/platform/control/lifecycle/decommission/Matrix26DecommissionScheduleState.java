package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

import java.time.LocalDateTime;

public record Matrix26DecommissionScheduleState(
        Long id,
        Long decommissionJobId,
        Long scheduleId,
        String scheduleName,
        boolean wasEnabled,
        LocalDateTime previousNextRunAt,
        boolean disabled,
        LocalDateTime disabledAt
) {
}
