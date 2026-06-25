package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.LocalDateTime;
import java.util.List;

public record Matrix26RuntimeInventoryItem(
        Matrix26RuntimeTarget target,
        Matrix26RuntimeState state,
        String stateDetail,
        boolean runtimeDirectoryPresent,
        String runtimeDirectory,
        boolean configurationPresent,
        String configurationPath,
        boolean launcherPresent,
        String launcherPath,
        boolean configurationConsistent,
        Integer configuredPort,
        String configuredRuntimeProfile,
        String configuredDatabaseName,
        boolean portListening,
        Long portOwnerPid,
        String portOwnerProcess,
        boolean expectedProcess,
        Long processId,
        String processExecutable,
        String processCommandLine,
        LocalDateTime processStartedAt,
        String uptimeLabel,
        Matrix26HttpProbe http,
        Matrix26LogInventoryItem primaryLog,
        long runtimeStorageBytes,
        String runtimeStorageLabel,
        long assetStorageBytes,
        String assetStorageLabel,
        List<String> warnings,
        LocalDateTime inspectedAt
) {
    public boolean online() {
        return state == Matrix26RuntimeState.ONLINE;
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}
