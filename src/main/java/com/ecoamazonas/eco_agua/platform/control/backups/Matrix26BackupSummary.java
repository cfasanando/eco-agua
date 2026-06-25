package com.ecoamazonas.eco_agua.platform.control.backups;

public record Matrix26BackupSummary(
        long total,
        long completed,
        long failed,
        long running,
        long totalBytes
) {
}
