package com.ecoamazonas.eco_agua.delivery;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/delivery-zones")
public class AdminDeliveryZoneController {

    private final DeliveryZoneRepository deliveryZoneRepository;

    public AdminDeliveryZoneController(DeliveryZoneRepository deliveryZoneRepository) {
        this.deliveryZoneRepository = deliveryZoneRepository;
    }

    @GetMapping
    public String list(@RequestParam(value = "id", required = false) Long id, Model model) {
        List<DeliveryZone> zones = deliveryZoneRepository.findAllByOrderByNameAsc();
        model.addAttribute("zones", zones);

        DeliveryZoneForm form;
        if (model.containsAttribute("form")) {
            // Reuse form from redirect (validation errors)
            form = (DeliveryZoneForm) model.getAttribute("form");
        } else if (id != null) {
            DeliveryZone zone = deliveryZoneRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Zone not found with id " + id));
            form = DeliveryZoneForm.fromEntity(zone);
        } else {
            form = new DeliveryZoneForm();
            form.setRadiusMeters(2000);
        }

        model.addAttribute("form", form);
        return "admin/delivery_zones/list";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") DeliveryZoneForm form,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.form", bindingResult);
            redirectAttributes.addFlashAttribute("form", form);
            redirectAttributes.addFlashAttribute("message", "Please fix the validation errors.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/admin/delivery-zones";
        }

        DeliveryZone zone;
        if (form.getId() != null) {
            zone = deliveryZoneRepository.findById(form.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Zone not found with id " + form.getId()));
            form.updateEntity(zone);
        } else {
            zone = form.toNewEntity();
        }

        deliveryZoneRepository.save(zone);

        redirectAttributes.addFlashAttribute("message", "Delivery zone has been saved successfully.");
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/admin/delivery-zones";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        DeliveryZone zone = deliveryZoneRepository.findById(id)
                .orElse(null);

        if (zone == null) {
            redirectAttributes.addFlashAttribute("message", "Delivery zone was not found.");
            redirectAttributes.addFlashAttribute("messageType", "warning");
            return "redirect:/admin/delivery-zones";
        }

        deliveryZoneRepository.delete(zone);
        redirectAttributes.addFlashAttribute("message", "Delivery zone has been deleted.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/admin/delivery-zones";
    }
}
