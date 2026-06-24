package com.ecoamazonas.eco_agua.platform.control;

public record Matrix26ProvisioningSummary(
        long total,
        long ready,
        long blocked,
        long running,
        long completed,
        long failed
) {
}
