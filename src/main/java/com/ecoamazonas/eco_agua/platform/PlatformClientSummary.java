package com.ecoamazonas.eco_agua.platform;

public record PlatformClientSummary(
        long totalClients,
        long draftClients,
        long configuredClients,
        long pendingDatabases,
        long activeTemplates,
        long activeModules
) {
}
