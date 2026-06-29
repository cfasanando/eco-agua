package com.ecoamazonas.eco_agua.platform.control.modules;

public record Matrix26ModuleActivationSummary(
        long totalInstances,
        long protectedInstances,
        long totalModules,
        long activeModules,
        long enabledAssignments,
        long instancesWithoutModules,
        long dependencyWarnings
) {
}
