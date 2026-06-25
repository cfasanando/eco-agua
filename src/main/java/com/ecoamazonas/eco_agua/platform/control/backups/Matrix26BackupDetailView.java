package com.ecoamazonas.eco_agua.platform.control.backups;

import java.util.List;

public record Matrix26BackupDetailView(
        Matrix26BackupJob job,
        List<Matrix26BackupArtifact> artifacts,
        List<Matrix26BackupVerification> verifications,
        String backupDirectoryDisplay,
        String compressedSizeLabel,
        String databaseSizeLabel,
        String dumpSizeLabel
) {
}
