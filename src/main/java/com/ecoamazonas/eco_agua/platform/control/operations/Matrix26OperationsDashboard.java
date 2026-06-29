package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.LocalDateTime;
import java.util.List;

public record Matrix26OperationsDashboard(
        LocalDateTime generatedAt,
        List<Matrix26OperationsDashboardMetric> metrics,
        List<Matrix26OperationsDashboardAlert> alerts,
        List<Matrix26OperationsDashboardActivity> activities,
        List<Matrix26OperationsDashboardInstance> instances,
        boolean archiveDestructionExecutionEnabled,
        boolean purgeExecutionEnabled,
        boolean incomplete
) {
    public boolean healthy() {
        return alerts.stream().noneMatch(alert -> "CRITICAL".equals(alert.severity()) || "WARNING".equals(alert.severity()));
    }
}
