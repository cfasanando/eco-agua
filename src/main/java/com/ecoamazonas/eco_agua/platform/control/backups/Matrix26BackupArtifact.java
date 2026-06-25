package com.ecoamazonas.eco_agua.platform.control.backups;

import java.time.LocalDateTime;

public record Matrix26BackupArtifact(
        Long id,
        Long jobId,
        String artifactType,
        String fileName,
        String relativePath,
        Long sizeBytes,
        String sha256,
        String status,
        LocalDateTime createdAt
) {
}
