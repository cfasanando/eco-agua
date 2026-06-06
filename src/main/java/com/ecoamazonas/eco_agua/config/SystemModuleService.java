package com.ecoamazonas.eco_agua.config;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SystemModuleService {

    private static final String SETTING_PREFIX = "module.";
    private static final String SETTING_SUFFIX = ".enabled";
    private static final String CATEGORY = "system_modules";

    private final PlatformSettingRepository platformSettingRepository;
    private final PlatformSettingService platformSettingService;
    private final ClientFeatureProperties clientFeatureProperties;

    public SystemModuleService(PlatformSettingRepository platformSettingRepository,
                               PlatformSettingService platformSettingService,
                               ClientFeatureProperties clientFeatureProperties) {
        this.platformSettingRepository = platformSettingRepository;
        this.platformSettingService = platformSettingService;
        this.clientFeatureProperties = clientFeatureProperties;
    }

    public Map<String, Boolean> getModuleFlags() {
        Map<String, Boolean> modules = new LinkedHashMap<>();
        moduleDefinitions().forEach(module -> modules.put(module.key(), isEnabled(module.key())));
        return modules;
    }

    public List<ModuleDefinition> getModuleDefinitions() {
        return moduleDefinitions().stream()
                .map(module -> module.withRuntimeState(defaultValueFor(module.key()), isEnabled(module.key())))
                .toList();
    }

    public List<ModuleGroup> getModuleGroups() {
        Map<String, List<ModuleDefinition>> groupedModules = new LinkedHashMap<>();
        for (ModuleDefinition module : getModuleDefinitions()) {
            groupedModules.computeIfAbsent(module.area(), ignored -> new ArrayList<>()).add(module);
        }

        List<ModuleGroup> groups = new ArrayList<>();
        groupedModules.forEach((area, modules) -> groups.add(new ModuleGroup(area, groupSummaryFor(area), modules)));
        return groups;
    }

    public boolean isEnabled(String key) {
        if (isLocked(key)) {
            return true;
        }
        return isEnabled(key, defaultValueFor(key));
    }

    public boolean isAnyEnabled(String... keys) {
        for (String key : keys) {
            if (isEnabled(key)) {
                return true;
            }
        }
        return false;
    }

    public void applyPreset(String presetKey) {
        String normalizedPreset = normalizePresetKey(presetKey);

        for (ModuleDefinition module : getModuleDefinitions()) {
            if (module.locked()) {
                continue;
            }

            boolean enabled = switch (normalizedPreset) {
                case "aguaeco" -> module.recommendedForAguaEco();
                case "belen" -> module.recommendedForBelen();
                default -> throw new IllegalArgumentException("Unsupported module preset: " + presetKey);
            };

            updateModuleFlag(module.key(), enabled);
        }
    }

    public String presetLabel(String presetKey) {
        return switch (normalizePresetKey(presetKey)) {
            case "aguaeco" -> "Agua Eco";
            case "belen" -> "Productos de la Selva Belén";
            default -> "Preset desconocido";
        };
    }

    public void updateModuleFlag(String key, boolean enabled) {
        ModuleDefinition definition = findDefinition(key);
        if (definition == null || definition.locked()) {
            return;
        }

        PlatformSetting setting = platformSettingService.ensure(
                settingName(key),
                Boolean.toString(defaultValueFor(key)),
                "boolean",
                CATEGORY,
                definition.description()
        );
        setting.setValue(Boolean.toString(enabled));
        platformSettingRepository.save(setting);
    }

    public void ensureDefaults() {
        moduleDefinitions().forEach(module -> {
            PlatformSetting setting = platformSettingService.ensure(
                    settingName(module.key()),
                    Boolean.toString(module.locked() || defaultValueFor(module.key())),
                    "boolean",
                    CATEGORY,
                    module.description()
            );

            if (module.locked() && !parseBoolean(setting.getValue(), true)) {
                setting.setValue("true");
                platformSettingRepository.save(setting);
            }
        });
    }

    public String settingName(String key) {
        return SETTING_PREFIX + key + SETTING_SUFFIX;
    }

    private boolean isEnabled(String key, boolean defaultValue) {
        String rawValue = platformSettingService.get(settingName(key), Boolean.toString(defaultValue));
        return parseBoolean(rawValue, defaultValue);
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized)) {
            return true;
        }

        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized) || "off".equals(normalized)) {
            return false;
        }

        return defaultValue;
    }


    private String normalizePresetKey(String presetKey) {
        if (presetKey == null || presetKey.isBlank()) {
            throw new IllegalArgumentException("Module preset is required.");
        }

        String normalized = presetKey.trim().toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "");

        if (!"aguaeco".equals(normalized) && !"belen".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported module preset: " + presetKey);
        }

        return normalized;
    }

    private boolean defaultValueFor(String key) {
        return switch (key) {
            case "containers" -> clientFeatureProperties.isContainers();
            case "delivery" -> clientFeatureProperties.isDelivery();
            case "production" -> clientFeatureProperties.isProduction();
            case "reorder" -> clientFeatureProperties.isReorder();
            case "marketing" -> clientFeatureProperties.isMarketing();
            case "blog" -> clientFeatureProperties.isBlog();
            case "testimonials" -> clientFeatureProperties.isTestimonials();
            case "public_catalog" -> clientFeatureProperties.isPublicCatalog();
            case "supplies" -> clientFeatureProperties.isSupplies();
            case "fixed_costs" -> clientFeatureProperties.isFixedCosts();
            case "break_even" -> clientFeatureProperties.isBreakEven();
            case "price_simulator" -> clientFeatureProperties.isPriceSimulator();
            default -> true;
        };
    }

    private boolean isLocked(String key) {
        ModuleDefinition definition = findDefinition(key);
        return definition != null && definition.locked();
    }

    private ModuleDefinition findDefinition(String key) {
        for (ModuleDefinition module : moduleDefinitions()) {
            if (module.key().equals(key)) {
                return module;
            }
        }
        return null;
    }

    private String groupSummaryFor(String area) {
        return switch (area) {
            case "Principal" -> "Core dashboards and business visibility pages.";
            case "Comercial y ventas" -> "Client, sales, promotion, delivery and reorder workflows.";
            case "Finanzas y contabilidad" -> "Income, expenses, accounting, cashflow and pricing analysis.";
            case "Inventario y operación" -> "Products, categories, warehouse, supplies, containers and production.";
            case "Marketing y portal público" -> "Campaigns, blog, testimonials, public portal and public catalog.";
            case "Sistema y RRHH" -> "Internal users, roles, permissions, staff and platform configuration.";
            default -> "Configurable system modules.";
        };
    }

    private List<ModuleDefinition> moduleDefinitions() {
        List<ModuleDefinition> modules = new ArrayList<>();

        modules.add(new ModuleDefinition("dashboard", "Principal", "Inicio administrativo", "Private dashboard home.", true, true, true, true, false));
        modules.add(new ModuleDefinition("business_overview", "Principal", "Estado del negocio", "Business overview dashboard.", true, true, true, false, false));
        modules.add(new ModuleDefinition("monthly_followup", "Principal", "Seguimiento del mes", "Monthly follow-up dashboard.", true, true, true, false, false));
        modules.add(new ModuleDefinition("commercial_daily", "Principal", "Panel comercial diario", "Daily commercial dashboard.", true, true, true, false, false));

        modules.add(new ModuleDefinition("crm", "Comercial y ventas", "Comercial / CRM", "CRM and commercial management group.", true, true, true, false, false));
        modules.add(new ModuleDefinition("clients", "Comercial y ventas", "Clientes y cartera", "Client and client portfolio management.", true, true, true, false, false));
        modules.add(new ModuleDefinition("promotions", "Comercial y ventas", "Promociones", "Promotion management.", true, true, true, false, false));
        modules.add(new ModuleDefinition("delivery", "Comercial y ventas", "Delivery / zonas", "Delivery zones and daily delivery tracking.", true, true, false, false, false));
        modules.add(new ModuleDefinition("reorder", "Comercial y ventas", "Agenda de reposición", "Recurring reorder agenda.", true, false, false, false, false));

        modules.add(new ModuleDefinition("income", "Finanzas y contabilidad", "Ingresos y cobros", "Income, sales by date and accounts receivable.", true, true, true, false, false));
        modules.add(new ModuleDefinition("expenses", "Finanzas y contabilidad", "Egresos y compras", "Expenses and accounts payable.", true, true, true, false, false));
        modules.add(new ModuleDefinition("fixed_costs", "Finanzas y contabilidad", "Costos fijos", "Monthly fixed cost management.", true, true, false, false, false));
        modules.add(new ModuleDefinition("suppliers", "Finanzas y contabilidad", "Proveedores", "Supplier management.", true, true, true, false, false));
        modules.add(new ModuleDefinition("finance", "Finanzas y contabilidad", "Finanzas y control", "Finance and control group.", true, true, true, false, false));
        modules.add(new ModuleDefinition("accounting", "Finanzas y contabilidad", "Contabilidad", "Sales and purchase accounting registries.", true, true, true, false, false));
        modules.add(new ModuleDefinition("cashflow", "Finanzas y contabilidad", "Flujo de caja", "Cashflow module.", true, true, true, false, false));
        modules.add(new ModuleDefinition("break_even", "Finanzas y contabilidad", "Punto de equilibrio", "Break-even analysis.", true, true, false, false, false));
        modules.add(new ModuleDefinition("price_simulator", "Finanzas y contabilidad", "Simulador de precios", "Price simulator.", true, true, false, false, false));

        modules.add(new ModuleDefinition("inventory", "Inventario y operación", "Inventario y catálogo", "Inventory and catalog management group.", true, true, true, false, false));
        modules.add(new ModuleDefinition("products", "Inventario y operación", "Productos", "Product administration.", true, true, true, false, false));
        modules.add(new ModuleDefinition("categories", "Inventario y operación", "Categorías", "Category administration.", true, true, true, false, false));
        modules.add(new ModuleDefinition("warehouse", "Inventario y operación", "Almacén / stock", "Warehouse and stock views.", true, true, true, false, false));
        modules.add(new ModuleDefinition("supplies", "Inventario y operación", "Insumos", "Supplies and supply stock control.", true, false, false, false, false));
        modules.add(new ModuleDefinition("containers", "Inventario y operación", "Envases retornables", "Returnable containers module.", true, false, false, false, false));
        modules.add(new ModuleDefinition("production", "Inventario y operación", "Producción / planta", "Production and plant module.", true, false, false, false, false));

        modules.add(new ModuleDefinition("marketing", "Marketing y portal público", "Marketing / campañas", "Marketing campaign management.", true, true, true, false, false));
        modules.add(new ModuleDefinition("blog", "Marketing y portal público", "Blog / consejos", "Blog and public content management.", true, true, true, false, false));
        modules.add(new ModuleDefinition("testimonials", "Marketing y portal público", "Testimonios", "Testimonial management and display.", true, false, false, false, false));
        modules.add(new ModuleDefinition("public_site", "Marketing y portal público", "Portal público", "Public website module.", true, true, true, false, false));
        modules.add(new ModuleDefinition("public_catalog", "Marketing y portal público", "Catálogo público", "Public catalog module.", true, true, true, false, false));

        modules.add(new ModuleDefinition("hr", "Sistema y RRHH", "Recursos humanos", "HR and personnel management.", true, true, true, false, false));
        modules.add(new ModuleDefinition("users", "Sistema y RRHH", "Usuarios", "Internal user administration.", true, true, true, false, false));
        modules.add(new ModuleDefinition("roles_permissions", "Sistema y RRHH", "Roles y permisos", "Role and permission administration.", true, true, true, false, false));
        modules.add(new ModuleDefinition("platform_settings", "Sistema y RRHH", "Configuración de plataforma", "Platform and client settings administration.", true, true, true, true, false));

        return modules;
    }

    public record ModuleDefinition(
            String key,
            String area,
            String label,
            String description,
            boolean recommendedForAguaEco,
            boolean recommendedForBelen,
            boolean defaultEnabled,
            boolean locked,
            boolean enabled
    ) {
        ModuleDefinition withRuntimeState(boolean defaultEnabled, boolean enabled) {
            return new ModuleDefinition(
                    key,
                    area,
                    label,
                    description,
                    recommendedForAguaEco,
                    recommendedForBelen,
                    defaultEnabled,
                    locked,
                    locked || enabled
            );
        }

        public String settingVariable() {
            return SETTING_PREFIX + key + SETTING_SUFFIX;
        }
    }

    public record ModuleGroup(String name, String summary, List<ModuleDefinition> modules) {
    }
}
