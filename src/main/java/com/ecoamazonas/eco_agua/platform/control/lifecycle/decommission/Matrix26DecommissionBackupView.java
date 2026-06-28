package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

import java.time.LocalDateTime;

public record Matrix26DecommissionBackupView(
        Long jobId,
        String publicId,
        LocalDateTime completedAt,
        LocalDateTime verifiedAt,
        String keyId,
        String packageSha256,
        String retentionClass,
        boolean protectedFlag,
        String verificationStatus
) {
    public boolean finalArchiveVerified() {
        return jobId != null
                && "FINAL".equalsIgnoreCase(retentionClass)
                && protectedFlag
                && "VERIFIED".equalsIgnoreCase(verificationStatus);
    }
}
