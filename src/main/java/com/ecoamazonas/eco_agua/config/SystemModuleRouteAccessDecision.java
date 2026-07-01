package com.ecoamazonas.eco_agua.config;

/**
 * Result of checking a request path against runtime module visibility rules.
 */
public record SystemModuleRouteAccessDecision(
        boolean protectedRoute,
        boolean allowed,
        String requestPath,
        SystemModuleRouteRule rule,
        boolean moduleEnabled
) {
    public static SystemModuleRouteAccessDecision allowedUnprotected(String requestPath) {
        return new SystemModuleRouteAccessDecision(false, true, requestPath, null, true);
    }

    public static SystemModuleRouteAccessDecision protectedRoute(String requestPath,
                                                                SystemModuleRouteRule rule,
                                                                boolean moduleEnabled) {
        return new SystemModuleRouteAccessDecision(true, moduleEnabled, requestPath, rule, moduleEnabled);
    }

    public String moduleKey() {
        return rule == null ? "" : rule.moduleKey();
    }

    public String label() {
        return rule == null ? "Unprotected route" : rule.label();
    }

    public String area() {
        return rule == null ? "General" : rule.area();
    }

    public String reason() {
        return rule == null ? "No system module route rule matched this path." : rule.reason();
    }
}
