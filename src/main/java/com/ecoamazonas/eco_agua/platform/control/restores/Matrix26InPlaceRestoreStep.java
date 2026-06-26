package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26InPlaceRestoreStep(
        Long id, Long jobId, String stepCode, int sequenceNumber, String label,
        Matrix26InPlaceRestoreStepStatus status, String detail,
        LocalDateTime startedAt, LocalDateTime completedAt
) { }
