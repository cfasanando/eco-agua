package com.ecoamazonas.eco_agua.platform.control.modules;

import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalog;

public record Matrix26ModuleActivationModuleView(
        PlatformModuleCatalog module,
        boolean enabled,
        String dependencyLabel,
        boolean dependenciesSatisfied,
        String runtimeProperty,
        String suggestedFor
) {
    public String statusLabel() {
        return enabled ? "Enabled" : "Disabled";
    }

    public String statusBadgeClass() {
        return enabled ? "text-bg-success" : "text-bg-secondary";
    }

    public boolean hasDependencyWarning() {
        return enabled && dependencyLabel != null && !dependencyLabel.isBlank() && !dependenciesSatisfied;
    }
}
