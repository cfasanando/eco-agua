package com.ecoamazonas.eco_agua.platform.control.modules;

import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record Matrix26ModuleActivationOverview(
        LocalDateTime generatedAt,
        Matrix26ModuleActivationSummary summary,
        Map<String, List<PlatformModuleCatalog>> groupedModules,
        List<Matrix26ModuleActivationInstanceView> instances,
        List<Matrix26ModuleActivationEvent> recentEvents,
        List<String> notes
) {
}
