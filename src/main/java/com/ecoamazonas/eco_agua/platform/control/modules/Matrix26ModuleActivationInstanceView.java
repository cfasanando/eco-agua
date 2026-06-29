package com.ecoamazonas.eco_agua.platform.control.modules;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;

import java.util.List;
import java.util.Set;

public record Matrix26ModuleActivationInstanceView(
        PlatformBusinessClient instance,
        List<Matrix26ModuleActivationModuleView> modules,
        Set<String> enabledKeys,
        String businessProfile,
        String safetyStatus,
        String safetyTone
) {
    public long enabledCount() {
        return modules.stream().filter(Matrix26ModuleActivationModuleView::enabled).count();
    }

    public long totalCount() {
        return modules.size();
    }

    public boolean hasNoModules() {
        return enabledKeys.isEmpty();
    }

    public boolean hasDependencyWarnings() {
        return modules.stream().anyMatch(Matrix26ModuleActivationModuleView::hasDependencyWarning);
    }
}
