package com.ecoamazonas.eco_agua.platform.control.modules.acceptance;

import com.ecoamazonas.eco_agua.config.SystemModuleRouteAccessService;
import com.ecoamazonas.eco_agua.config.SystemModuleRouteRule;
import com.ecoamazonas.eco_agua.config.SystemModuleService;
import com.ecoamazonas.eco_agua.platform.control.modules.Matrix26ModuleActivationEvent;
import com.ecoamazonas.eco_agua.platform.control.modules.Matrix26ModuleActivationInstanceView;
import com.ecoamazonas.eco_agua.platform.control.modules.Matrix26ModuleActivationOverview;
import com.ecoamazonas.eco_agua.platform.control.modules.Matrix26ModuleActivationService;
import com.ecoamazonas.eco_agua.platform.control.modules.Matrix26ModuleActivationSummary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26FeatureFlagAcceptanceService {

    private static final Set<String> RESTAURANT_KEYS = Set.of(
            "restaurant", "restaurant_cash", "restaurant_qr", "restaurant_recipes", "restaurant_reservations"
    );
    private static final Set<String> WATER_KEYS = Set.of(
            "sales", "delivery", "inventory", "warehouse", "production", "containers", "accounting", "finance"
    );
    private static final Set<String> CATALOG_KEYS = Set.of(
            "public_catalog", "marketing", "blog", "testimonials", "promotions"
    );
    private static final Set<String> MATRIX26_KEYS = Set.of(
            "matrix26_operations", "matrix26_backups", "matrix26_restore", "matrix26_lifecycle", "matrix26_purge", "matrix26_security"
    );

    private final Matrix26ModuleActivationService activationService;
    private final SystemModuleService systemModuleService;
    private final SystemModuleRouteAccessService routeAccessService;

    public Matrix26FeatureFlagAcceptanceService(
            Matrix26ModuleActivationService activationService,
            SystemModuleService systemModuleService,
            SystemModuleRouteAccessService routeAccessService
    ) {
        this.activationService = activationService;
        this.systemModuleService = systemModuleService;
        this.routeAccessService = routeAccessService;
    }

    public Matrix26FeatureFlagAcceptanceMatrix acceptance(boolean refresh) {
        List<String> notes = new ArrayList<>();
        Matrix26ModuleActivationOverview overview = safe("Module activation overview", activationService::overview, null, notes);
        Map<String, Boolean> runtimeFlags = safe("Runtime module flags", systemModuleService::getModuleFlags, Map.of(), notes);
        List<SystemModuleRouteAccessService.RouteDiagnostic> routeDiagnostics = safe(
                "Route access diagnostics", routeAccessService::diagnostics, List.of(), notes
        );
        List<SystemModuleRouteRule> routeRules = safe("Route access rules", routeAccessService::rules, List.of(), notes);

        List<Matrix26FeatureFlagAcceptanceGroup> groups = List.of(
                activationGroup(overview),
                navigationGroup(runtimeFlags, routeDiagnostics, routeRules),
                routeProtectionGroup(routeDiagnostics, routeRules),
                instanceCoverageGroup(overview),
                governanceGroup(overview, routeDiagnostics)
        );
        List<Matrix26FeatureFlagAcceptanceRisk> risks = risks(overview, routeDiagnostics, routeRules);
        Matrix26FeatureFlagAcceptanceStatus overallStatus = overall(groups, risks);

        return new Matrix26FeatureFlagAcceptanceMatrix(
                LocalDateTime.now(),
                overallStatus,
                metrics(overview, runtimeFlags, routeDiagnostics, routeRules, overallStatus),
                groups,
                risks,
                notes
        );
    }

    private Matrix26FeatureFlagAcceptanceGroup activationGroup(Matrix26ModuleActivationOverview overview) {
        Matrix26ModuleActivationSummary summary = overview == null ? null : overview.summary();
        long totalInstances = summary == null ? 0 : summary.totalInstances();
        long activeModules = summary == null ? 0 : summary.activeModules();
        long enabledAssignments = summary == null ? 0 : summary.enabledAssignments();
        long recentEvents = overview == null || overview.recentEvents() == null ? 0 : overview.recentEvents().size();

        List<Matrix26FeatureFlagAcceptanceItem> items = new ArrayList<>();
        items.add(item(
                "phase-4a-activation-center",
                "4A Module Activation Center is available",
                "Matrix26 can store active module declarations by instance without installing code or changing tenant databases.",
                overview != null && totalInstances > 0 && activeModules > 0 ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                totalInstances + " instance(s), " + activeModules + " active catalog module(s), " + enabledAssignments + " enabled declaration(s).",
                "/control-center/modules/activation",
                overview == null || totalInstances == 0 ? "Register Matrix26 instances and confirm module catalog initialization." : "Keep using this screen as the source of truth for customer module declarations."
        ));
        items.add(item(
                "activation-events",
                "Activation events are tracked",
                "Each activation/deactivation review creates evidence in matrix26_instance_module_activation_event.",
                recentEvents > 0 ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                recentEvents + " recent activation event(s) available in the overview.",
                "/control-center/modules/activation",
                recentEvents > 0 ? "No action required." : "Save at least one activation review per live instance before final customer handoff."
        ));
        items.add(item(
                "dependency-review",
                "Recommended dependencies are visible",
                "Activation screens show missing recommended dependencies without blocking safe metadata changes.",
                summary != null && summary.dependencyWarnings() == 0 ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                summary == null ? "Dependency summary unavailable." : summary.dependencyWarnings() + " instance(s) with dependency warning(s).",
                "/control-center/modules/activation",
                summary != null && summary.dependencyWarnings() == 0 ? "No dependency action required now." : "Review enabled modules with missing dependencies before production onboarding."
        ));
        return group("4A Activation Center", "Persistent feature flag declarations by Matrix26 instance.", "bi-toggles2", items);
    }

    private Matrix26FeatureFlagAcceptanceGroup navigationGroup(
            Map<String, Boolean> runtimeFlags,
            List<SystemModuleRouteAccessService.RouteDiagnostic> routeDiagnostics,
            List<SystemModuleRouteRule> routeRules
    ) {
        long enabledFlags = runtimeFlags.values().stream().filter(Boolean.TRUE::equals).count();
        boolean restaurantSubmodulesKnown = runtimeFlags.containsKey("restaurant_cash")
                && runtimeFlags.containsKey("restaurant_qr")
                && runtimeFlags.containsKey("restaurant_recipes")
                && runtimeFlags.containsKey("restaurant_reservations");
        boolean routeModulesCovered = routeRules.stream().allMatch(rule -> runtimeFlags.containsKey(rule.moduleKey()));
        long knownRuleModules = routeRules.stream().map(SystemModuleRouteRule::moduleKey).distinct().count();

        List<Matrix26FeatureFlagAcceptanceItem> items = new ArrayList<>();
        items.add(item(
                "phase-4b-runtime-flags",
                "4B runtime flags are available",
                "Central declarations can be projected to module.*.enabled and ecoagua.features.* runtime flags consumed by the client sidebar.",
                runtimeFlags.isEmpty() ? Matrix26FeatureFlagAcceptanceStatus.WARNING : Matrix26FeatureFlagAcceptanceStatus.PASSED,
                runtimeFlags.size() + " runtime flag(s), " + enabledFlags + " currently enabled.",
                "/admin/system-modules/visibility",
                runtimeFlags.isEmpty() ? "Open a client runtime and verify SystemModuleService initialization." : "Use the visibility page on each client runtime to validate effective flags."
        ));
        items.add(item(
                "restaurant-granular-sidebar",
                "Restaurant submodule visibility is granular",
                "Cash, QR, recipes and reservations have independent flags for sidebar visibility.",
                restaurantSubmodulesKnown ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                restaurantSubmodulesKnown ? "restaurant_cash, restaurant_qr, restaurant_recipes and restaurant_reservations flags are present." : "At least one restaurant submodule flag is missing from runtime visibility.",
                "/admin/system-modules/visibility",
                restaurantSubmodulesKnown ? "No action required." : "Review SystemModuleVisibilityMapper and sidebar conditions."
        ));
        items.add(item(
                "sidebar-route-module-coverage",
                "Sidebar modules are covered by route diagnostics",
                "Every protected route should reference a module key known by SystemModuleService.",
                routeModulesCovered ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                knownRuleModules + " unique route module key(s) covered by " + routeRules.size() + " rule(s).",
                "/admin/system-modules/visibility",
                routeModulesCovered ? "No action required." : "Add missing runtime module definitions before blocking those routes in production."
        ));
        return group("4B Runtime Navigation", "Sidebar/runtime visibility projected from Matrix26 activation declarations.", "bi-layout-sidebar", items);
    }

    private Matrix26FeatureFlagAcceptanceGroup routeProtectionGroup(
            List<SystemModuleRouteAccessService.RouteDiagnostic> routeDiagnostics,
            List<SystemModuleRouteRule> routeRules
    ) {
        long blocked = routeDiagnostics.stream().filter(diagnostic -> !diagnostic.moduleEnabled()).count();
        long allowed = routeDiagnostics.size() - blocked;
        boolean hasPublicRules = routeRules.stream().anyMatch(rule -> rule.pathPrefix().startsWith("/catalogo") || rule.pathPrefix().startsWith("/restaurant"));
        boolean hasAdminRules = routeRules.stream().anyMatch(rule -> rule.pathPrefix().startsWith("/admin/restaurant") || rule.pathPrefix().startsWith("/warehouse"));
        boolean hasFinanceRules = routeRules.stream().anyMatch(rule -> rule.pathPrefix().startsWith("/accounting") || rule.pathPrefix().startsWith("/cashflow"));

        List<Matrix26FeatureFlagAcceptanceItem> items = new ArrayList<>();
        items.add(item(
                "phase-4c-route-guard",
                "4C route guard is configured",
                "Direct URLs are evaluated against module flags and return HTTP 403 when the module is inactive.",
                routeRules.isEmpty() ? Matrix26FeatureFlagAcceptanceStatus.FAILED : Matrix26FeatureFlagAcceptanceStatus.PASSED,
                routeRules.size() + " route protection rule(s) configured.",
                "/admin/system-modules/visibility",
                routeRules.isEmpty() ? "Restore SystemModuleRouteAccessService rules before customer handoff." : "Keep route rules reviewed whenever a new module route is added."
        ));
        items.add(item(
                "blocked-routes-visible",
                "Blocked route evidence is visible",
                "The runtime visibility diagnostic shows which routes would be allowed or blocked for the current instance.",
                routeDiagnostics.isEmpty() ? Matrix26FeatureFlagAcceptanceStatus.WARNING : Matrix26FeatureFlagAcceptanceStatus.PASSED,
                allowed + " allowed rule(s), " + blocked + " blocked rule(s) in current runtime diagnostics.",
                "/admin/system-modules/visibility",
                routeDiagnostics.isEmpty() ? "Open a client runtime and verify route diagnostics are rendered." : "Use this evidence when testing disabled modules."
        ));
        items.add(item(
                "route-area-coverage",
                "Main route areas are protected",
                "Public catalog, restaurant, warehouse, finance, production, marketing and HR routes should have ownership rules.",
                hasPublicRules && hasAdminRules && hasFinanceRules ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                "Public rules: " + yesNo(hasPublicRules) + ", admin/restaurant/warehouse rules: " + yesNo(hasAdminRules) + ", finance rules: " + yesNo(hasFinanceRules) + ".",
                "/admin/system-modules/visibility",
                hasPublicRules && hasAdminRules && hasFinanceRules ? "No action required." : "Extend route rules for any uncovered area before enabling strict production mode."
        ));
        return group("4C Route Protection", "Direct URL protection by module flag with clear 403 feedback.", "bi-shield-lock", items);
    }

    private Matrix26FeatureFlagAcceptanceGroup instanceCoverageGroup(Matrix26ModuleActivationOverview overview) {
        Matrix26ModuleActivationSummary summary = overview == null ? null : overview.summary();
        List<Matrix26ModuleActivationInstanceView> instances = overview == null || overview.instances() == null ? List.of() : overview.instances();
        long restaurantInstances = countInstancesWithAny(instances, RESTAURANT_KEYS);
        long waterInstances = countInstancesWithAny(instances, WATER_KEYS);
        long catalogInstances = countInstancesWithAny(instances, CATALOG_KEYS);
        long matrix26Instances = countInstancesWithAny(instances, MATRIX26_KEYS);
        long protectedInstances = summary == null ? 0 : summary.protectedInstances();
        long withoutModules = summary == null ? 0 : summary.instancesWithoutModules();

        List<Matrix26FeatureFlagAcceptanceItem> items = new ArrayList<>();
        items.add(item(
                "customer-profiles-detected",
                "Customer module profiles are detectable",
                "Matrix26 should distinguish restaurant, water-delivery, catalog/commerce and control-center profiles from activation data.",
                restaurantInstances > 0 && (waterInstances > 0 || catalogInstances > 0) ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                restaurantInstances + " restaurant, " + waterInstances + " water-delivery, " + catalogInstances + " catalog/commerce, " + matrix26Instances + " Matrix26 control profile(s).",
                "/control-center/modules/activation",
                restaurantInstances > 0 ? "Review each profile before moving to a new module development." : "Activate restaurant module declarations for Restaurante Buen Sabor."
        ));
        items.add(item(
                "instances-with-modules",
                "Instances have module declarations",
                "Each active/protected instance should have at least one module declaration before customer handoff.",
                withoutModules == 0 ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                withoutModules + " instance(s) currently have no enabled module declaration.",
                "/control-center/modules/activation",
                withoutModules == 0 ? "No action required." : "Review empty instances and decide whether they are archived, lab-only or pending configuration."
        ));
        items.add(item(
                "protected-instances-reviewed",
                "Protected instances remain part of feature-flag review",
                "Core business/customer runtimes should be visible and protected in Matrix26 before module flags are enforced.",
                protectedInstances >= 3 ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                protectedInstances + " protected instance(s) reported by the activation summary.",
                "/control-center/instances",
                protectedInstances >= 3 ? "Keep protected flags for production-like instances." : "Mark core instances as protected before destructive lifecycle/purge workflows."
        ));
        return group("Instance Coverage", "Feature flag coverage by customer profile and protected instance.", "bi-hdd-stack", items);
    }

    private Matrix26FeatureFlagAcceptanceGroup governanceGroup(
            Matrix26ModuleActivationOverview overview,
            List<SystemModuleRouteAccessService.RouteDiagnostic> routeDiagnostics
    ) {
        long recentEvents = overview == null || overview.recentEvents() == null ? 0 : overview.recentEvents().size();
        long blocked = routeDiagnostics.stream().filter(diagnostic -> !diagnostic.moduleEnabled()).count();
        List<Matrix26FeatureFlagAcceptanceItem> items = new ArrayList<>();
        items.add(item(
                "read-only-acceptance",
                "Acceptance review is read-only",
                "This page only evaluates metadata, flags and route diagnostics. It does not modify module declarations or runtime settings.",
                Matrix26FeatureFlagAcceptanceStatus.PASSED,
                "Only GET /control-center/modules/acceptance is added in Phase 4D.",
                "/control-center/modules/acceptance",
                "Keep acceptance pages free of POST actions."
        ));
        items.add(item(
                "operational-impact-contained",
                "Operational impact is contained",
                "Feature flags affect sidebar visibility and route access, not database schema installation or runtime process control.",
                Matrix26FeatureFlagAcceptanceStatus.PASSED,
                "4A stores metadata, 4B projects flags, 4C blocks inactive direct URLs with HTTP 403.",
                "/control-center/modules/activation",
                "Use Platform Module Installation screens separately for schema/module installation work."
        ));
        items.add(item(
                "handoff-evidence",
                "Handoff evidence is available",
                "Recent activation events and blocked-route diagnostics provide proof for final acceptance.",
                recentEvents > 0 || blocked > 0 ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                recentEvents + " event(s), " + blocked + " currently blocked route rule(s).",
                "/admin/system-modules/visibility",
                recentEvents > 0 || blocked > 0 ? "Attach screenshots from activation and visibility pages to the handoff notes." : "Perform at least one activation save and one disabled-route test before handoff."
        ));
        return group("Governance and Handoff", "Final acceptance evidence and operating guardrails.", "bi-clipboard2-check", items);
    }

    private List<Matrix26FeatureFlagAcceptanceMetric> metrics(
            Matrix26ModuleActivationOverview overview,
            Map<String, Boolean> runtimeFlags,
            List<SystemModuleRouteAccessService.RouteDiagnostic> routeDiagnostics,
            List<SystemModuleRouteRule> routeRules,
            Matrix26FeatureFlagAcceptanceStatus overallStatus
    ) {
        Matrix26ModuleActivationSummary summary = overview == null ? null : overview.summary();
        long enabledFlags = runtimeFlags.values().stream().filter(Boolean.TRUE::equals).count();
        long blockedRules = routeDiagnostics.stream().filter(diagnostic -> !diagnostic.moduleEnabled()).count();
        long recentEvents = overview == null || overview.recentEvents() == null ? 0 : overview.recentEvents().size();
        return List.of(
                metric("Overall", overallStatus.getLabel(), "Feature flags acceptance status", overallStatus.getIcon(), tone(overallStatus)),
                metric("Instances", Long.toString(summary == null ? 0 : summary.totalInstances()), (summary == null ? 0 : summary.protectedInstances()) + " protected", "bi-hdd-stack", "primary"),
                metric("Enabled declarations", Long.toString(summary == null ? 0 : summary.enabledAssignments()), "Stored in Matrix26 activation metadata", "bi-toggles2", "success"),
                metric("Runtime flags", runtimeFlags.size() + " / " + enabledFlags, "Defined / enabled in current runtime", "bi-layout-sidebar", "info"),
                metric("Route rules", Long.toString(routeRules.size()), blockedRules + " currently blocked", "bi-shield-lock", blockedRules > 0 ? "warning" : "success"),
                metric("Recent events", Long.toString(recentEvents), "Activation event evidence", "bi-clock-history", recentEvents > 0 ? "success" : "secondary")
        );
    }

    private List<Matrix26FeatureFlagAcceptanceRisk> risks(
            Matrix26ModuleActivationOverview overview,
            List<SystemModuleRouteAccessService.RouteDiagnostic> routeDiagnostics,
            List<SystemModuleRouteRule> routeRules
    ) {
        Matrix26ModuleActivationSummary summary = overview == null ? null : overview.summary();
        long withoutModules = summary == null ? 0 : summary.instancesWithoutModules();
        long dependencyWarnings = summary == null ? 0 : summary.dependencyWarnings();
        boolean hasRoutes = !routeRules.isEmpty();
        boolean allDiagnosticsAvailable = !routeDiagnostics.isEmpty();
        List<Matrix26FeatureFlagAcceptanceRisk> risks = new ArrayList<>();
        risks.add(risk(
                "Empty module declarations",
                withoutModules + " instance(s) have no active module declaration.",
                withoutModules == 0 ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                withoutModules == 0 ? "No action required." : "Classify empty instances as archived/lab or configure their modules before customer delivery."
        ));
        risks.add(risk(
                "Recommended dependency warnings",
                dependencyWarnings + " instance(s) have enabled modules with recommended dependencies missing.",
                dependencyWarnings == 0 ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                dependencyWarnings == 0 ? "No action required." : "Fix dependency combinations in Module Activation Center before strict enforcement."
        ));
        risks.add(risk(
                "Route protection rule drift",
                hasRoutes ? routeRules.size() + " route rules are currently registered." : "No route protection rules are registered.",
                hasRoutes ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.FAILED,
                hasRoutes ? "Add new rules whenever a new module controller is created." : "Restore SystemModuleRouteAccessService route rules."
        ));
        risks.add(risk(
                "Runtime evidence required per tenant",
                allDiagnosticsAvailable ? "Client runtime diagnostics can show allowed/blocked route rules." : "Route diagnostics are not available in this context.",
                allDiagnosticsAvailable ? Matrix26FeatureFlagAcceptanceStatus.PASSED : Matrix26FeatureFlagAcceptanceStatus.WARNING,
                "Capture screenshots from /admin/system-modules/visibility for Restaurante, Agua Eco and Belén before final sign-off."
        ));
        return risks;
    }

    private Matrix26FeatureFlagAcceptanceStatus overall(
            List<Matrix26FeatureFlagAcceptanceGroup> groups,
            List<Matrix26FeatureFlagAcceptanceRisk> risks
    ) {
        Matrix26FeatureFlagAcceptanceStatus groupStatus = groups.stream()
                .map(Matrix26FeatureFlagAcceptanceGroup::status)
                .max((left, right) -> Integer.compare(left.getSeverityRank(), right.getSeverityRank()))
                .orElse(Matrix26FeatureFlagAcceptanceStatus.NOT_TESTED);
        Matrix26FeatureFlagAcceptanceStatus riskStatus = risks.stream()
                .map(Matrix26FeatureFlagAcceptanceRisk::status)
                .max((left, right) -> Integer.compare(left.getSeverityRank(), right.getSeverityRank()))
                .orElse(Matrix26FeatureFlagAcceptanceStatus.NOT_TESTED);
        return groupStatus.getSeverityRank() >= riskStatus.getSeverityRank() ? groupStatus : riskStatus;
    }

    private long countInstancesWithAny(List<Matrix26ModuleActivationInstanceView> instances, Collection<String> expectedKeys) {
        return instances.stream()
                .filter(instance -> instance.enabledKeys().stream().anyMatch(expectedKeys::contains))
                .count();
    }

    private Matrix26FeatureFlagAcceptanceGroup group(String title,
                                                     String description,
                                                     String icon,
                                                     List<Matrix26FeatureFlagAcceptanceItem> items) {
        return new Matrix26FeatureFlagAcceptanceGroup(title, description, icon, items);
    }

    private Matrix26FeatureFlagAcceptanceItem item(String code,
                                                   String title,
                                                   String description,
                                                   Matrix26FeatureFlagAcceptanceStatus status,
                                                   String evidence,
                                                   String route,
                                                   String recommendedAction) {
        return new Matrix26FeatureFlagAcceptanceItem(code, title, description, status, evidence, route, recommendedAction);
    }

    private Matrix26FeatureFlagAcceptanceMetric metric(String label,
                                                       String value,
                                                       String detail,
                                                       String icon,
                                                       String tone) {
        return new Matrix26FeatureFlagAcceptanceMetric(label, value, detail, icon, tone);
    }

    private Matrix26FeatureFlagAcceptanceRisk risk(String title,
                                                   String detail,
                                                   Matrix26FeatureFlagAcceptanceStatus status,
                                                   String recommendedAction) {
        return new Matrix26FeatureFlagAcceptanceRisk(title, detail, status, recommendedAction);
    }

    private String tone(Matrix26FeatureFlagAcceptanceStatus status) {
        return switch (status) {
            case PASSED -> "success";
            case WARNING -> "warning";
            case FAILED -> "danger";
            case NOT_TESTED -> "secondary";
        };
    }

    private String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private <T> T safe(String source, Supplier<T> supplier, T fallback, List<String> notes) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            notes.add(source + " could not be loaded: " + safeMessage(ex));
            return fallback;
        }
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}
