package com.ecoamazonas.eco_agua.platform.control.backups;

public record Matrix26BackupCandidate(
        Long instanceId,
        String instanceCode,
        String businessName,
        String databaseName,
        String runtimeProfile,
        Integer runtimePort,
        boolean allowed,
        String restrictionReason
) {
}
