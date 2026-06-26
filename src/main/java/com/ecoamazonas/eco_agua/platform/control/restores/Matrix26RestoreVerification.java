package com.ecoamazonas.eco_agua.platform.control.restores;

import java.time.LocalDateTime;

public record Matrix26RestoreVerification(
        Long id,
        Long restoreJobId,
        String checkCode,
        String label,
        String status,
        String detail,
        LocalDateTime checkedAt
) {
}
