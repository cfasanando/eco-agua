package com.ecoamazonas.eco_agua.platform.control.backups;

import java.nio.file.Path;

public record Matrix26BackupExtraction(
        Matrix26BackupJob job,
        Matrix26BackupEncryption encryption,
        Path extractedDirectory,
        int entryCount,
        String verificationMessage
) {
}
