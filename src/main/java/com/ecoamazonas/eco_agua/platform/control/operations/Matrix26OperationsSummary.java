package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.LocalDateTime;

public record Matrix26OperationsSummary(
        long totalRuntimes,
        long onlineRuntimes,
        long offlineRuntimes,
        long degradedRuntimes,
        long protectedRuntimes,
        long listeningPorts,
        long detectedProcesses,
        long totalStorageBytes,
        String totalStorageLabel,
        LocalDateTime inspectedAt
) {
}
