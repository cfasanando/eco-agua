package com.ecoamazonas.eco_agua.platform.control.backups;

import java.nio.file.Path;
import java.util.Map;

public record Matrix26FullBackupResult(
        Path filesArchive,
        Path sanitizedRuntimeConfig,
        Path instanceMetadata,
        Path modulesMetadata,
        Path appearanceMetadata,
        Path filesInventory,
        Path diagnosticLogTail,
        Map<Path, String> hashes,
        long archiveEntries,
        long sourceBytes,
        long archiveBytes,
        long storedBytes,
        boolean stableInventory,
        int skippedSymlinks
) {
}
