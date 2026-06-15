package com.ecoamazonas.eco_agua.config;

import com.ecoamazonas.eco_agua.restaurant.RestaurantModuleInstaller;
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
    private final RestaurantModuleInstaller restaurantModuleInstaller;

    public SystemModuleAdminController(SystemModuleService systemModuleService,
                                       RestaurantModuleInstaller restaurantModuleInstaller) {
        this.systemModuleService = systemModuleService;
        this.restaurantModuleInstaller = restaurantModuleInstaller;
    }

    @GetMapping
    public String showModules(Model model) {
        systemModuleService.ensureDefaults();

        var modules = systemModuleService.getModuleDefinitions();
        long enabledCount = modules.stream().filter(SystemModuleService.ModuleDefinition::enabled).count();
        model.addAttribute("activePage", "system_modules");
        model.addAttribute("moduleGroups", systemModuleService.getModuleGroups());
        model.addAttribute("moduleTotalCount", modules.size());
        model.addAttribute("moduleEnabledCount", enabledCount);
        model.addAttribute("restaurantInstalled", restaurantModuleInstaller.isInstalled());
        model.addAttribute("restaurantEnabled", systemModuleService.isEnabled("restaurant"));

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
                if ("restaurant".equals(module.key())) {
                    if (enabled) {
                        restaurantModuleInstaller.installAndActivate(false);
                    } else {
                        restaurantModuleInstaller.disable();
                    }
                    continue;
                }

                systemModuleService.updateModuleFlag(module.key(), enabled);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Módulos del sistema actualizados correctamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error actualizando módulos: " + ex.getMessage());
        }

        return "redirect:/admin/system-modules";
    }

    @PostMapping("/restaurant/install")
    public String installRestaurant(@RequestParam(defaultValue = "true") boolean demoData,
                                    RedirectAttributes redirectAttributes) {
        try {
            restaurantModuleInstaller.installAndActivate(demoData);
            redirectAttributes.addFlashAttribute("successMessage", "Módulo Restaurante instalado y activado correctamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo instalar Restaurante: " + ex.getMessage());
        }

        return "redirect:/admin/system-modules";
    }

    @PostMapping("/restaurant/disable")
    public String disableRestaurant(RedirectAttributes redirectAttributes) {
        try {
            restaurantModuleInstaller.disable();
            redirectAttributes.addFlashAttribute("successMessage", "Módulo Restaurante desactivado para esta base de datos.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo desactivar Restaurante: " + ex.getMessage());
        }

        return "redirect:/admin/system-modules";
    }

    private String inputName(String moduleKey) {
        return "module_" + moduleKey;
    }
}
