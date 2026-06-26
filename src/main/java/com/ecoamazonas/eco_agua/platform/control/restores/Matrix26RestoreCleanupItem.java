package com.ecoamazonas.eco_agua.platform.control.restores;

public record Matrix26RestoreCleanupItem(
        String resourceType,
        String location,
        boolean exists,
        String ownership,
        String proposedAction,
        String detail
) {
}
