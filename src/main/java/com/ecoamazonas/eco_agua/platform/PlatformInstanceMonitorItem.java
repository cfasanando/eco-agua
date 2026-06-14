package com.ecoamazonas.eco_agua.platform;

import java.time.LocalDateTime;

public record PlatformInstanceMonitorItem(
        PlatformBusinessClient client,
        String runtimeProfile,
        int runtimePort,
        String localUrl,
        String publicUrl,
        String databaseStatus,
        String businessStatus,
        String runtimeStatus,
        boolean databaseReady,
        boolean businessActive,
        boolean runtimeConfigured,
        boolean runtimeFilesGenerated,
        boolean configFileExists,
        boolean runScriptExists,
        boolean running,
        String healthStatus,
        String healthBadgeClass,
        String healthAlertClass,
        String healthDescription,
        String runCommand,
        String runtimeFolder,
        LocalDateTime lastRuntimeGeneratedAt,
        LocalDateTime checkedAt
) {
}
