package com.ecoamazonas.eco_agua.platform.control.appearance;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/control-center/instances/{instanceId}/appearance/branding")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BrandingController {

    private final Matrix26BrandingService brandingService;

    public Matrix26BrandingController(Matrix26BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @GetMapping
    public String branding(@PathVariable Long instanceId, Model model) {
        Matrix26BrandingView view = brandingService.view(instanceId);
        model.addAttribute("activePage", "matrix26_instance_appearance");
        model.addAttribute("brandingView", view);
        if (!model.containsAttribute("brandingForm")) {
            model.addAttribute("brandingForm", view.form());
        }
        return "control_center/appearance/branding";
    }

    @PostMapping("/text")
    public String saveText(
            @PathVariable Long instanceId,
            @Valid @ModelAttribute("brandingForm") Matrix26BrandingForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "matrix26_instance_appearance");
            model.addAttribute("brandingView", brandingService.view(instanceId));
            return "control_center/appearance/branding";
        }
        try {
            brandingService.saveText(instanceId, form, actor(authentication));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Branding guardado como borrador. La instancia todavía no fue modificada."
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirect(instanceId);
    }

    @PostMapping("/asset/{assetCode}")
    public String uploadAsset(
            @PathVariable Long instanceId,
            @PathVariable String assetCode,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            brandingService.storeAsset(instanceId, assetCode, file, actor(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Recurso visual guardado en el borrador.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirect(instanceId);
    }

    @PostMapping("/asset/{assetCode}/remove")
    public String removeAsset(
            @PathVariable Long instanceId,
            @PathVariable String assetCode,
            RedirectAttributes redirectAttributes
    ) {
        try {
            brandingService.removeAsset(instanceId, assetCode);
            redirectAttributes.addFlashAttribute("successMessage", "Recurso retirado del borrador.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirect(instanceId);
    }

    @PostMapping("/demo-kit")
    public String demoKit(
            @PathVariable Long instanceId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            brandingService.applyDemoKit(instanceId, actor(authentication));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Kit demo cargado: textos, logo, favicon, login, hero y recursos de prueba."
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirect(instanceId);
    }

    private String redirect(Long instanceId) {
        return "redirect:/control-center/instances/" + instanceId + "/appearance/branding";
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null
                ? "matrix26-system"
                : authentication.getName();
    }
}
