package com.ecoamazonas.eco_agua.platform.control.lifecycle.archive;

public record Matrix26ArchiveSummary(
        long totalArchives,
        long readyArchives,
        long verifiedArchives,
        long cloneRestores
) {}
