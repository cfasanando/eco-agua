package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;

import java.time.LocalDateTime;

public record Matrix26InstanceStatus(
        PlatformBusinessClient instance,
        boolean online,
        Integer httpStatus,
        Long responseTimeMs,
        String message,
        LocalDateTime checkedAt,
        String statusLabel,
        String badgeClass
) {
}
