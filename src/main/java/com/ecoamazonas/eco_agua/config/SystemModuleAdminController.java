package com.ecoamazonas.eco_agua.config;

import com.ecoamazonas.eco_agua.platform.module.PlatformModuleInstallationStatus;
import com.ecoamazonas.eco_agua.platform.module.PlatformModuleManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/system-modules")
public class SystemModuleAdminController {

    private final SystemModuleService systemModuleService;
    private final PlatformModuleManager platformModuleManager;

    public SystemModuleAdminController(SystemModuleService systemModuleService,
                                       PlatformModuleManager platformModuleManager) {
        this.systemModuleService = systemModuleService;
        this.platformModuleManager = platformModuleManager;
    }

    @GetMapping
    public String showModules(Model model) {
        systemModuleService.ensureDefaults();

        var modules = systemModuleService.getModuleDefinitions();
        long enabledCount = modules.stream().filter(SystemModuleService.ModuleDefinition::enabled).count();
        PlatformModuleInstallationStatus restaurantStatus = platformModuleManager.getStatus("restaurant");

        model.addAttribute("activePage", "system_modules");
        model.addAttribute("moduleGroups", systemModuleService.getModuleGroups());
        model.addAttribute("moduleTotalCount", modules.size());
        model.addAttribute("moduleEnabledCount", enabledCount);
        model.addAttribute("restaurantInstalled", restaurantStatus.schemaInstalled());
        model.addAttribute("restaurantEnabled", restaurantStatus.enabled());
        model.addAttribute("restaurantInstallationStatus", restaurantStatus);
        model.addAttribute("installationStatuses", platformModuleManager.listStatuses());
        model.addAttribute("moduleInstallationAllowed", platformModuleManager.isInstallationAllowed());
        model.addAttribute("moduleInstallationDatabase", platformModuleManager.currentDatabaseName());

        return "admin/system_modules";
    }

    @GetMapping("/installations")
    public String showInstallations(Model model) {
        model.addAttribute("activePage", "system_module_installations");
        model.addAttribute("installationStatuses", platformModuleManager.listStatuses());
        model.addAttribute("moduleInstallationAllowed", platformModuleManager.isInstallationAllowed());
        model.addAttribute("moduleInstallationDatabase", platformModuleManager.currentDatabaseName());
        return "admin/system_modules/installations";
    }

    @PostMapping
    public String updateModules(@RequestParam Map<String, String> requestParams,
                                RedirectAttributes redirectAttributes) {
        try {
            for (SystemModuleService.ModuleDefinition module : systemModuleService.getModuleDefinitions()) {
                if (module.locked()) {
                    continue;
                }

                boolean enabled = requestParams.containsKey(inputName(module.key()));
                if (platformModuleManager.supports(module.key())) {
                    PlatformModuleInstallationStatus currentStatus = platformModuleManager.getStatus(module.key());
                    if (enabled == currentStatus.enabled()) {
                        continue;
                    }
                    if (enabled) {
                        platformModuleManager.installAndActivate(module.key(), false);
                    } else {
                        platformModuleManager.disable(module.key());
                    }
                    continue;
                }

                systemModuleService.updateModuleFlag(module.key(), enabled);
            }

            redirectAttributes.addFlashAttribute("successMessage", "System modules were updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not update system modules: " + safeMessage(ex));
        }

        return "redirect:/admin/system-modules";
    }

    @PostMapping("/{moduleKey}/install")
    public String installModule(@PathVariable String moduleKey,
                                @RequestParam(defaultValue = "false") boolean demoData,
                                RedirectAttributes redirectAttributes) {
        try {
            platformModuleManager.installAndActivate(moduleKey, demoData);
            redirectAttributes.addFlashAttribute("successMessage", "Module installed and activated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", safeMessage(ex));
        }
        return "redirect:/admin/system-modules/installations";
    }

    @PostMapping("/{moduleKey}/synchronize")
    public String synchronizeModule(@PathVariable String moduleKey,
                                    RedirectAttributes redirectAttributes) {
        try {
            platformModuleManager.synchronizeInstalledModule(moduleKey);
            redirectAttributes.addFlashAttribute("successMessage", "Existing module installation was synchronized successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", safeMessage(ex));
        }
        return "redirect:/admin/system-modules/installations";
    }

    @PostMapping("/{moduleKey}/disable")
    public String disableModule(@PathVariable String moduleKey,
                                RedirectAttributes redirectAttributes) {
        try {
            platformModuleManager.disable(moduleKey);
            redirectAttributes.addFlashAttribute("successMessage", "Module disabled. Existing data was preserved.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", safeMessage(ex));
        }
        return "redirect:/admin/system-modules/installations";
    }

    private String inputName(String moduleKey) {
        return "module_" + moduleKey;
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "Unexpected module operation error." : message;
    }
}
