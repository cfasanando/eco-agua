package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.LocalDateTime;

public record Matrix26OperationAlertEvent(
        Long id,
        Long alertId,
        String eventType,
        Matrix26OperationAlertStatus status,
        String actor,
        String note,
        LocalDateTime createdAt
) {
}
