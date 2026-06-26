package com.ecoamazonas.eco_agua.platform.control.restores;

import java.util.List;

public record Matrix26RestoreCleanupPreview(
        Long restoreJobId,
        boolean cleanupCandidate,
        long existingResources,
        long blockedResources,
        List<Matrix26RestoreCleanupItem> items,
        String summary
) {
}
