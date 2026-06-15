package com.ecoamazonas.eco_agua.platform;

public record PlatformInstanceMonitorSummary(
        long totalClients,
        long visibleClients,
        long protectedClients,
        long demoClients,
        long hiddenClients,
        long readyClients,
        long runningInstances,
        long stoppedReadyInstances,
        long pendingProvisioning,
        long missingRuntimeFiles
) {
}
