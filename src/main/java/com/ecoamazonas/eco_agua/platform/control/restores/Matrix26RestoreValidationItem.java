package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26RestoreValidationItem(
        Long id,
        Long validationRunId,
        String checkCode,
        String category,
        String label,
        Matrix26RestoreCheckStatus status,
        String sourceValue,
        String targetValue,
        String detail,
        LocalDateTime checkedAt
) {
}
