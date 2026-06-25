package com.ecoamazonas.eco_agua.platform.control.operations;

public record Matrix26RuntimeStabilityView(
        Matrix26RuntimePidFileInfo pidFile,
        boolean stalePid,
        boolean orphanProcess,
        boolean portConflict,
        boolean interruptedOperation,
        boolean canCleanStalePid,
        boolean canAdoptProcess,
        boolean canForceStop,
        boolean canRotateLogs,
        String cleanPidConfirmation,
        String adoptConfirmation,
        String forceStopConfirmation,
        String rotateLogsConfirmation,
        long currentLogBytes,
        String currentLogSizeLabel,
        int rotatedLogCount,
        String diagnosticMessage
) {
}
