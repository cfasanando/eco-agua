package com.ecoamazonas.eco_agua.config;

import com.ecoamazonas.eco_agua.dashboard.DashboardWidgetAccessService;
import com.ecoamazonas.eco_agua.dashboard.DashboardAreaWidgetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import org.springframework.security.core.GrantedAuthority;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalModelAttributes {

    private final BusinessProperties businessProperties;
    private final ClientFeatureProperties clientFeatureProperties;
    private final PlatformSettingService platformSettingService;
    private final SystemModuleService systemModuleService;
    private final DashboardWidgetAccessService dashboardWidgetAccessService;
    private final DashboardAreaWidgetService dashboardAreaWidgetService;

    public GlobalModelAttributes(
            BusinessProperties businessProperties,
            ClientFeatureProperties clientFeatureProperties,
            PlatformSettingService platformSettingService,
            SystemModuleService systemModuleService,
            DashboardWidgetAccessService dashboardWidgetAccessService,
            DashboardAreaWidgetService dashboardAreaWidgetService
    ) {
        this.businessProperties = businessProperties;
        this.clientFeatureProperties = clientFeatureProperties;
        this.platformSettingService = platformSettingService;
        this.systemModuleService = systemModuleService;
        this.dashboardWidgetAccessService = dashboardWidgetAccessService;
        this.dashboardAreaWidgetService = dashboardAreaWidgetService;
    }

    @ModelAttribute
    public void addBusinessAttributes(Model model, Authentication authentication, HttpServletRequest request) {
        model.addAttribute("businessProfileCode", businessProperties.getProfileCode());
        model.addAttribute("businessName", businessProperties.getName());
        model.addAttribute("businessShortName", businessProperties.getShortName());
        model.addAttribute("businessTagline", businessProperties.getTagline());
        model.addAttribute("businessType", businessProperties.getType());
        model.addAttribute("businessAdminTitle", businessProperties.getAdminTitle());
        model.addAttribute("businessLogo", businessProperties.getLogo());
        model.addAttribute("businessAdminLogo", businessProperties.getAdminLogo());
        model.addAttribute("businessWhatsappNumber", businessProperties.getWhatsappNumber());
        model.addAttribute("businessCatalogWhatsappIntro", businessProperties.getCatalogWhatsappIntro());
        model.addAttribute("businessProductLabel", businessProperties.getProductLabel());
        model.addAttribute("businessProductPluralLabel", businessProperties.getProductPluralLabel());
        model.addAttribute("businessCustomerLabel", businessProperties.getCustomerLabel());
        model.addAttribute("businessCustomerPluralLabel", businessProperties.getCustomerPluralLabel());
        model.addAttribute("businessSupplierLabel", businessProperties.getSupplierLabel());
        model.addAttribute("businessSupplierPluralLabel", businessProperties.getSupplierPluralLabel());
        model.addAttribute("businessSupplyLabel", businessProperties.getSupplyLabel());
        model.addAttribute("businessSupplyPluralLabel", businessProperties.getSupplyPluralLabel());
        model.addAttribute("businessDeliveryPersonLabel", businessProperties.getDeliveryPersonLabel());
        model.addAttribute("businessDeliveryLabel", businessProperties.getDeliveryLabel());
        model.addAttribute("businessContainerLabel", businessProperties.getContainerLabel());
        model.addAttribute("businessContainerPluralLabel", businessProperties.getContainerPluralLabel());
        model.addAttribute("businessProductionLabel", businessProperties.getProductionLabel());
        model.addAttribute("businessReorderLabel", businessProperties.getReorderLabel());
        model.addAttribute("businessProfilePriceHelpText", businessProperties.getProfilePriceHelpText());

        addModuleAttributes(model);
        Map<String, Boolean> visibleWidgets = addDashboardWidgetAttributes(model, authentication);
        addDashboardRoleDashboardAttributes(model, authentication);

        String platformName = platformSettingService.get("platform.name", businessProperties.getName());
        String platformTagline = platformSettingService.get("platform.tagline", businessProperties.getTagline());
        String platformLogo = platformSettingService.get("platform.logo", businessProperties.getLogo());

        String adminBrandTitle = platformSettingService.get("admin.brand.title", platformName);
        String adminBrandSubtitle = platformSettingService.get("admin.brand.subtitle", businessProperties.getAdminTitle());
        String adminBrandLogo = platformSettingService.get("admin.brand.logo", platformLogo);
        String adminHomeBackgroundImage = platformSettingService.get("admin.home.background_image", "");
        String adminHomeBackgroundColor = platformSettingService.get("admin.home.background_color", "#f3f5f9");
        String adminHomeBackgroundOverlay = platformSettingService.get("admin.home.background_overlay", "rgba(243,245,249,0.88)");

        model.addAttribute("adminPlatformName", platformName);
        model.addAttribute("adminPlatformTagline", platformTagline);
        model.addAttribute("adminBrandTitle", adminBrandTitle);
        model.addAttribute("adminBrandSubtitle", adminBrandSubtitle);
        model.addAttribute("adminBrandLogo", adminBrandLogo);
        model.addAttribute("adminHomeBackgroundImage", adminHomeBackgroundImage);
        model.addAttribute("adminHomeBackgroundColor", adminHomeBackgroundColor);
        model.addAttribute("adminHomeBackgroundOverlay", adminHomeBackgroundOverlay);
    }

    private Map<String, Boolean> addDashboardWidgetAttributes(Model model, Authentication authentication) {
        Map<String, Boolean> allowedWidgets = dashboardWidgetAccessService.getAllowedWidgetMap(authentication);
        Map<String, Boolean> visibleWidgets = dashboardWidgetAccessService.getVisibleWidgetMap(authentication);
        model.addAttribute("dashboardAllowedWidgets", allowedWidgets);
        model.addAttribute("dashboardAllowedWidgetCount", allowedWidgets.values().stream().filter(Boolean.TRUE::equals).count());
        model.addAttribute("dashboardVisibleWidgets", visibleWidgets);
        model.addAttribute("dashboardVisibleWidgetCount", visibleWidgets.values().stream().filter(Boolean.TRUE::equals).count());
        return visibleWidgets;
    }

    private void addDashboardRoleDashboardAttributes(Model model, Authentication authentication) {
        Set<String> authorities = authorityNames(authentication);

        boolean marketingRole = hasAnyAuthority(authorities,
                "ROLE_MARKETING", "ADMIN_MKT");
        boolean hrRole = hasAnyAuthority(authorities,
                "ROLE_HR", "ADMIN_RRHH");

        boolean marketingAreaVisible = marketingRole
                && systemModuleService.isAnyEnabled("marketing", "blog", "promotions", "products", "public_catalog");
        boolean hrAreaVisible = hrRole
                && systemModuleService.isAnyEnabled("hr", "users", "roles_permissions");

        model.addAttribute("dashboardMarketingAreaVisible", marketingAreaVisible);
        model.addAttribute("dashboardHrAreaVisible", hrAreaVisible);
        model.addAttribute("dashboardAreaRoleDashboardVisible", marketingAreaVisible || hrAreaVisible);

        if (marketingAreaVisible) {
            model.addAttribute("dashboardMarketingSnapshot", dashboardAreaWidgetService.buildMarketingSnapshot());
        } else {
            model.addAttribute("dashboardMarketingSnapshot", null);
        }

        if (hrAreaVisible) {
            model.addAttribute("dashboardHrSnapshot", dashboardAreaWidgetService.buildHrSnapshot());
        } else {
            model.addAttribute("dashboardHrSnapshot", null);
        }
    }

    private Set<String> authorityNames(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean hasAnyAuthority(Set<String> authorities, String... candidates) {
        for (String candidate : candidates) {
            if (authorities.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void addModuleAttributes(Model model) {
        Map<String, Boolean> moduleFlags = systemModuleService.getModuleFlags();

        boolean moduleDashboard = flag(moduleFlags, "dashboard");
        boolean moduleBusinessOverview = flag(moduleFlags, "business_overview");
        boolean moduleMonthlyFollowup = flag(moduleFlags, "monthly_followup");
        boolean moduleCommercialDaily = flag(moduleFlags, "commercial_daily");
        boolean moduleCrm = flag(moduleFlags, "crm");
        boolean moduleClients = flag(moduleFlags, "clients");
        boolean modulePromotions = flag(moduleFlags, "promotions");
        boolean moduleDelivery = flag(moduleFlags, "delivery");
        boolean moduleReorder = flag(moduleFlags, "reorder");
        boolean moduleIncome = flag(moduleFlags, "income");
        boolean moduleExpenses = flag(moduleFlags, "expenses");
        boolean moduleFixedCosts = flag(moduleFlags, "fixed_costs");
        boolean moduleSuppliers = flag(moduleFlags, "suppliers");
        boolean moduleInventory = flag(moduleFlags, "inventory");
        boolean moduleProducts = flag(moduleFlags, "products");
        boolean moduleCategories = flag(moduleFlags, "categories");
        boolean moduleWarehouse = flag(moduleFlags, "warehouse");
        boolean moduleSupplies = flag(moduleFlags, "supplies");
        boolean moduleContainers = flag(moduleFlags, "containers");
        boolean moduleProduction = flag(moduleFlags, "production");
        boolean moduleFinance = flag(moduleFlags, "finance");
        boolean moduleAccounting = flag(moduleFlags, "accounting");
        boolean moduleCashflow = flag(moduleFlags, "cashflow");
        boolean moduleBreakEven = flag(moduleFlags, "break_even");
        boolean modulePriceSimulator = flag(moduleFlags, "price_simulator");
        boolean moduleMarketing = flag(moduleFlags, "marketing");
        boolean moduleBlog = flag(moduleFlags, "blog");
        boolean moduleTestimonials = flag(moduleFlags, "testimonials");
        boolean modulePublicSite = flag(moduleFlags, "public_site");
        boolean modulePublicCatalog = flag(moduleFlags, "public_catalog");
        boolean moduleHr = flag(moduleFlags, "hr");
        boolean moduleUsers = flag(moduleFlags, "users");
        boolean moduleRolesPermissions = flag(moduleFlags, "roles_permissions");
        boolean modulePlatformSettings = flag(moduleFlags, "platform_settings");

        boolean moduleMarketingSection = moduleMarketing || moduleBlog || moduleTestimonials;
        boolean moduleCommercialSection = moduleCrm && (moduleCommercialDaily || moduleClients || modulePromotions || moduleDelivery || moduleReorder);
        boolean moduleInventorySection = moduleInventory && (moduleProducts || moduleCategories || moduleWarehouse || moduleSupplies || moduleContainers || moduleProduction);
        boolean moduleFinanceSection = moduleFinance && (moduleAccounting || moduleCashflow || moduleBreakEven || modulePriceSimulator);
        boolean moduleSystemSection = moduleHr || moduleUsers || moduleRolesPermissions || modulePlatformSettings;
        boolean moduleOperationSection = moduleCommercialSection || moduleIncome || moduleExpenses || moduleInventorySection;

        model.addAttribute("clientFeatures", clientFeatureProperties);
        model.addAttribute("systemModules", moduleFlags);

        model.addAttribute("moduleDashboardEnabled", moduleDashboard);
        model.addAttribute("moduleBusinessOverviewEnabled", moduleBusinessOverview);
        model.addAttribute("moduleMonthlyFollowupEnabled", moduleMonthlyFollowup);
        model.addAttribute("moduleCommercialDailyEnabled", moduleCommercialDaily);
        model.addAttribute("moduleCrmEnabled", moduleCrm);
        model.addAttribute("moduleClientsEnabled", moduleClients);
        model.addAttribute("modulePromotionsEnabled", modulePromotions);
        model.addAttribute("moduleDeliveryEnabled", moduleDelivery);
        model.addAttribute("moduleReorderEnabled", moduleReorder);
        model.addAttribute("moduleIncomeEnabled", moduleIncome);
        model.addAttribute("moduleExpensesEnabled", moduleExpenses);
        model.addAttribute("moduleFixedCostsEnabled", moduleFixedCosts);
        model.addAttribute("moduleSuppliersEnabled", moduleSuppliers);
        model.addAttribute("moduleInventoryEnabled", moduleInventory);
        model.addAttribute("moduleProductsEnabled", moduleProducts);
        model.addAttribute("moduleCategoriesEnabled", moduleCategories);
        model.addAttribute("moduleWarehouseEnabled", moduleWarehouse);
        model.addAttribute("moduleSuppliesEnabled", moduleSupplies);
        model.addAttribute("moduleContainersEnabled", moduleContainers);
        model.addAttribute("moduleProductionEnabled", moduleProduction);
        model.addAttribute("moduleFinanceEnabled", moduleFinance);
        model.addAttribute("moduleAccountingEnabled", moduleAccounting);
        model.addAttribute("moduleCashflowEnabled", moduleCashflow);
        model.addAttribute("moduleBreakEvenEnabled", moduleBreakEven);
        model.addAttribute("modulePriceSimulatorEnabled", modulePriceSimulator);
        model.addAttribute("moduleMarketingEnabled", moduleMarketing);
        model.addAttribute("moduleBlogEnabled", moduleBlog);
        model.addAttribute("moduleTestimonialsEnabled", moduleTestimonials);
        model.addAttribute("modulePublicSiteEnabled", modulePublicSite);
        model.addAttribute("modulePublicCatalogEnabled", modulePublicCatalog);
        model.addAttribute("moduleHrEnabled", moduleHr);
        model.addAttribute("moduleUsersEnabled", moduleUsers);
        model.addAttribute("moduleRolesPermissionsEnabled", moduleRolesPermissions);
        model.addAttribute("modulePlatformSettingsEnabled", modulePlatformSettings);
        model.addAttribute("moduleMarketingSectionEnabled", moduleMarketingSection);
        model.addAttribute("moduleCommercialSectionEnabled", moduleCommercialSection);
        model.addAttribute("moduleInventorySectionEnabled", moduleInventorySection);
        model.addAttribute("moduleFinanceSectionEnabled", moduleFinanceSection);
        model.addAttribute("moduleSystemSectionEnabled", moduleSystemSection);
        model.addAttribute("moduleOperationSectionEnabled", moduleOperationSection);

        // Backward-compatible feature attributes used by existing templates.
        model.addAttribute("featureContainers", moduleContainers);
        model.addAttribute("featureDelivery", moduleDelivery);
        model.addAttribute("featureProduction", moduleProduction);
        model.addAttribute("featureReorder", moduleReorder);
        model.addAttribute("featureMarketing", moduleMarketing);
        model.addAttribute("featureBlog", moduleBlog);
        model.addAttribute("featureTestimonials", moduleTestimonials);
        model.addAttribute("featurePublicCatalog", modulePublicCatalog);
        model.addAttribute("featureSupplies", moduleSupplies);
        model.addAttribute("featureFixedCosts", moduleFixedCosts);
        model.addAttribute("featureBreakEven", moduleBreakEven);
        model.addAttribute("featurePriceSimulator", modulePriceSimulator);
        model.addAttribute("featureMarketingSection", moduleMarketingSection);
    }

    private boolean flag(Map<String, Boolean> moduleFlags, String key) {
        return Boolean.TRUE.equals(moduleFlags.get(key));
    }
}
