package com.ecoamazonas.eco_agua.platform.control.restores;

import java.util.List;

public record Matrix26RestoreCleanupSnapshot(
        String fingerprint,
        List<Matrix26RestoreCleanupPlanItem> items,
        List<String> blockers,
        String summary
) {
    public boolean blocked() { return blockers != null && !blockers.isEmpty(); }
}
