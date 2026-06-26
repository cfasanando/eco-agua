package com.ecoamazonas.eco_agua.platform.control.backups;

import java.time.LocalDateTime;

public record Matrix26RetentionItem(
        Long jobId,
        String publicId,
        LocalDateTime requestedAt,
        Matrix26BackupRetentionClass retentionClass,
        Matrix26BackupVerificationState verificationStatus,
        boolean protectedFlag,
        long sizeBytes,
        String backupDirectory,
        boolean deletable,
        String reason
) {
    public String sizeLabel() {
        return Matrix26BackupService.formatBytes(sizeBytes);
    }
}
