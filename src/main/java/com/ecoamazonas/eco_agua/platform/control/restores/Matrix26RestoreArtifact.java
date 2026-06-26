package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26RestoreArtifact(
        Long id,
        Long restoreJobId,
        String artifactType,
        String path,
        Long sizeBytes,
        String sha256,
        String status,
        LocalDateTime createdAt
) {
}
