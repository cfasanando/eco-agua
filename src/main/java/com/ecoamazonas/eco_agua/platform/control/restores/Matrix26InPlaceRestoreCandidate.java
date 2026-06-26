package com.ecoamazonas.eco_agua.platform.control.restores;

import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupEncryption;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupJob;

public record Matrix26InPlaceRestoreCandidate(
        Matrix26BackupJob backup,
        Matrix26BackupEncryption encryption,
        boolean eligible,
        String reason
) { }
