package com.ecoamazonas.eco_agua.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Applies runtime module flags to client routes.
 *
 * Matrix26 Control Center is intentionally excluded by the filter, so these rules protect only client instances.
 */
@Service
public class SystemModuleRouteAccessService {

    private static final List<SystemModuleRouteRule> ROUTE_RULES = List.of(
            // Public routes.
            rule("/portal", "public_site", "Portal público", "Portal público", "Public site is disabled for this instance."),
            rule("/catalogo", "public_catalog", "Catálogo público", "Portal público", "Public catalog is disabled for this instance."),
            rule("/order/whatsapp", "public_catalog", "Pedido por WhatsApp", "Portal público", "WhatsApp catalog order flow requires the public catalog module."),
            rule("/blog", "blog", "Blog público", "Portal público", "Blog module is disabled for this instance."),
            rule("/academy", "academy", "Academia pública", "Portal público", "Academy module is disabled for this instance."),
            rule("/restaurant", "restaurant_qr", "Carta pública restaurante", "Restaurante", "Public restaurant menu requires the restaurant QR module."),

            // Personal tools.
            rule("/gasto-claro", "personal_finance", "GastoClaro", "Personal", "GastoClaro personal finance module is disabled for this instance."),

            // Dashboards.
            rule("/dashboard/business-overview", "business_overview", "Estado del negocio", "Dashboard", "Business overview dashboard is disabled for this instance."),
            rule("/dashboard/monthly-followup", "monthly_followup", "Seguimiento mensual", "Dashboard", "Monthly follow-up dashboard is disabled for this instance."),
            rule("/dashboard/commercial-daily", "commercial_daily", "Panel comercial diario", "Dashboard", "Daily commercial dashboard is disabled for this instance."),
            rule("/dashboard/business", "dashboard", "Dashboard general", "Dashboard", "Dashboard module is disabled for this instance."),
            rule("/dashboard/areas", "dashboard", "Dashboards por área", "Dashboard", "Dashboard module is disabled for this instance."),

            // Sales and clients.
            rule("/orders", "sales", "Pedidos y cotizaciones", "Comercial", "Sales module is disabled for this instance."),
            rule("/income/sales", "sales", "Ventas por fecha", "Comercial", "Sales module is disabled for this instance."),
            rule("/income/credit", "income", "Cuentas por cobrar", "Finanzas", "Income module is disabled for this instance."),
            rule("/income/others", "income", "Otros ingresos", "Finanzas", "Income module is disabled for this instance."),
            rule("/income", "income", "Ingresos", "Finanzas", "Income module is disabled for this instance."),
            rule("/admin/clients", "clients", "Clientes", "Comercial", "Client management module is disabled for this instance."),
            rule("/admin/client-profiles", "clients", "Perfiles de cliente", "Comercial", "Client management module is disabled for this instance."),
            rule("/admin/promotions", "promotions", "Promociones", "Comercial", "Promotions module is disabled for this instance."),
            rule("/delivery", "delivery", "Delivery", "Comercial", "Delivery module is disabled for this instance."),
            rule("/admin/delivery-zones", "delivery", "Zonas de delivery", "Comercial", "Delivery module is disabled for this instance."),
            rule("/reorder-agenda", "reorder", "Agenda de reposición", "Comercial", "Reorder module is disabled for this instance."),

            // Finance.
            rule("/expenses/fixed-costs", "fixed_costs", "Costos fijos", "Finanzas", "Fixed costs module is disabled for this instance."),
            rule("/expenses", "expenses", "Egresos", "Finanzas", "Expenses module is disabled for this instance."),
            rule("/accounting", "accounting", "Contabilidad", "Finanzas", "Accounting module is disabled for this instance."),
            rule("/cashflow/break-even", "break_even", "Punto de equilibrio", "Finanzas", "Break-even module is disabled for this instance."),
            rule("/cashflow", "cashflow", "Flujo de caja", "Finanzas", "Cashflow module is disabled for this instance."),
            rule("/admin/price-simulator", "price_simulator", "Simulador de precios", "Finanzas", "Price simulator module is disabled for this instance."),
            rule("/admin/suppliers", "suppliers", "Proveedores", "Finanzas", "Supplier module is disabled for this instance."),

            // Inventory and operations.
            rule("/admin/products", "products", "Productos", "Inventario", "Product module is disabled for this instance."),
            rule("/admin/categories", "categories", "Categorías", "Inventario", "Category module is disabled for this instance."),
            rule("/warehouse/supplies-stock", "supplies", "Stock de insumos", "Inventario", "Supplies module is disabled for this instance."),
            rule("/warehouse/purchase-history", "warehouse", "Historial de compras", "Inventario", "Warehouse module is disabled for this instance."),
            rule("/warehouse/reorder-suggestions", "warehouse", "Sugerencias de reposición", "Inventario", "Warehouse module is disabled for this instance."),
            rule("/warehouse/products-stock", "warehouse", "Stock de productos", "Inventario", "Warehouse module is disabled for this instance."),
            rule("/warehouse", "warehouse", "Almacén", "Inventario", "Warehouse module is disabled for this instance."),
            rule("/admin/supplies", "supplies", "Insumos", "Inventario", "Supplies module is disabled for this instance."),
            rule("/containers", "containers", "Envases retornables", "Inventario", "Container module is disabled for this instance."),
            rule("/production", "production", "Producción", "Producción", "Production module is disabled for this instance."),

            // Marketing.
            rule("/admin/blog", "blog", "Administración de blog", "Marketing", "Blog module is disabled for this instance."),
            rule("/marketing/admin/testimonials", "testimonials", "Testimonios", "Marketing", "Testimonials module is disabled for this instance."),
            rule("/marketing/admin/promotions", "promotions", "Promociones marketing", "Marketing", "Promotions module is disabled for this instance."),
            rule("/marketing/admin", "marketing", "Marketing", "Marketing", "Marketing module is disabled for this instance."),
            rule("/admin/academy", "academy", "Administración academia", "Marketing", "Academy module is disabled for this instance."),
            rule("/my-courses", "academy", "Mis cursos", "Marketing", "Academy module is disabled for this instance."),

            // Restaurant: specific submodules must be evaluated before the general restaurant route.
            rule("/admin/restaurant/cash-sessions", "restaurant_cash", "Cierres de caja", "Restaurante", "Restaurant cash module is disabled for this instance."),
            rule("/admin/restaurant/cash", "restaurant_cash", "Caja restaurante", "Restaurante", "Restaurant cash module is disabled for this instance."),
            rule("/admin/restaurant/reports", "restaurant_cash", "Reportes restaurante", "Restaurante", "Restaurant cash module is disabled for this instance."),
            rule("/admin/restaurant/receipt", "restaurant_cash", "Recibos restaurante", "Restaurante", "Restaurant cash module is disabled for this instance."),
            rule("/admin/restaurant/bill", "restaurant_cash", "Cuenta restaurante", "Restaurante", "Restaurant cash module is disabled for this instance."),
            rule("/admin/restaurant/qr-orders", "restaurant_qr", "Pedidos QR", "Restaurante", "Restaurant QR module is disabled for this instance."),
            rule("/admin/restaurant/external-orders", "restaurant_qr", "Pedidos externos", "Restaurante", "Restaurant QR module is disabled for this instance."),
            rule("/admin/restaurant/ingredients", "restaurant_recipes", "Ingredientes restaurante", "Restaurante", "Restaurant recipes module is disabled for this instance."),
            rule("/admin/restaurant/recipes", "restaurant_recipes", "Recetas restaurante", "Restaurante", "Restaurant recipes module is disabled for this instance."),
            rule("/admin/restaurant/recipe", "restaurant_recipes", "Receta restaurante", "Restaurante", "Restaurant recipes module is disabled for this instance."),
            rule("/admin/restaurant/reservations", "restaurant_reservations", "Reservas restaurante", "Restaurante", "Restaurant reservations module is disabled for this instance."),
            rule("/admin/restaurant/table-requests", "restaurant_reservations", "Solicitudes de mesa", "Restaurante", "Restaurant reservations module is disabled for this instance."),
            rule("/admin/restaurant/tables", "restaurant_reservations", "Mesas restaurante", "Restaurante", "Restaurant reservations module is disabled for this instance."),
            rule("/admin/restaurant/menu-items", "restaurant", "Carta restaurante", "Restaurante", "Restaurant module is disabled for this instance."),
            rule("/admin/restaurant/kitchen", "restaurant", "Cocina restaurante", "Restaurante", "Restaurant module is disabled for this instance."),
            rule("/admin/restaurant/orders", "restaurant", "Comandas restaurante", "Restaurante", "Restaurant module is disabled for this instance."),
            rule("/admin/restaurant/settings", "restaurant", "Configuración restaurante", "Restaurante", "Restaurant module is disabled for this instance."),
            rule("/admin/restaurant", "restaurant", "Restaurante", "Restaurante", "Restaurant module is disabled for this instance."),

            // HR and administration.
            rule("/admin/personnel", "hr", "Personal", "Sistema y RRHH", "HR module is disabled for this instance."),
            rule("/admin/job-positions", "hr", "Puestos", "Sistema y RRHH", "HR module is disabled for this instance."),
            rule("/admin/users", "users", "Usuarios", "Sistema y RRHH", "Users module is disabled for this instance."),
            rule("/admin/roles-permissions", "roles_permissions", "Roles y permisos", "Sistema y RRHH", "Roles and permissions module is disabled for this instance."),
            rule("/admin/platform-settings", "platform_settings", "Configuración de plataforma", "Sistema", "Platform settings module is disabled for this instance."),
            rule("/admin/platform", "platform_settings", "Plataforma cliente", "Sistema", "Platform settings module is disabled for this instance."),
            rule("/admin/system-modules", "platform_settings", "Módulos del sistema", "Sistema", "System modules diagnostics remain tied to platform settings.")
    );

