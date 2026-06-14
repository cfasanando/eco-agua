package com.ecoamazonas.eco_agua.platform;

public record PlatformTemplateSummary(
        PlatformBusinessTemplate template,
        long recommendedModules,
        long requiredModules
) {
}
