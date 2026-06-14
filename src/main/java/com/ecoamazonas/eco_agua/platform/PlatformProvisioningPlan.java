package com.ecoamazonas.eco_agua.platform;

import java.util.List;

public record PlatformProvisioningPlan(
        PlatformBusinessClient client,
        List<PlatformProvisioningStep> steps,
        List<String> activeModuleKeys,
        String createDatabaseSql,
        String bootstrapSql,
        List<String> manualCommands,
        boolean canCreateDatabase,
        boolean canCopyStructureAutomatically,
        boolean canApplyBootstrapAutomatically,
        boolean canMarkStructureReady,
        boolean canMarkActive,
        boolean canGenerateRuntimeFiles,
        String warningMessage,
        boolean databaseCreated,
        boolean structureReady,
        boolean bootstrapApplied,
        boolean active,
        boolean ready,
        String statusTitle,
        String statusDescription,
        String statusBadgeClass,
        String statusAlertClass,
        String bootstrapFileName,
        String createDatabaseFileName,
        String manualCommandsText,
        String openBusinessUrl,
        String runtimeFolder,
        String runtimeApplicationPath,
        String runtimeRunScriptPath
) {
}
