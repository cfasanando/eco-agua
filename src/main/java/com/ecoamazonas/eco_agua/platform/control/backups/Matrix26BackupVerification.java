package com.ecoamazonas.eco_agua.platform.control.backups;

import java.time.LocalDateTime;

public record Matrix26BackupVerification(
        Long id,
        Long jobId,
        String checkCode,
        String label,
        Matrix26BackupVerificationStatus status,
        String detail,
        LocalDateTime checkedAt
) {
}
