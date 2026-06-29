package com.ecoamazonas.eco_agua.platform.control.purge;

public record Matrix26ArchiveDestructionSummary(
        long totalPlans,
        long readyForReview,
        long blocked,
        long protectedArchives,
        long wouldDelete
) {}
