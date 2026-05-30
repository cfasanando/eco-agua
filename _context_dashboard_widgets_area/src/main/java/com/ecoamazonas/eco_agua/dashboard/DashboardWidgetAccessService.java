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

        if (!hasAnyAuthority(authorityNames, definition.requiredAuthorities())) {
            return false;
        }

        if (roleCodes.isEmpty()) {
            return false;
        }

        for (String roleCode : roleCodes) {
            Boolean configured = settings.getOrDefault(roleCode, Map.of()).get(definition.key());
            boolean enabledForRole = configured != null ? configured : definition.defaultEnabledForRole(roleCode);
            if (enabledForRole) {
                return true;
            }
        }

        return false;
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
        return authorityNames.stream()
                .filter(this::looksLikeRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
