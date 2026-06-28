package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;

public record Matrix26DecommissionCandidate(
        PlatformBusinessClient instance,
        boolean allowlisted,
        boolean eligible,
        String blocker,
        boolean runtimeStopped,
        int enabledSchedules,
        String prepareConfirmation
) {
}
