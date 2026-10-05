package com.ecoamazonas.eco_agua.platform.control.demo;

import java.util.List;

public record Matrix26DemoPortalDefinition(
        String code,
        String name,
        String category,
        Matrix26DemoPortalMode mode,
        Matrix26DemoReadiness readiness,
        String localUrl,
        String mainPath,
        Integer port,
        String runtimeProfile,
        String instanceCode,
        String databaseName,
        String demoUsername,
        String demoPassword,
        String iconClass,
        String accentColor,
        String salesPitch,
        String demoScenario,
        String internalNote,
        List<String> modules,
        int priority
) {
    public boolean runtimeManaged() {
        return mode == Matrix26DemoPortalMode.MANAGED_RUNTIME || mode == Matrix26DemoPortalMode.CONTROL_CENTER;
    }

    public String displayUrl() {
        if (localUrl == null || localUrl.isBlank()) {
            return mainPath == null || mainPath.isBlank() ? "No configurado" : mainPath;
        }
        return localUrl;
    }
}
