package com.ecoamazonas.eco_agua.platform.control.backups;

import java.time.LocalDateTime;

public record Matrix26BackupPolicy(
        Long id,
        Long instanceId,
        String instanceCode,
        int dailyKeep,
        int weeklyKeep,
        int monthlyKeep,
        boolean finalKeepIndefinitely,
        boolean enabled,
        String updatedBy,
        LocalDateTime updatedAt
) {
    public static Matrix26BackupPolicy defaults(Long instanceId, String instanceCode) {
        return new Matrix26BackupPolicy(null, instanceId, instanceCode, 7, 4, 6, true, true, "matrix26-system", null);
    }
}
