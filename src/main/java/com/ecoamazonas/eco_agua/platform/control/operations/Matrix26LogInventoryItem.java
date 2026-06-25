package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.LocalDateTime;

public record Matrix26LogInventoryItem(
        String runtimeKey,
        String instanceName,
        String relativePath,
        long sizeBytes,
        String sizeLabel,
        LocalDateTime modifiedAt,
        boolean currentCandidate
) {
}
