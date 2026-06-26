package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26RestoreValidationRun(
        Long id,
        String publicId,
        Long restoreJobId,
        Matrix26RestoreValidationStatus status,
        String requestedBy,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String summary
) {
    public boolean finished() { return status != Matrix26RestoreValidationStatus.RUNNING; }
}
