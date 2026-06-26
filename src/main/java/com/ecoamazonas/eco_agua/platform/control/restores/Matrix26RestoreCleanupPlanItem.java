package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26RestoreCleanupPlanItem(
        Long id,
        Long cleanupPlanId,
        int sequenceNumber,
        String resourceType,
        String location,
        boolean existedAtPreview,
        String ownership,
        String plannedAction,
        String confirmationGroup,
        Matrix26RestoreCleanupItemStatus status,
        String detail,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String lastError
) {
    public boolean actionable() {
        return existedAtPreview && !"KEEP".equals(plannedAction) && !"BLOCKED".equals(plannedAction);
    }
}
