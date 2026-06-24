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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/control-center/instances/{instanceId}/appearance")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26AppearanceEditorController {

    private final Matrix26AppearanceEditorService editorService;

    public Matrix26AppearanceEditorController(Matrix26AppearanceEditorService editorService) {
        this.editorService = editorService;
    }

    @GetMapping("/edit")
    public String edit(@PathVariable Long instanceId, Model model) {
        addEditorModel(model, instanceId, editorService.currentForm(instanceId));
        return "control_center/appearance/editor";
    }

    @PostMapping("/preview")
    public String preview(
            @PathVariable Long instanceId,
            @Valid @ModelAttribute("appearanceForm") Matrix26AppearanceEditorForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (!bindingResult.hasErrors()) {
            try {
                editorService.validate(form);
            } catch (IllegalArgumentException ex) {
                bindingResult.reject("appearance", ex.getMessage());
            }
        }
        if (bindingResult.hasErrors()) {
            addEditorModel(model, instanceId, form);
            return "control_center/appearance/editor";
        }

        addPreviewModel(model, instanceId, form);
        return "control_center/appearance/preview";
    }

    @GetMapping("/preview")
    public String previewSaved(@PathVariable Long instanceId, Model model) {
        Matrix26AppearanceEditorForm form = editorService.currentForm(instanceId);
        addPreviewModel(model, instanceId, form);
        return "control_center/appearance/preview";
    }

    @PostMapping("/draft")
    public String saveDraft(
            @PathVariable Long instanceId,
            @Valid @ModelAttribute("appearanceForm") Matrix26AppearanceEditorForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (form.getReason() == null || form.getReason().isBlank()) {
            bindingResult.rejectValue("reason", "appearance.reason", "Indica el motivo del cambio.");
        }
        if (!bindingResult.hasErrors()) {
            try {
                editorService.saveDraft(instanceId, form, actor(authentication));
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Borrador guardado. La apariencia publicada de la instancia no fue modificada."
                );
                return "redirect:/control-center/instances/" + instanceId + "/appearance";
            } catch (IllegalArgumentException ex) {
                bindingResult.reject("appearance", ex.getMessage());
            }
        }

        addEditorModel(model, instanceId, form);
        return "control_center/appearance/editor";
    }

    @PostMapping("/draft/discard")
    public String discardDraft(
            @PathVariable Long instanceId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            editorService.discardDraft(instanceId, actor(authentication));
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Borrador descartado. La versión publicada permanece intacta."
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/control-center/instances/" + instanceId + "/appearance";
    }

    private void addEditorModel(Model model, Long instanceId, Matrix26AppearanceEditorForm form) {
        model.addAttribute("activePage", "matrix26_instance_appearance");
        model.addAttribute("editor", editorService.editorView(instanceId));
        if (!model.containsAttribute("appearanceForm")) {
            model.addAttribute("appearanceForm", form);
        }
    }

    private void addPreviewModel(Model model, Long instanceId, Matrix26AppearanceEditorForm form) {
        model.addAttribute("activePage", "matrix26_instance_appearance");
        model.addAttribute("editor", editorService.editorView(instanceId));
        model.addAttribute("appearanceForm", form);
        model.addAttribute(
                "publicPreviewVariables",
                editorService.previewVariables(form, form.getPublicThemeCode())
        );
        model.addAttribute(
                "adminPreviewVariables",
                editorService.previewVariables(form, form.getAdminThemeCode())
        );
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null
                ? "matrix26-system"
                : authentication.getName();
    }
}
