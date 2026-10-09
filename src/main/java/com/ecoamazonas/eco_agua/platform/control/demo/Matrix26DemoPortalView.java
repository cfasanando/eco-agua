package com.ecoamazonas.eco_agua.platform.control.demo;

import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlView;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeInventoryItem;

public record Matrix26DemoPortalView(
        Matrix26DemoPortalDefinition definition,
        Matrix26RuntimeInventoryItem runtime,
        Matrix26RuntimeControlView control,
        Matrix26DemoRuntimeStatus localRuntime,
        boolean online,
        String statusLabel,
        String statusDetail,
        String statusBadgeClass,
        String runtimeKey,
        String openUrl,
        String operationDetailUrl
) {
    public boolean hasRuntime() {
        return runtime != null || (localRuntime != null && localRuntime.configured());
    }

    public boolean canOpen() {
        return openUrl != null && !openUrl.isBlank();
    }

    public boolean canStart() {
        return (localRuntime != null && localRuntime.canStart()) || (control != null && control.canStart());
    }

    public boolean canStop() {
        return (localRuntime != null && localRuntime.canStop()) || (control != null && control.canStop());
    }

    public boolean canRestart() {
        return (localRuntime != null && localRuntime.canRestart()) || (control != null && control.canRestart());
    }

    public boolean canViewLog() {
        return localRuntime != null && localRuntime.applicable() && localRuntime.configured();
    }

    public String stopConfirmation() {
        if (control != null && control.stopConfirmation() != null && !control.stopConfirmation().isBlank()) {
            return control.stopConfirmation();
        }
        return "STOP " + definition.code();
    }

    public String restartConfirmation() {
        if (control != null && control.restartConfirmation() != null && !control.restartConfirmation().isBlank()) {
            return control.restartConfirmation();
        }
        return "RESTART " + definition.code();
    }
}
