package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.LocalDateTime;

public record Matrix26OperationsDashboardActivity(
        LocalDateTime occurredAt,
        String area,
        String instanceCode,
        String title,
        String status,
        String badgeClass,
        String detail,
        String href
) {
}
