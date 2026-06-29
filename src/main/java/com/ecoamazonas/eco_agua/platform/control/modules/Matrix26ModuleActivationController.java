package com.ecoamazonas.eco_agua.platform.control.modules;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/control-center/modules/activation")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ModuleActivationController {

    private final Matrix26ModuleActivationService activationService;

    public Matrix26ModuleActivationController(Matrix26ModuleActivationService activationService) {
        this.activationService = activationService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_module_activation");
        model.addAttribute("overview", activationService.overview());
        return "control_center/modules/activation";
    }

    @PostMapping("/instances/{id}")
    public String updateInstance(
            @PathVariable Long id,
            @RequestParam(value = "selectedModules", required = false) List<String> selectedModules,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            activationService.updateInstanceModules(id, selectedModules, actor(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Module activation declarations were updated. Runtime navigation and operational databases were not changed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", safeMessage(ex));
        }
        return "redirect:/control-center/modules/activation#instance-" + id;
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null || authentication.getName().isBlank()
                ? "system"
                : authentication.getName();
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "Unexpected module activation error." : message;
    }
}
