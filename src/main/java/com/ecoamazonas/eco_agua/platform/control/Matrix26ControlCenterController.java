package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.PlatformClientModule;
import com.ecoamazonas.eco_agua.platform.PlatformClientModuleRepository;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalog;
import com.ecoamazonas.eco_agua.platform.PlatformModuleCatalogRepository;
import com.ecoamazonas.eco_agua.platform.control.appearance.Matrix26ProvisioningAppearanceService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/control-center")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ControlCenterController {

    private final Matrix26InstanceHealthService healthService;
    private final Matrix26InstanceManagementService managementService;
    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformModuleCatalogRepository moduleRepository;
    private final PlatformClientModuleRepository clientModuleRepository;
    private final Matrix26ControlCenterProperties properties;
    private final Matrix26ProvisioningService provisioningService;
    private final Matrix26ProvisioningExecutionService provisioningExecutionService;
    private final Matrix26ProvisioningAppearanceService provisioningAppearanceService;

    public Matrix26ControlCenterController(
            Matrix26InstanceHealthService healthService,
            Matrix26InstanceManagementService managementService,
            PlatformBusinessClientRepository clientRepository,
            PlatformModuleCatalogRepository moduleRepository,
            PlatformClientModuleRepository clientModuleRepository,
            Matrix26ControlCenterProperties properties,
            Matrix26ProvisioningService provisioningService,
            Matrix26ProvisioningExecutionService provisioningExecutionService,
            Matrix26ProvisioningAppearanceService provisioningAppearanceService
    ) {
        this.healthService = healthService;
        this.managementService = managementService;
        this.clientRepository = clientRepository;
        this.moduleRepository = moduleRepository;
        this.clientModuleRepository = clientModuleRepository;
        this.properties = properties;
        this.provisioningService = provisioningService;
        this.provisioningExecutionService = provisioningExecutionService;
        this.provisioningAppearanceService = provisioningAppearanceService;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(
            @RequestParam(value = "refresh", required = false) String refresh,
            Model model
    ) {
        List<Matrix26InstanceStatus> statuses = healthService.currentStatuses("1".equals(refresh));
        Map<Long, List<PlatformClientModule>> modulesByInstance = modulesByInstance(statuses.stream()
                .map(Matrix26InstanceStatus::instance)
                .toList());
        long totalModules = moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc().size();

        model.addAttribute("activePage", "matrix26_dashboard");
        model.addAttribute("statuses", statuses);
        model.addAttribute("modulesByInstance", modulesByInstance);
        model.addAttribute("summary", healthService.buildSummary(statuses, totalModules));
        model.addAttribute("recentChecks", healthService.recentChecks());
        model.addAttribute("recentProvisioningJobs", provisioningService.recentJobs());
        model.addAttribute("provisioningSummary", provisioningService.summary());
        model.addAttribute("controlProperties", properties);
        return "control_center/dashboard";
    }

    @GetMapping("/instances")
    public String instances(
            @RequestParam(value = "refresh", required = false) String refresh,
            Model model
    ) {
        List<Matrix26InstanceStatus> statuses = healthService.currentStatuses("1".equals(refresh));
        model.addAttribute("activePage", "matrix26_instances");
        model.addAttribute("statuses", statuses);
        model.addAttribute("modulesByInstance", modulesByInstance(statuses.stream()
                .map(Matrix26InstanceStatus::instance)
                .toList()));
        model.addAttribute("summary", healthService.buildSummary(
                statuses,
                moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc().size()
        ));
        return "control_center/instances";
    }

    @GetMapping("/instances/new")
    public String newInstance(Model model) {
        addInstanceFormModel(model, managementService.newForm(), null, Set.of());
        return "control_center/instance_form";
    }

    @PostMapping("/instances")
    public String createInstance(
            @Valid @ModelAttribute("instanceForm") Matrix26InstanceForm form,
            BindingResult bindingResult,
            @RequestParam(value = "selectedModules", required = false) List<String> selectedModules,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Set<String> selected = selectedSet(selectedModules);
        if (bindingResult.hasErrors()) {
            addInstanceFormModel(model, form, null, selected);
            return "control_center/instance_form";
        }

        try {
            PlatformBusinessClient created = managementService.create(form, selectedModules, actor(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Instancia registrada correctamente. No se modificó ninguna base operativa.");
            return "redirect:/control-center/instances/" + created.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("instance", ex.getMessage());
            addInstanceFormModel(model, form, null, selected);
            return "control_center/instance_form";
        }
    }

    @GetMapping("/instances/{id}")
    public String instanceDetail(@PathVariable Long id, Model model) {
        PlatformBusinessClient instance = managementService.getInstance(id);
        model.addAttribute("activePage", "matrix26_instances");
        model.addAttribute("instance", instance);
        model.addAttribute("status", healthService.currentStatus(id, false));
        model.addAttribute("healthHistory", healthService.history(id));
        model.addAttribute("auditHistory", managementService.auditForInstance(id));
        model.addAttribute("groupedModules", managementService.groupedModules());
        model.addAttribute("selectedModuleKeys", managementService.assignedModuleKeys(id));
        return "control_center/instance_detail";
    }

    @GetMapping("/instances/{id}/edit")
    public String editInstance(@PathVariable Long id, Model model) {
        PlatformBusinessClient instance = managementService.getInstance(id);
        addInstanceFormModel(
                model,
                managementService.editForm(id),
                instance,
                managementService.assignedModuleKeys(id)
        );
        return "control_center/instance_form";
    }

    @PostMapping("/instances/{id}")
    public String updateInstance(
            @PathVariable Long id,
            @Valid @ModelAttribute("instanceForm") Matrix26InstanceForm form,
            BindingResult bindingResult,
            @RequestParam(value = "selectedModules", required = false) List<String> selectedModules,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        PlatformBusinessClient instance = managementService.getInstance(id);
        Set<String> selected = selectedSet(selectedModules);
        if (bindingResult.hasErrors()) {
            addInstanceFormModel(model, form, instance, selected);
            return "control_center/instance_form";
        }

        try {
            managementService.update(id, form, selectedModules, actor(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Metadatos de la instancia actualizados correctamente.");
            return "redirect:/control-center/instances/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("instance", ex.getMessage());
            addInstanceFormModel(model, form, instance, selected);
            return "control_center/instance_form";
        }
    }

    @PostMapping("/instances/{id}/check")
    public String checkInstance(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26InstanceStatus status = healthService.refreshInstance(id);
            managementService.recordManualHealthCheck(id, actor(authentication), status);
            redirectAttributes.addFlashAttribute(
                    status.online() ? "successMessage" : "warningMessage",
                    "Comprobación completada: " + status.statusLabel() + "."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", safeMessage(ex));
        }
        return "redirect:/control-center/instances/" + id;
    }

    @PostMapping("/instances/{id}/monitoring")
    public String toggleMonitoring(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        PlatformBusinessClient instance = managementService.toggleMonitoring(id, actor(authentication));
        redirectAttributes.addFlashAttribute(
                "successMessage",
                instance.isMonitorVisible() ? "Monitoreo automático activado." : "Monitoreo automático pausado."
        );
        return "redirect:/control-center/instances/" + id;
    }

    @PostMapping("/instances/{id}/protection")
    public String toggleProtection(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        PlatformBusinessClient instance = managementService.toggleProtection(id, actor(authentication));
        redirectAttributes.addFlashAttribute(
                "successMessage",
                instance.isProtectedInstance() ? "Instancia marcada como protegida." : "Protección administrativa desactivada."
        );
        return "redirect:/control-center/instances/" + id;
    }

    @PostMapping("/instances/{id}/modules")
    public String updateInstanceModules(
            @PathVariable Long id,
            @RequestParam(value = "selectedModules", required = false) List<String> selectedModules,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            managementService.updateModules(id, selectedModules, actor(authentication));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Declaraciones de módulos actualizadas. Los portales operativos no fueron modificados."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", safeMessage(ex));
        }
        if ("modules".equals(returnTo)) {
            return "redirect:/control-center/modules#instance-" + id;
        }
        return "redirect:/control-center/instances/" + id;
    }


    @GetMapping("/provisioning")
    public String provisioning(Model model) {
        model.addAttribute("activePage", "matrix26_provisioning");
        model.addAttribute("jobs", provisioningService.listJobs());
        model.addAttribute("provisioningSummary", provisioningService.summary());
        return "control_center/provisioning";
    }

    @GetMapping("/provisioning/new")
    public String newProvisioningPlan(Model model) {
        addProvisioningFormModel(model, provisioningService.newForm());
        return "control_center/provisioning_form";
    }

    @PostMapping("/provisioning/dry-run")
    public String createProvisioningDryRun(
            @Valid @ModelAttribute("provisioningForm") Matrix26ProvisioningPlanForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addProvisioningFormModel(model, form);
            return "control_center/provisioning_form";
        }

        try {
            Matrix26ProvisioningJob job = provisioningService.createDryRun(form, actor(authentication));
            redirectAttributes.addFlashAttribute(
                    "READY".equals(job.getStatus()) ? "successMessage" : "warningMessage",
                    "READY".equals(job.getStatus())
                            ? "Dry Run validado correctamente. No se ejecutó ninguna operación real."
                            : "Dry Run guardado con observaciones. Revisa los bloqueos antes de continuar."
            );
            return "redirect:/control-center/provisioning/" + job.getId();
        } catch (RuntimeException ex) {
            bindingResult.reject("provisioning", safeMessage(ex));
            addProvisioningFormModel(model, form);
            return "control_center/provisioning_form";
        }
    }

    @GetMapping("/provisioning/{id}")
    public String provisioningDetail(@PathVariable Long id, Model model) {
        addProvisioningDetailModel(model, id, new Matrix26ProvisioningExecutionForm());
        return "control_center/provisioning_detail";
    }

    @PostMapping("/provisioning/{id}/execute")
    public String executeProvisioningPlan(
            @PathVariable Long id,
            @Valid @ModelAttribute("executionForm") Matrix26ProvisioningExecutionForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            clearExecutionSecrets(form);
            addProvisioningDetailModel(model, id, form);
            return "control_center/provisioning_detail";
        }

        try {
            Matrix26ProvisioningJob job = provisioningExecutionService.execute(id, form, actor(authentication));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Aprovisionamiento completado. La base, el runtime y la apariencia inicial fueron generados; la instancia quedó protegida."
            );
            return "redirect:/control-center/provisioning/" + job.getId();
        } catch (RuntimeException ex) {
            bindingResult.reject("execution", safeMessage(ex));
            clearExecutionSecrets(form);
            addProvisioningDetailModel(model, id, form);
            return "control_center/provisioning_detail";
        }
    }

    @PostMapping("/provisioning/{id}/revalidate")
    public String revalidateProvisioningPlan(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26ProvisioningJob job = provisioningService.revalidate(id, actor(authentication));
            redirectAttributes.addFlashAttribute(
                    "READY".equals(job.getStatus()) ? "successMessage" : "warningMessage",
                    "READY".equals(job.getStatus())
                            ? "El plan continúa listo para ejecución confirmada."
                            : "El plan mantiene bloqueos pendientes."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", safeMessage(ex));
        }
        return "redirect:/control-center/provisioning/" + id;
    }

    @GetMapping("/modules")
    public String modules(Model model) {
        List<PlatformBusinessClient> instances = managementService.listInstances();
        List<PlatformModuleCatalog> modules = managementService.listModules();
        model.addAttribute("activePage", "matrix26_modules");
        model.addAttribute("instances", instances);
        model.addAttribute("modules", modules);
        model.addAttribute("groupedModules", managementService.groupedModules());
        model.addAttribute("moduleKeysByInstance", managementService.assignedModuleKeysByInstance(instances));
        return "control_center/modules";
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        model.addAttribute("activePage", "matrix26_audit");
        model.addAttribute("auditLogs", managementService.recentAudit());
        return "control_center/audit";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("activePage", "matrix26_settings");
        model.addAttribute("controlProperties", properties);
        model.addAttribute("instanceCount", clientRepository.count());
        model.addAttribute("moduleCount", moduleRepository.count());
        model.addAttribute("auditCount", managementService.auditCount());
        model.addAttribute("provisioningCount", provisioningService.summary().total());
        return "control_center/settings";
    }


    private void addProvisioningFormModel(Model model, Matrix26ProvisioningPlanForm form) {
        model.addAttribute("activePage", "matrix26_provisioning");
        model.addAttribute("provisioningForm", form);
        model.addAttribute("groupedProvisioningModules", provisioningService.groupedModuleOptions());
        model.addAttribute("appearancePresets", provisioningAppearanceService.presets());
        model.addAttribute("appearanceThemes", provisioningAppearanceService.activeThemes());
        model.addAttribute("publicAppearanceLayouts", provisioningAppearanceService.publicLayouts());
        model.addAttribute("adminAppearanceLayouts", provisioningAppearanceService.adminLayouts());
        model.addAttribute("loginAppearanceLayouts", provisioningAppearanceService.loginLayouts());
    }

    private void addProvisioningDetailModel(
            Model model,
            Long id,
            Matrix26ProvisioningExecutionForm executionForm
    ) {
        Matrix26ProvisioningPlanView plan = provisioningService.getPlan(id);
        model.addAttribute("activePage", "matrix26_provisioning");
        model.addAttribute("plan", plan);
        model.addAttribute("executionForm", executionForm);
        model.addAttribute("executionEnabled", provisioningExecutionService.isExecutionEnabled());
        model.addAttribute("canExecute", provisioningExecutionService.canExecute(plan.job()));
        model.addAttribute("appearanceSummary", provisioningAppearanceService.summary(plan.job()));
    }

    private void clearExecutionSecrets(Matrix26ProvisioningExecutionForm form) {
        form.setAdminPassword("");
        form.setAdminPasswordConfirmation("");
    }

    private void addInstanceFormModel(
            Model model,
            Matrix26InstanceForm form,
            PlatformBusinessClient instance,
            Set<String> selectedModuleKeys
    ) {
        model.addAttribute("activePage", "matrix26_instances");
        model.addAttribute("instanceForm", form);
        model.addAttribute("instance", instance);
        model.addAttribute("editing", instance != null);
        model.addAttribute("groupedModules", managementService.groupedModules());
        model.addAttribute("selectedModuleKeys", selectedModuleKeys);
    }

    private Set<String> selectedSet(List<String> selectedModules) {
        return selectedModules == null
                ? Set.of()
                : selectedModules.stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null
                ? "system"
                : authentication.getName();
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "No se pudo completar la operación.";
        }
        return message.length() > 450 ? message.substring(0, 450) : message;
    }

    private Map<Long, Set<String>> moduleKeysByInstance(Map<Long, List<PlatformClientModule>> assignments) {
        Map<Long, Set<String>> result = new LinkedHashMap<>();
        assignments.forEach((instanceId, values) -> result.put(
                instanceId,
                values.stream()
                        .filter(PlatformClientModule::isEnabled)
                        .map(item -> item.getModule().getModuleKey())
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        ));
        return result;
    }

    private Map<Long, List<PlatformClientModule>> modulesByInstance(List<PlatformBusinessClient> instances) {
        Map<Long, List<PlatformClientModule>> result = new LinkedHashMap<>();
        for (PlatformBusinessClient instance : instances) {
            result.put(instance.getId(), clientModuleRepository.findClientModules(instance.getId()));
        }
        return result;
    }
}
