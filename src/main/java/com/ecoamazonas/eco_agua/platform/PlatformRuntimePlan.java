package com.ecoamazonas.eco_agua.platform;

import java.util.List;

public record PlatformRuntimePlan(
        PlatformBusinessClient client,
        String runtimeProfile,
        int runtimePort,
        String localUrl,
        String publicUrl,
        String applicationFileName,
        String runScriptFileName,
        String applicationProperties,
        String runScript,
        List<String> runCommands,
        String runCommandsText,
        boolean databaseReady,
        boolean active,
        boolean runtimeConfigured,
        String statusTitle,
        String statusDescription,
        String statusAlertClass
) {
}
