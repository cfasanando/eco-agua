package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26RestoreCleanupPlan(
        Long id,
        String publicId,
        Long restoreJobId,
        Matrix26RestoreCleanupStatus status,
        String snapshotFingerprint,
        String planSignature,
        String requestedBy,
        LocalDateTime requestedAt,
        String approvedBy,
        LocalDateTime approvedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String summary,
        String lastError
) {
    public boolean previewReady() { return status == Matrix26RestoreCleanupStatus.PREVIEW_READY; }
    public boolean blocked() { return status == Matrix26RestoreCleanupStatus.BLOCKED; }
    public boolean cleaned() { return status == Matrix26RestoreCleanupStatus.CLEANED; }
    public boolean executable() { return status.executable(); }
    public boolean firstExecution() { return status == Matrix26RestoreCleanupStatus.APPROVED; }

    public boolean approved() {
        return status == Matrix26RestoreCleanupStatus.APPROVED
                || status == Matrix26RestoreCleanupStatus.RUNNING
                || status == Matrix26RestoreCleanupStatus.PARTIALLY_CLEANED
                || status == Matrix26RestoreCleanupStatus.FAILED;
    }

    public boolean finished() {
        return status == Matrix26RestoreCleanupStatus.CLEANED
                || status == Matrix26RestoreCleanupStatus.CANCELLED;
    }
}
