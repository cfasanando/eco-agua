package com.ecoamazonas.eco_agua.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SystemModuleAccessFilter extends OncePerRequestFilter {

    private static final List<ModuleRoute> MODULE_ROUTES = List.of(
            ModuleRoute.prefix("/portal", "public_site"),
            ModuleRoute.prefix("/catalogo", "public_catalog"),
            ModuleRoute.prefix("/order/whatsapp", "public_catalog"),
            ModuleRoute.prefix("/blog", "blog"),
            ModuleRoute.prefix("/academy", "academy"),
            ModuleRoute.prefix("/admin/academy", "academy"),
            ModuleRoute.prefix("/admin/blog", "blog"),
            ModuleRoute.prefix("/marketing/admin/promotions", "promotions"),
            ModuleRoute.prefix("/marketing/admin/campaigns", "marketing"),
            ModuleRoute.prefix("/marketing/admin/strategy", "marketing"),
            ModuleRoute.prefix("/marketing/admin/ideas", "marketing"),
            ModuleRoute.prefix("/marketing/admin/publication-plan", "marketing"),
            ModuleRoute.prefix("/marketing/admin/actions-report", "marketing"),
            ModuleRoute.prefix("/marketing/admin/image-library", "marketing"),
            ModuleRoute.prefix("/marketing/admin/testimonials", "testimonials"),
            ModuleRoute.prefix("/admin/promotions", "promotions"),
            ModuleRoute.prefix("/admin/clients", "clients"),
            ModuleRoute.prefix("/admin/client-profiles", "clients"),
            ModuleRoute.prefix("/delivery", "delivery"),
            ModuleRoute.prefix("/admin/delivery-zones", "delivery"),
            ModuleRoute.prefix("/reorder-agenda", "reorder"),
            ModuleRoute.prefix("/orders", "income"),
            ModuleRoute.prefix("/income", "income"),
            ModuleRoute.prefix("/expenses/fixed-costs", "fixed_costs"),
            ModuleRoute.prefix("/expenses", "expenses"),
            ModuleRoute.prefix("/admin/suppliers", "suppliers"),
            ModuleRoute.prefix("/admin/products", "products"),
            ModuleRoute.prefix("/admin/categories", "categories"),
            ModuleRoute.prefix("/warehouse/purchase-history", "warehouse"),
            ModuleRoute.prefix("/warehouse/reorder-suggestions", "warehouse"),
            ModuleRoute.prefix("/warehouse/products-stock", "warehouse"),
            ModuleRoute.prefix("/warehouse/supplies-stock", "supplies"),
            ModuleRoute.prefix("/admin/supplies", "supplies"),
            ModuleRoute.prefix("/containers", "containers"),
            ModuleRoute.prefix("/production", "production"),
            ModuleRoute.prefix("/accounting", "accounting"),
            ModuleRoute.prefix("/cashflow/break-even", "break_even"),
            ModuleRoute.prefix("/cashflow", "cashflow"),
            ModuleRoute.prefix("/admin/price-simulator", "price_simulator"),
            ModuleRoute.prefix("/dashboard/business", "dashboard"),
            ModuleRoute.prefix("/dashboard/areas", "dashboard"),
            ModuleRoute.prefix("/dashboard/business-overview", "business_overview"),
            ModuleRoute.prefix("/dashboard/monthly-followup", "monthly_followup"),
            ModuleRoute.prefix("/dashboard/commercial-daily", "commercial_daily"),
            ModuleRoute.prefix("/admin/system-modules", "platform_settings")
    );

    private final SystemModuleService systemModuleService;

    public SystemModuleAccessFilter(SystemModuleService systemModuleService) {
        this.systemModuleService = systemModuleService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = normalizedPath(request);
        ModuleRoute matchedRoute = findRoute(path);

        if (matchedRoute != null && !systemModuleService.isEnabled(matchedRoute.moduleKey())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Module is disabled.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private ModuleRoute findRoute(String path) {
        for (ModuleRoute route : MODULE_ROUTES) {
            if (route.matches(path)) {
                return route;
            }
        }
        return null;
    }

    private String normalizedPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();

        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        if (uri == null || uri.isBlank()) {
            return "/";
        }

        return uri.startsWith("/") ? uri : "/" + uri;
    }

    private record ModuleRoute(String pathPrefix, String moduleKey) {
        static ModuleRoute prefix(String pathPrefix, String moduleKey) {
            return new ModuleRoute(pathPrefix, moduleKey);
        }

        boolean matches(String requestPath) {
            return requestPath.equals(pathPrefix) || requestPath.startsWith(pathPrefix + "/");
        }
    }
}
