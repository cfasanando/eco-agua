package com.ecoamazonas.eco_agua.platform.control.purge;

public record Matrix26PurgeSummary(
        long totalPlans,
        long dryRunReady,
        long blocked,
        long totalWouldDelete,
        long totalProtected
) {}
