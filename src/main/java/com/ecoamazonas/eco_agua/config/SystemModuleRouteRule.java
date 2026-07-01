package com.ecoamazonas.eco_agua.config;

/**
 * Describes a runtime route protected by a system module flag.
 */
public record SystemModuleRouteRule(
        String pathPrefix,
        String moduleKey,
        String label,
        String area,
        String reason
) {
    public boolean matches(String requestPath) {
        return requestPath.equals(pathPrefix) || requestPath.startsWith(pathPrefix + "/");
    }
}
