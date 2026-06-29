package com.ecoamazonas.eco_agua.platform.control.modules;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.PlatformClientModule;
import com.ecoamazonas.eco_agua.platform.PlatformClientModuleRepository;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalog;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalogRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceManagementService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ModuleActivationService {

    private static final String PHASE_SOURCE = "MATRIX26_PHASE4A_ACTIVATION";

    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformModuleCatalogRepository moduleRepository;
    private final PlatformClientModuleRepository clientModuleRepository;
    private final Matrix26ModuleActivationEventRepository eventRepository;
    private final Matrix26InstanceManagementService instanceManagementService;

    public Matrix26ModuleActivationService(
            PlatformBusinessClientRepository clientRepository,
            PlatformModuleCatalogRepository moduleRepository,
            PlatformClientModuleRepository clientModuleRepository,
            Matrix26ModuleActivationEventRepository eventRepository,
            Matrix26InstanceManagementService instanceManagementService
    ) {
        this.clientRepository = clientRepository;
        this.moduleRepository = moduleRepository;
        this.clientModuleRepository = clientModuleRepository;
        this.eventRepository = eventRepository;
        this.instanceManagementService = instanceManagementService;
    }

    @Transactional(readOnly = true)
    public Matrix26ModuleActivationOverview overview() {
        List<PlatformModuleCatalog> modules = moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc();
        List<PlatformBusinessClient> instances = clientRepository.findAllByOrderByBusinessNameAsc();
        List<Matrix26ModuleActivationInstanceView> views = new ArrayList<>();
        long enabledAssignments = 0;
        long instancesWithoutModules = 0;
        long dependencyWarnings = 0;

        for (PlatformBusinessClient instance : instances) {
            Set<String> enabledKeys = enabledKeys(instance.getId());
            List<Matrix26ModuleActivationModuleView> moduleViews = moduleViews(modules, enabledKeys);
            Matrix26ModuleActivationInstanceView view = new Matrix26ModuleActivationInstanceView(
                    instance,
                    moduleViews,
                    enabledKeys,
                    profileFor(instance, enabledKeys),
                    safetyStatus(instance),
                    safetyTone(instance)
            );
            views.add(view);
            enabledAssignments += enabledKeys.size();
            if (enabledKeys.isEmpty()) {
                instancesWithoutModules++;
            }
            if (view.hasDependencyWarnings()) {
                dependencyWarnings++;
            }
        }

        Matrix26ModuleActivationSummary summary = new Matrix26ModuleActivationSummary(
                instances.size(),
                instances.stream().filter(PlatformBusinessClient::isProtectedInstance).count(),
                modules.size(),
                modules.stream().filter(PlatformModuleCatalog::isActive).count(),
                enabledAssignments,
                instancesWithoutModules,
                dependencyWarnings
        );

        return new Matrix26ModuleActivationOverview(
                LocalDateTime.now(),
                summary,
                groupedModules(modules),
                views,
                eventRepository.findTop100ByOrderByCreatedAtDescIdDesc(),
                notes(summary)
        );
    }

    @Transactional
    public void updateInstanceModules(Long instanceId, Collection<String> selectedModules, String actor) {
        PlatformBusinessClient instance = clientRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Matrix26 instance was not found."));
        List<PlatformModuleCatalog> activeModules = moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc();
        Map<String, PlatformModuleCatalog> activeByKey = activeModules.stream()
                .collect(Collectors.toMap(
                        module -> normalize(module.getModuleKey()),
                        module -> module,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Set<String> beforeKeys = enabledKeys(instanceId);
        Set<String> afterKeys = selectedModules == null
                ? new LinkedHashSet<>()
                : selectedModules.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .filter(activeByKey::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        instanceManagementService.updateModules(instanceId, new ArrayList<>(afterKeys), actor);
        Set<String> allChangedKeys = new LinkedHashSet<>();
        allChangedKeys.addAll(beforeKeys);
        allChangedKeys.addAll(afterKeys);

        if (allChangedKeys.isEmpty() || beforeKeys.equals(afterKeys)) {
            saveEvent(instance, "*", Matrix26ModuleActivationAction.NO_CHANGE, beforeKeys.equals(afterKeys), beforeKeys.equals(afterKeys), actor,
                    "Module activation review saved without changes.");
        } else {
            for (String moduleKey : allChangedKeys.stream().sorted().toList()) {
                boolean before = beforeKeys.contains(moduleKey);
                boolean after = afterKeys.contains(moduleKey);
                if (before == after) {
                    continue;
                }
                saveEvent(
                        instance,
                        moduleKey,
                        after ? Matrix26ModuleActivationAction.ACTIVATED : Matrix26ModuleActivationAction.DEACTIVATED,
                        before,
                        after,
                        actor,
                        "Module activation declaration changed. Operational installation is unchanged."
                );
            }
        }
    }

    private List<Matrix26ModuleActivationModuleView> moduleViews(List<PlatformModuleCatalog> modules, Set<String> enabledKeys) {
        return modules.stream()
                .map(module -> new Matrix26ModuleActivationModuleView(
                        module,
                        enabledKeys.contains(normalize(module.getModuleKey())),
                        dependencyLabel(module.getModuleKey()),
                        dependenciesSatisfied(module.getModuleKey(), enabledKeys),
                        runtimeProperty(module.getModuleKey()),
                        suggestedFor(module.getModuleKey())
                ))
                .toList();
    }

    private Set<String> enabledKeys(Long instanceId) {
        return clientModuleRepository.findClientModules(instanceId).stream()
                .filter(PlatformClientModule::isEnabled)
                .map(item -> normalize(item.getModule().getModuleKey()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, List<PlatformModuleCatalog>> groupedModules(List<PlatformModuleCatalog> modules) {
        Map<String, List<PlatformModuleCatalog>> result = new LinkedHashMap<>();
        modules.stream()
                .sorted(Comparator.comparing(PlatformModuleCatalog::getArea)
                        .thenComparing(PlatformModuleCatalog::getDisplayOrder)
                        .thenComparing(PlatformModuleCatalog::getName))
                .forEach(module -> result.computeIfAbsent(module.getArea(), ignored -> new ArrayList<>()).add(module));
        return result;
    }

    private void saveEvent(
            PlatformBusinessClient instance,
            String moduleKey,
            Matrix26ModuleActivationAction action,
            boolean before,
            boolean after,
            String actor,
            String notes
    ) {
        Matrix26ModuleActivationEvent event = new Matrix26ModuleActivationEvent();
        event.setInstance(instance);
        event.setModuleKey(moduleKey);
        event.setAction(action);
        event.setBeforeEnabled(before);
        event.setAfterEnabled(after);
        event.setActorUsername(actor == null || actor.isBlank() ? "system" : actor);
        event.setSource(PHASE_SOURCE);
        event.setNotes(notes);
        eventRepository.save(event);
    }

    private List<String> notes(Matrix26ModuleActivationSummary summary) {
        List<String> notes = new ArrayList<>();
        notes.add("Phase 4A is configuration-only: it writes module declarations and event evidence, but does not install code or change operational navigation.");
        if (summary.instancesWithoutModules() > 0) {
            notes.add(summary.instancesWithoutModules() + " instance(s) have no active module declaration yet.");
        }
        if (summary.dependencyWarnings() > 0) {
            notes.add(summary.dependencyWarnings() + " instance(s) have enabled modules with recommended dependencies missing.");
        }
        return notes;
    }

    private String profileFor(PlatformBusinessClient instance, Set<String> enabledKeys) {
        String businessType = normalize(instance.getBusinessType());
        if (enabledKeys.contains("matrix26_operations") || enabledKeys.contains("matrix26_security")) {
            return "Matrix26 Control";
        }
        if (enabledKeys.contains("restaurant") || enabledKeys.contains("restaurant_cash") || businessType.contains("restaurant")) {
            return "Restaurant";
        }
        if (enabledKeys.contains("production") || enabledKeys.contains("containers") || businessType.contains("water")) {
            return "Water delivery";
        }
        if (enabledKeys.contains("public_catalog") || enabledKeys.contains("blog") || businessType.contains("jungle")) {
            return "Catalog / commerce";
        }
        return "General business";
    }

    private String safetyStatus(PlatformBusinessClient instance) {
        if (instance.isProtectedInstance()) {
            return "Protected";
        }
        String status = instance.getStatus();
        if (status != null && status.equalsIgnoreCase("DECOMMISSIONED")) {
            return "Decommissioned";
        }
        return "Editable";
    }

    private String safetyTone(PlatformBusinessClient instance) {
        if (instance.isProtectedInstance()) {
            return "primary";
        }
        String status = instance.getStatus();
        if (status != null && status.equalsIgnoreCase("DECOMMISSIONED")) {
            return "warning";
        }
        return "secondary";
    }

    private boolean dependenciesSatisfied(String moduleKey, Set<String> enabledKeys) {
        Set<String> dependencies = dependenciesFor(moduleKey);
        return dependencies.isEmpty() || enabledKeys.containsAll(dependencies);
    }

    private String dependencyLabel(String moduleKey) {
        Set<String> dependencies = dependenciesFor(moduleKey);
        if (dependencies.isEmpty()) {
            return "No required dependency";
        }
        return "Recommended: " + String.join(", ", dependencies);
    }

    private Set<String> dependenciesFor(String moduleKey) {
        return switch (normalize(moduleKey)) {
            case "delivery" -> Set.of("sales");
            case "warehouse", "supplies", "containers" -> Set.of("inventory");
            case "production" -> Set.of("inventory");
            case "accounting", "cashflow", "fixed_costs", "break_even", "price_simulator" -> Set.of("finance");
            case "public_catalog", "blog", "testimonials" -> Set.of("marketing");
            case "restaurant", "restaurant_cash", "restaurant_qr", "restaurant_recipes", "restaurant_reservations" -> Set.of("sales", "inventory");
            case "matrix26_backups", "matrix26_restore", "matrix26_lifecycle", "matrix26_purge", "matrix26_security" -> Set.of("matrix26_operations");
            default -> Set.of();
        };
    }

    private String runtimeProperty(String moduleKey) {
        return switch (normalize(moduleKey)) {
            case "containers" -> "ecoagua.features.containers";
            case "delivery" -> "ecoagua.features.delivery";
            case "production" -> "ecoagua.features.production";
            case "reorder" -> "ecoagua.features.reorder";
            case "marketing" -> "ecoagua.features.marketing";
            case "blog" -> "ecoagua.features.blog";
            case "academy" -> "ecoagua.features.academy";
            case "restaurant", "restaurant_cash", "restaurant_qr", "restaurant_recipes", "restaurant_reservations" -> "ecoagua.features.restaurant";
            case "testimonials" -> "ecoagua.features.testimonials";
            case "public_catalog" -> "ecoagua.features.public-catalog";
            case "supplies" -> "ecoagua.features.supplies";
            case "fixed_costs" -> "ecoagua.features.fixed-costs";
            case "break_even" -> "ecoagua.features.break-even";
            case "price_simulator" -> "ecoagua.features.price-simulator";
            default -> "Matrix26 declaration only";
        };
    }

    private String suggestedFor(String moduleKey) {
        return switch (normalize(moduleKey)) {
            case "restaurant", "restaurant_cash", "restaurant_qr", "restaurant_recipes", "restaurant_reservations" -> "Restaurant";
            case "containers", "production", "warehouse", "reorder" -> "Water delivery";
            case "public_catalog", "blog", "testimonials" -> "Catalog / commerce";
            case "matrix26_operations", "matrix26_backups", "matrix26_restore", "matrix26_lifecycle", "matrix26_purge", "matrix26_security" -> "Matrix26 Control";
            default -> "General";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
