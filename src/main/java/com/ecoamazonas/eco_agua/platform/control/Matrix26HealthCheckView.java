package com.ecoamazonas.eco_agua.platform.control;

import java.time.LocalDateTime;

public record Matrix26HealthCheckView(
        String instanceName,
        String instanceCode,
        boolean online,
        Integer httpStatus,
        Long responseTimeMs,
        String message,
        LocalDateTime checkedAt,
        String badgeClass,
        String statusLabel
) {
}
