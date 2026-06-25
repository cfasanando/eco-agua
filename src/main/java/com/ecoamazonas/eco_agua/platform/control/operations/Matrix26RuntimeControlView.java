package com.ecoamazonas.eco_agua.platform.control.operations;

public record Matrix26RuntimeControlView(
        boolean controlEnabled,
        boolean manageable,
        boolean operationInProgress,
        boolean canStart,
        boolean canStop,
        boolean canRestart,
        String reason,
        String stopConfirmation,
        String restartConfirmation,
        Matrix26RuntimeManagedState managedState,
        Matrix26RuntimeOperation lastOperation
) {
}
