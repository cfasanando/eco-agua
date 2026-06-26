package com.ecoamazonas.eco_agua.platform.control.lifecycle;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;

public record Matrix26LifecycleInstanceView(
        PlatformBusinessClient instance,
        boolean allowlisted,
        boolean runtimeOnline,
        boolean portListening,
        boolean expectedProcess,
        String runtimeState,
        int enabledSchedules,
        Matrix26LifecycleBackupView latestVerifiedBackup,
        boolean recentBackup,
        boolean operationBlocked,
        String blockingReason,
        boolean canSuspend,
        boolean canReactivate,
        String suspendConfirmation,
        String reactivateConfirmation
) {
}
