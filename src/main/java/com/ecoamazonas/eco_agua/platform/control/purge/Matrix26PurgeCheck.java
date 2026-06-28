package com.ecoamazonas.eco_agua.platform.control.purge;

import java.time.LocalDateTime;

public record Matrix26PurgeCheck(
        Long id,
        Long purgePlanId,
        Integer runNumber,
        String checkCode,
        String label,
        String status,
        String detail,
        LocalDateTime checkedAt
) {
    public boolean passed() {
        return "PASSED".equalsIgnoreCase(status) || "WARNING".equalsIgnoreCase(status);
    }
}
