package com.ecoamazonas.eco_agua.config;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/system-modules")
public class SystemModuleAdminController {

    private final SystemModuleService systemModuleService;

    public SystemModuleAdminController(SystemModuleService systemModuleService) {
        this.systemModuleService = systemModuleService;
    }

    @GetMapping
    public String showModules(Model model) {
        systemModuleService.ensureDefaults();

        var modules = systemModuleService.getModuleDefinitions();
        long enabledCount = modules.stream().filter(SystemModuleService.ModuleDefinition::enabled).count();
        long belenSuggestedCount = modules.stream().filter(SystemModuleService.ModuleDefinition::recommendedForBelen).count();
        long aguaEcoSuggestedCount = modules.stream().filter(SystemModuleService.ModuleDefinition::recommendedForAguaEco).count();

        model.addAttribute("activePage", "system_modules");
        model.addAttribute("moduleGroups", systemModuleService.getModuleGroups());
        model.addAttribute("moduleTotalCount", modules.size());
        model.addAttribute("moduleEnabledCount", enabledCount);
        model.addAttribute("moduleBelenSuggestedCount", belenSuggestedCount);
        model.addAttribute("moduleAguaEcoSuggestedCount", aguaEcoSuggestedCount);

        return "admin/system_modules";
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
                systemModuleService.updateModuleFlag(module.key(), enabled);
            }

            redirectAttributes.addFlashAttribute("successMessage", "System modules updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating system modules: " + ex.getMessage());
        }

        return "redirect:/admin/system-modules";
    }

    @PostMapping("/preset")
    public String applyPreset(@RequestParam("preset") String preset,
                              RedirectAttributes redirectAttributes) {
        try {
            systemModuleService.applyPreset(preset);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Module preset applied successfully: " + systemModuleService.presetLabel(preset) + "."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/system-modules";
    }

    private String inputName(String moduleKey) {
        return "module_" + moduleKey;
    }
}