    private final SystemModuleService systemModuleService;

    public SystemModuleRouteAccessService(SystemModuleService systemModuleService) {
        this.systemModuleService = systemModuleService;
    }

    public SystemModuleRouteAccessDecision decide(String requestPath) {
        String normalizedPath = normalizePath(requestPath);
        SystemModuleRouteRule matchedRule = findRule(normalizedPath);
        if (matchedRule == null) {
            return SystemModuleRouteAccessDecision.allowedUnprotected(normalizedPath);
        }
        boolean enabled = systemModuleService.isEnabled(matchedRule.moduleKey());
        return SystemModuleRouteAccessDecision.protectedRoute(normalizedPath, matchedRule, enabled);
    }

    public SystemModuleRouteAccessDecision decide(HttpServletRequest request) {
        return decide(normalizedPath(request));
    }

    public List<RouteDiagnostic> diagnostics() {
        return ROUTE_RULES.stream()
                .map(rule -> new RouteDiagnostic(rule, systemModuleService.isEnabled(rule.moduleKey())))
                .toList();
    }

    public List<SystemModuleRouteRule> rules() {
        return ROUTE_RULES;
    }

    private SystemModuleRouteRule findRule(String normalizedPath) {
        for (SystemModuleRouteRule rule : ROUTE_RULES) {
            if (rule.matches(normalizedPath)) {
                return rule;
            }
        }
        return null;
    }

    private static SystemModuleRouteRule rule(String pathPrefix,
                                              String moduleKey,
                                              String label,
                                              String area,
                                              String reason) {
        return new SystemModuleRouteRule(pathPrefix, moduleKey, label, area, reason);
    }

    private static String normalizedPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return normalizePath(uri);
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        String normalized = value.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    public record RouteDiagnostic(SystemModuleRouteRule rule, boolean moduleEnabled) {
        public String statusLabel() {
            return moduleEnabled ? "Allowed" : "Blocked";
        }
    }
}
