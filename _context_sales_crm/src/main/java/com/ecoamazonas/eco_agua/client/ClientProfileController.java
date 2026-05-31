package com.ecoamazonas.eco_agua.client;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/client-profiles")
@PreAuthorize("hasRole('ADMIN')")
public class ClientProfileController {

    private final ClientProfileService service;

    public ClientProfileController(ClientProfileService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        List<ClientProfile> profiles = service.findAll();
        model.addAttribute("profiles", profiles);
        model.addAttribute("activePage", "client-profiles");
        return "admin/client_profiles"; // templates/admin/client_profiles.html
    }

    @PostMapping("/save")
    public String save(ClientProfile formProfile,
                       RedirectAttributes redirectAttributes) {

        if (formProfile.getSuggestedPrice() == null) {
            formProfile.setSuggestedPrice(BigDecimal.ZERO);
        }

        service.saveFromForm(formProfile);

        redirectAttributes.addFlashAttribute("message", "Perfil guardado correctamente.");
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/admin/client-profiles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {

        service.delete(id);

        redirectAttributes.addFlashAttribute("message", "Perfil eliminado correctamente.");
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/admin/client-profiles";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam(name = "ids", required = false) List<Long> ids,
                             RedirectAttributes redirectAttributes) {

        if (ids == null || ids.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "No se seleccionó ningún perfil.");
            redirectAttributes.addFlashAttribute("messageType", "info");
        } else {
            service.deleteMany(ids);
            redirectAttributes.addFlashAttribute(
                    "message",
                    ids.size() + " perfiles eliminados correctamente."
            );
            redirectAttributes.addFlashAttribute("messageType", "success");
        }

        return "redirect:/admin/client-profiles";
    }
}
