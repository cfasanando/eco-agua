package com.ecoamazonas.eco_agua.platform.control.demo;

import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlView;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeInventoryItem;

public record Matrix26DemoPortalView(
        Matrix26DemoPortalDefinition definition,
        Matrix26RuntimeInventoryItem runtime,
        Matrix26RuntimeControlView control,
        boolean online,
        String statusLabel,
        String statusDetail,
        String statusBadgeClass,
        String runtimeKey,
        String openUrl,
        String operationDetailUrl
) {
    public boolean hasRuntime() {
        return runtime != null;
    }

    public boolean canOpen() {
        return openUrl != null && !openUrl.isBlank();
    }

    public boolean canStart() {
        return control != null && control.canStart();
    }

    public boolean canStop() {
        return control != null && control.canStop();
    }

    public boolean canRestart() {
        return control != null && control.canRestart();
    }

    public String stopConfirmation() {
        return control == null ? "" : control.stopConfirmation();
    }

    public String restartConfirmation() {
        return control == null ? "" : control.restartConfirmation();
    }
}
