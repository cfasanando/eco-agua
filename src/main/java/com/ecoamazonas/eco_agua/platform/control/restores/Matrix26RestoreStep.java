package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26RestoreStep(
        Long id,
        Long restoreJobId,
        String stepCode,
        Integer sequenceNumber,
        String label,
        Matrix26RestoreStepStatus status,
        String detail,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
