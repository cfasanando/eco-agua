package com.ecoamazonas.eco_agua.config;

import com.ecoamazonas.eco_agua.platform.control.Matrix26ControlCenterProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Blocks direct URL access when the owning runtime module is disabled.
 *
 * Sidebar visibility is useful, but direct URLs must also respect the active module map.
 */
@Component
public class SystemModuleAccessFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTRIBUTE_DENIED = "systemModuleAccessDenied";
    public static final String REQUEST_ATTRIBUTE_MODULE_KEY = "systemModuleAccessDeniedModuleKey";
    public static final String REQUEST_ATTRIBUTE_MODULE_LABEL = "systemModuleAccessDeniedModuleLabel";
    public static final String REQUEST_ATTRIBUTE_ROUTE_PREFIX = "systemModuleAccessDeniedRoutePrefix";
    public static final String REQUEST_ATTRIBUTE_REQUEST_PATH = "systemModuleAccessDeniedRequestPath";

    private final SystemModuleRouteAccessService routeAccessService;
    private final Matrix26ControlCenterProperties controlCenterProperties;

    public SystemModuleAccessFilter(SystemModuleRouteAccessService routeAccessService,
                                    Matrix26ControlCenterProperties controlCenterProperties) {
        this.routeAccessService = routeAccessService;
        this.controlCenterProperties = controlCenterProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (controlCenterProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        SystemModuleRouteAccessDecision decision = routeAccessService.decide(request);
        if (decision.protectedRoute() && !decision.allowed()) {
            markDeniedRequest(request, decision);
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Module is disabled: " + decision.moduleKey()
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void markDeniedRequest(HttpServletRequest request, SystemModuleRouteAccessDecision decision) {
        request.setAttribute(REQUEST_ATTRIBUTE_DENIED, true);
        request.setAttribute(REQUEST_ATTRIBUTE_MODULE_KEY, decision.moduleKey());
        request.setAttribute(REQUEST_ATTRIBUTE_MODULE_LABEL, decision.label());
        request.setAttribute(REQUEST_ATTRIBUTE_ROUTE_PREFIX, decision.rule().pathPrefix());
        request.setAttribute(REQUEST_ATTRIBUTE_REQUEST_PATH, decision.requestPath());
    }
}
