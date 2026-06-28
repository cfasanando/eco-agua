package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

public record Matrix26DecommissionSummary(
        long totalJobs,
        long readyJobs,
        long decommissionedInstances,
        long failedJobs
) {
}
