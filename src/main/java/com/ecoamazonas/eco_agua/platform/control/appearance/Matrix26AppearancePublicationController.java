package com.ecoamazonas.eco_agua.platform.control.appearance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26AppearancePublicationController {

    private final Matrix26AppearancePublicationService publicationService;

    public Matrix26AppearancePublicationController(
            Matrix26AppearancePublicationService publicationService
    ) {
        this.publicationService = publicationService;
    }

    @PostMapping("/control-center/instances/{instanceId}/appearance/publish")
    public String publish(
            @PathVariable Long instanceId,
            @RequestParam String confirmationCode,
            @RequestParam(defaultValue = "false") boolean acknowledged,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int version = publicationService.publishDraft(
                    instanceId,
                    confirmationCode,
                    acknowledged,
                    actor(authentication)
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Apariencia v" + version + " publicada. Recarga el portal administrado para ver los cambios."
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/control-center/instances/" + instanceId + "/appearance";
    }

    @PostMapping("/control-center/instances/{instanceId}/appearance/rollback/{historyId}")
    public String rollback(
            @PathVariable Long instanceId,
            @PathVariable Long historyId,
            @RequestParam String confirmationCode,
            @RequestParam(defaultValue = "false") boolean acknowledged,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int version = publicationService.rollback(
                    instanceId,
                    historyId,
                    confirmationCode,
                    acknowledged,
                    actor(authentication)
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Versión restaurada y publicada como v" + version + "."
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/control-center/instances/" + instanceId + "/appearance";
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null
                ? "matrix26-system"
                : authentication.getName();
    }
}
