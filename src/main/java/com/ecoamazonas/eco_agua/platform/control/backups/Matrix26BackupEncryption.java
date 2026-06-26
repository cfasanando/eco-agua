package com.ecoamazonas.eco_agua.platform.control.backups;

import java.time.LocalDateTime;

public record Matrix26BackupEncryption(
        Long id,
        Long jobId,
        boolean encrypted,
        String algorithm,
        Integer formatVersion,
        String keyId,
        String packagePath,
        Long packageSizeBytes,
        String packageSha256,
        Matrix26BackupVerificationState verificationStatus,
        LocalDateTime verifiedAt,
        Matrix26BackupRetentionClass retentionClass,
        LocalDateTime expiresAt,
        boolean protectedFlag,
        String protectionReason,
        LocalDateTime encryptedAt,
        LocalDateTime updatedAt
) {
    public String packageSizeLabel() {
        return Matrix26BackupService.formatBytes(packageSizeBytes);
    }
}
