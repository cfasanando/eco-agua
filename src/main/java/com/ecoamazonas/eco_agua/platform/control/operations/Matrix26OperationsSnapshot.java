package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.LocalDateTime;
import java.util.List;

public record Matrix26OperationsSnapshot(
        List<Matrix26RuntimeInventoryItem> runtimes,
        List<Matrix26PortBinding> ports,
        List<Matrix26LogInventoryItem> logs,
        Matrix26OperationsSummary summary,
        List<String> probeWarnings,
        LocalDateTime capturedAt
) {
}
