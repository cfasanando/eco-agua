package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.config.SystemModuleService;
import com.ecoamazonas.eco_agua.user.Role;
import com.ecoamazonas.eco_agua.user.RoleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardWidgetAccessService {

    private final DashboardWidgetRoleSettingRepository settingRepository;
    private final RoleRepository roleRepository;
    private final SystemModuleService systemModuleService;

    public DashboardWidgetAccessService(
            DashboardWidgetRoleSettingRepository settingRepository,
            RoleRepository roleRepository,
            SystemModuleService systemModuleService
    ) {
        this.settingRepository = settingRepository;
        this.roleRepository = roleRepository;
        this.systemModuleService = systemModuleService;
    }

    public List<DashboardWidgetDefinition> getWidgetDefinitions() {
        return definitions();
    }

    public List<DashboardWidgetGroup> getWidgetGroups() {
        Map<String, List<DashboardWidgetDefinition>> grouped = new LinkedHashMap<>();
        for (DashboardWidgetDefinition definition : definitions()) {
            grouped.computeIfAbsent(definition.area(), ignored -> new ArrayList<>()).add(definition);
        }

        List<DashboardWidgetGroup> groups = new ArrayList<>();
        grouped.forEach((area, widgets) -> groups.add(new DashboardWidgetGroup(area, groupSummaryFor(area), widgets)));
        return groups;
    }

    public List<Role> getConfigurableRoles() {
        List<Role> roles = roleRepository.findAll();
        roles.sort(Comparator.comparingInt((Role role) -> roleOrder(role.getCode())).thenComparing(Role::getTitle));
        return roles;
    }

    public Map<String, Boolean> getVisibleWidgetMap(Authentication authentication) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        Set<String> authorityNames = authorityNames(authentication);
        Set<String> roleCodes = roleCodes(authorityNames);
        Map<String, Map<String, Boolean>> settings = settingsByRole(roleCodes);

        for (DashboardWidgetDefinition definition : definitions()) {
            result.put(definition.key(), isVisible(definition, roleCodes, authorityNames, settings));
        }

        return result;
    }

    public int countVisibleWidgets(Authentication authentication) {
        int count = 0;
        for (Boolean visible : getVisibleWidgetMap(authentication).values()) {
            if (Boolean.TRUE.equals(visible)) {
                count++;
            }
        }
        return count;
    }

    public Map<String, Map<String, Boolean>> getRoleWidgetMatrix() {
        List<Role> roles = getConfigurableRoles();
        List<DashboardWidgetDefinition> widgets = definitions();
        Map<String, Map<String, Boolean>> matrix = new LinkedHashMap<>();

        for (Role role : roles) {
            Map<String, Boolean> row = new LinkedHashMap<>();
            for (DashboardWidgetDefinition widget : widgets) {
                row.put(widget.key(), settingRepository.findByRoleCodeAndWidgetKey(role.getCode(), widget.key())
                        .map(DashboardWidgetRoleSetting::isEnabled)
                        .orElse(widget.defaultEnabledForRole(role.getCode())));
            }
            matrix.put(role.getCode(), row);
        }

        return matrix;
    }

    @Transactional
    public void updateRoleWidgetSettings(Map<String, Set<String>> enabledWidgetsByRole) {
        for (Role role : getConfigurableRoles()) {
            Set<String> enabledWidgets = enabledWidgetsByRole.getOrDefault(role.getCode(), Set.of());
            for (DashboardWidgetDefinition widget : definitions()) {
                boolean enabled = enabledWidgets.contains(widget.key());
                DashboardWidgetRoleSetting setting = settingRepository
                        .findByRoleCodeAndWidgetKey(role.getCode(), widget.key())
                        .orElseGet(() -> new DashboardWidgetRoleSetting(role.getCode(), widget.key(), enabled));
                setting.setEnabled(enabled);
                settingRepository.save(setting);
            }
        }
    }

    private boolean isVisible(
            DashboardWidgetDefinition definition,
            Set<String> roleCodes,
            Set<String> authorityNames,
            Map<String, Map<String, Boolean>> settings
    ) {
        if (!isModuleEnabled(definition.moduleKey())) {
            return false;
        }

        if (roleCodes.isEmpty()) {
            return false;
        }

        boolean enabledForAnyRole = false;
        for (String roleCode : roleCodes) {
            Boolean configured = settings.getOrDefault(roleCode, Map.of()).get(definition.key());
            boolean enabledForRole = configured != null ? configured : definition.defaultEnabledForRole(roleCode);
            if (enabledForRole) {
                enabledForAnyRole = true;
                break;
            }
        }

        if (!enabledForAnyRole) {
            return false;
        }

        // The dashboard widget matrix is already configured by role.
        // Some legacy demo roles do not have all new permission authorities yet,
        // so a role-enabled widget must remain visible while route security still
        // protects the destination URLs and actions.
        return hasAnyAuthority(authorityNames, definition.requiredAuthorities())
                || hasAnyAuthority(authorityNames, definition.defaultRoleCodes());
    }

    private boolean isModuleEnabled(String moduleKey) {
        if (moduleKey == null || moduleKey.isBlank()) {
            return true;
        }
        return systemModuleService.isEnabled(moduleKey);
    }

    private boolean hasAnyAuthority(Set<String> authorityNames, Set<String> requiredAuthorities) {
        if (requiredAuthorities.isEmpty()) {
            return true;
        }
        for (String authority : requiredAuthorities) {
            if (authorityNames.contains(authority)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> authorityNames(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> roleCodes(Set<String> authorityNames) {
        Set<String> roles = authorityNames.stream()
                .filter(this::looksLikeRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Keep backward compatibility between legacy demo roles and the new
        // business-area roles used by the dashboard widget matrix.
        if (roles.contains("ADMIN_PRINC") || roles.contains("ADMIN")) {
            roles.add("ROLE_OWNER");
        }
        if (roles.contains("ADMIN_MKT")) {
            roles.add("ROLE_MARKETING");
        }
        if (roles.contains("ADMIN_RRHH")) {
            roles.add("ROLE_HR");
        }
        if (roles.contains("ADMIN_CONT")) {
            roles.add("ROLE_FINANCE");
        }
        if (roles.contains("SUPERVISOR")) {
            roles.add("ROLE_MANAGEMENT");
            roles.add("ROLE_SALES");
            roles.add("ROLE_LOGISTICS");
        }
        if (roles.contains("OPERARIO")) {
            roles.add("ROLE_SALES");
            roles.add("ROLE_LOGISTICS");
        }

        return roles;
    }

    private boolean looksLikeRole(String authority) {
        if ("ROLE_ANONYMOUS".equals(authority)) {
            return false;
        }
        return authority.startsWith("ROLE_")
                || authority.equals("ADMIN_PRINC")
                || authority.equals("ADMIN")
                || authority.equals("ADMIN_CONT")
                || authority.equals("ADMIN_MKT")
                || authority.equals("ADMIN_RRHH")
                || authority.equals("SUPERVISOR")
                || authority.equals("OPERARIO");
    }

    private Map<String, Map<String, Boolean>> settingsByRole(Set<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return Map.of();
        }

        return settingRepository.findByRoleCodeIn(roleCodes).stream()
                .collect(Collectors.groupingBy(
                        DashboardWidgetRoleSetting::getRoleCode,
                        LinkedHashMap::new,
                        Collectors.toMap(
                                DashboardWidgetRoleSetting::getWidgetKey,
                                DashboardWidgetRoleSetting::isEnabled,
                                (left, right) -> right,
                                LinkedHashMap::new
                        )
                ));
    }

    private int roleOrder(String roleCode) {
        return switch (roleCode) {
            case "ROLE_OWNER", "ADMIN_PRINC", "ADMIN" -> 1;
            case "ROLE_MANAGEMENT" -> 2;
            case "ROLE_SALES", "OPERARIO", "SUPERVISOR" -> 3;
            case "ROLE_MARKETING", "ADMIN_MKT" -> 4;
            case "ROLE_FINANCE", "ADMIN_CONT" -> 5;
            case "ROLE_LOGISTICS" -> 6;
            case "ROLE_PRODUCTION" -> 7;
            case "ROLE_HR", "ADMIN_RRHH" -> 8;
            case "ROLE_READONLY" -> 9;
            default -> 99;
        };
    }

    private String groupSummaryFor(String area) {
        return switch (area) {
            case "Ventas y atención" -> "Widgets related to orders, customers, daily sales and pending deliveries.";
            case "Finanzas" -> "Widgets related to cashflow, expenses, receivables and break-even analysis.";
            case "Marketing y contenido" -> "Widgets related to promotions, campaigns, blog content and public catalog visibility.";
            case "Recursos humanos" -> "Widgets related to personnel, job positions, user access and employee obligations.";
            case "Gestión" -> "General business management widgets used by administrators and supervisors.";
            default -> "Configurable dashboard widgets.";
        };
    }

    private List<DashboardWidgetDefinition> definitions() {
        List<DashboardWidgetDefinition> widgets = new ArrayList<>();

        widgets.add(new DashboardWidgetDefinition(
                "operations",
                "Ventas y atención",
                "Operaciones",
                "Quick access to register new orders.",
                "income",
                set("ROLE_OWNER", "ROLE_SALES", "ADMIN_PRINC", "ADMIN", "OPERARIO"),
                set("ROLE_OWNER", "ROLE_SALES", "ADMIN_PRINC", "ADMIN", "OPERARIO", "crear_ventas", "editar_ventas")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "possible-orders",
                "Ventas y atención",
                "Posibles pedidos",
                "Clients with higher probability of buying today.",
                "crm",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ROLE_SALES", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ROLE_SALES", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT", "SUPERVISOR", "ver_clientes", "ver_ventas")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "sales-today",
                "Ventas y atención",
                "Ventas del día",
                "Paid sales registered today.",
                "income",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_SALES", "ROLE_FINANCE", "ROLE_READONLY", "ADMIN_PRINC", "ADMIN", "ADMIN_CONT", "SUPERVISOR", "OPERARIO"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_SALES", "ROLE_FINANCE", "ROLE_READONLY", "ADMIN_PRINC", "ADMIN", "ADMIN_CONT", "SUPERVISOR", "OPERARIO", "ver_ventas", "ver_ingresos")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "weekly-cash-flow",
                "Finanzas",
                "Flujo de caja semanal",
                "Weekly income, expense and net cashflow summary.",
                "cashflow",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_FINANCE", "ROLE_READONLY", "ADMIN_PRINC", "ADMIN", "ADMIN_CONT"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_FINANCE", "ROLE_READONLY", "ADMIN_PRINC", "ADMIN", "ADMIN_CONT", "ver_cashflow", "ver_finanzas")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "break-even-month",
                "Finanzas",
                "Punto de equilibrio del mes",
                "Monthly break-even status by product.",
                "break_even",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_FINANCE", "ROLE_READONLY", "ADMIN_PRINC", "ADMIN", "ADMIN_CONT"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_FINANCE", "ROLE_READONLY", "ADMIN_PRINC", "ADMIN", "ADMIN_CONT", "ver_punto_equilibrio", "ver_finanzas")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "expenses-today",
                "Finanzas",
                "Gastos del día",
                "Daily expense list and quick expense entry.",
                "expenses",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_FINANCE", "ADMIN_PRINC", "ADMIN", "ADMIN_CONT"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_FINANCE", "ADMIN_PRINC", "ADMIN", "ADMIN_CONT", "ver_egresos", "crear_egresos", "editar_egresos")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "credit-orders",
                "Finanzas",
                "Pedidos fiados",
                "Pending receivables from credit orders.",
                "income",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_SALES", "ROLE_FINANCE", "ADMIN_PRINC", "ADMIN", "ADMIN_CONT", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_SALES", "ROLE_FINANCE", "ADMIN_PRINC", "ADMIN", "ADMIN_CONT", "SUPERVISOR", "gestionar_cuentas_cobrar", "ver_ventas", "ver_ingresos")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "requested-orders",
                "Ventas y atención",
                "Pedidos del día por entregar",
                "Orders requested today and pending delivery.",
                "delivery",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_SALES", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "OPERARIO"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_SALES", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "OPERARIO", "ver_delivery", "ver_ventas")
        ));



        widgets.add(new DashboardWidgetDefinition(
                "management-overview",
                "Gestión",
                "Estado del negocio",
                "Executive business overview with operational and financial signals.",
                "business_overview",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_READONLY", "ADMIN_PRINC", "ADMIN", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_READONLY", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "ver_dashboard", "ver_reportes")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "management-followup",
                "Gestión",
                "Seguimiento del mes",
                "Monthly follow-up for goals, pending actions and business control.",
                "monthly_followup",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_READONLY", "ADMIN_PRINC", "ADMIN", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_READONLY", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "ver_dashboard", "ver_reportes")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "management-modules",
                "Gestión",
                "Módulos activos",
                "Review which system modules are active for this client.",
                "platform_settings",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ADMIN_PRINC", "ADMIN", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "admin_config")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "management-widget-config",
                "Gestión",
                "Widgets por rol",
                "Configure which home widgets each business role should see.",
                "dashboard",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ADMIN_PRINC", "ADMIN"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ADMIN_PRINC", "ADMIN", "admin_config")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "logistics-products",
                "Logística y almacén",
                "Catálogo operativo",
                "Products that logistics can review before delivery, stock and coordination.",
                "products",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_LOGISTICS", "ROLE_SALES", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "OPERARIO"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_LOGISTICS", "ROLE_SALES", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "OPERARIO", "ver_productos", "administra_productos")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "logistics-stock",
                "Logística y almacén",
                "Stock de productos",
                "Quick access to product stock and movement review.",
                "warehouse",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "OPERARIO"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "OPERARIO", "ver_inventario", "administra_inventario")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "logistics-delivery",
                "Logística y almacén",
                "Entregas pendientes",
                "Delivery control and pending dispatch coordination.",
                "delivery",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_LOGISTICS", "ROLE_SALES", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "OPERARIO"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_LOGISTICS", "ROLE_SALES", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "OPERARIO", "ver_delivery", "ver_ventas")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "logistics-zones",
                "Logística y almacén",
                "Zonas de reparto",
                "Delivery zones and coverage rules used by the public portal and operations.",
                "delivery",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "ver_delivery", "admin_config")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "logistics-suppliers",
                "Logística y almacén",
                "Proveedores",
                "Supplier list and purchase coordination support.",
                "suppliers",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "ver_proveedores", "administra_proveedores")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "readonly-summary",
                "Consulta",
                "Resumen del negocio",
                "Read-only access to the business summary.",
                "business_overview",
                set("ROLE_READONLY"),
                set("ROLE_READONLY", "ver_dashboard", "ver_reportes")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "readonly-followup",
                "Consulta",
                "Seguimiento mensual",
                "Read-only monthly follow-up without edit actions.",
                "monthly_followup",
                set("ROLE_READONLY"),
                set("ROLE_READONLY", "ver_dashboard", "ver_reportes")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "readonly-reports",
                "Consulta",
                "Reportes disponibles",
                "Safe access points for review and audit.",
                "dashboard",
                set("ROLE_READONLY"),
                set("ROLE_READONLY", "ver_dashboard", "ver_reportes")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "readonly-modules",
                "Consulta",
                "Módulos visibles",
                "Informational view of available active areas.",
                "dashboard",
                set("ROLE_READONLY"),
                set("ROLE_READONLY", "ver_dashboard", "ver_reportes")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "readonly-actions",
                "Consulta",
                "Acciones permitidas",
                "Read-only guidance: review information without changing business data.",
                "dashboard",
                set("ROLE_READONLY"),
                set("ROLE_READONLY", "ver_dashboard", "ver_reportes")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "production-today",
                "Producción",
                "Producción del día",
                "Production module quick access for Agua Eco or clients that use production.",
                "production",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_PRODUCTION", "ADMIN_PRINC", "ADMIN", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_PRODUCTION", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "ver_produccion", "administra_produccion")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "production-supplies",
                "Producción",
                "Insumos críticos",
                "Supply catalog and low stock review for production.",
                "supplies",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_PRODUCTION", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_PRODUCTION", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "ver_insumos", "administra_insumos")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "production-supply-stock",
                "Producción",
                "Stock de insumos",
                "Supply stock and movement control.",
                "supplies",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_PRODUCTION", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_PRODUCTION", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "ver_inventario", "ver_insumos")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "production-containers",
                "Producción",
                "Envases retornables",
                "Container movements and client container control.",
                "containers",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_PRODUCTION", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_PRODUCTION", "ROLE_LOGISTICS", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "ver_envases", "administra_envases")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "production-reorder",
                "Producción",
                "Agenda de reposición",
                "Recurring refill or reorder follow-up for water delivery businesses.",
                "reorder",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_PRODUCTION", "ROLE_LOGISTICS", "ROLE_SALES", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "OPERARIO"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_PRODUCTION", "ROLE_LOGISTICS", "ROLE_SALES", "ADMIN_PRINC", "ADMIN", "SUPERVISOR", "OPERARIO", "ver_recompras", "ver_delivery")
        ));


        widgets.add(new DashboardWidgetDefinition(
                "marketing-promotions",
                "Marketing y contenido",
                "Promociones activas",
                "Current promotions and campaigns that should be pushed in the public portal and WhatsApp.",
                "promotions",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT", "ver_marketing", "ver_promociones", "administra_marketing", "administra_promos")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "marketing-campaigns",
                "Marketing y contenido",
                "Campañas del mes",
                "Marketing assets and active messages to keep aligned with sales.",
                "marketing",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT", "ver_marketing", "administra_marketing")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "marketing-blog",
                "Marketing y contenido",
                "Últimos artículos del blog",
                "Published and draft content available for trust, education and sales support.",
                "blog",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT", "ver_blog", "administra_blog", "ver_marketing")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "marketing-featured-products",
                "Marketing y contenido",
                "Productos destacados",
                "Featured products that can be used in posts, catalog banners and campaigns.",
                "products",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT", "ver_productos", "ver_marketing", "administra_marketing")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "marketing-actions",
                "Marketing y contenido",
                "Acciones sugeridas de contenido",
                "Practical actions to improve public visibility, social trust and campaign readiness.",
                "marketing",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ADMIN_PRINC", "ADMIN", "ADMIN_MKT", "ver_marketing", "administra_marketing")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "hr-staff",
                "Recursos humanos",
                "Personal activo",
                "Active employees and quick access to personnel management.",
                "hr",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_HR", "ADMIN_PRINC", "ADMIN", "ADMIN_RRHH"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_HR", "ADMIN_PRINC", "ADMIN", "ADMIN_RRHH", "ver_personal", "administra_personal")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "hr-positions",
                "Recursos humanos",
                "Cargos registrados",
                "Active job positions and payment modes configured for employees.",
                "hr",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_HR", "ADMIN_PRINC", "ADMIN", "ADMIN_RRHH"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_HR", "ADMIN_PRINC", "ADMIN", "ADMIN_RRHH", "ver_personal", "administra_personal")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "hr-users",
                "Recursos humanos",
                "Usuarios activos",
                "System users enabled for internal access.",
                "users",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_HR", "ADMIN_PRINC", "ADMIN", "ADMIN_RRHH"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_HR", "ADMIN_PRINC", "ADMIN", "ADMIN_RRHH", "administra_usuarios", "ver_personal")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "hr-roles",
                "Recursos humanos",
                "Roles configurados",
                "Roles and permissions available for assigning secure system access.",
                "roles_permissions",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_HR", "ADMIN_PRINC", "ADMIN", "ADMIN_RRHH"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_HR", "ADMIN_PRINC", "ADMIN", "ADMIN_RRHH", "administra_roles", "admin_config")
        ));

        widgets.add(new DashboardWidgetDefinition(
                "hr-obligations",
                "Recursos humanos",
                "Obligaciones pendientes",
                "Pending employee loans, advances or obligations that need follow-up.",
                "hr",
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_HR", "ROLE_FINANCE", "ADMIN_PRINC", "ADMIN", "ADMIN_RRHH", "ADMIN_CONT"),
                set("ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_HR", "ROLE_FINANCE", "ADMIN_PRINC", "ADMIN", "ADMIN_RRHH", "ADMIN_CONT", "ver_pagos_personal", "administra_pagos_personal", "ver_personal")
        ));

        return widgets;
    }

    private Set<String> set(String... values) {
        return Arrays.stream(values).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public record DashboardWidgetDefinition(
            String key,
            String area,
            String title,
            String description,
            String moduleKey,
            Set<String> defaultRoleCodes,
            Set<String> requiredAuthorities
    ) {
        public boolean defaultEnabledForRole(String roleCode) {
            return defaultRoleCodes.contains(roleCode);
        }
    }

    public record DashboardWidgetGroup(
            String area,
            String summary,
            List<DashboardWidgetDefinition> widgets
    ) {
    }
}
