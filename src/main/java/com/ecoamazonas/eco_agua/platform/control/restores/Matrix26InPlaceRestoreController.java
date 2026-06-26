package com.ecoamazonas.eco_agua.platform.control.restores;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/control-center/restores/in-place")
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26InPlaceRestoreController {

    private final Matrix26InPlaceRestoreService service;

    public Matrix26InPlaceRestoreController(Matrix26InPlaceRestoreService service) {
        this.service = service;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "matrix26_inplace_restores");
        model.addAttribute("summary", service.summary());
        model.addAttribute("jobs", service.recentJobs());
        model.addAttribute("candidates", service.candidates());
        model.addAttribute("restoreProperties", service.properties());
        return "control_center/restores/in_place_index";
    }

    @GetMapping("/new")
    public String newRestore(@RequestParam(value = "backupId", required = false) Long backupId, Model model) {
        model.addAttribute("activePage", "matrix26_inplace_restores");
        model.addAttribute("candidates", service.candidates());
        model.addAttribute("selectedBackupId", backupId);
        model.addAttribute("restoreProperties", service.properties());
        return "control_center/restores/in_place_new";
    }

    @PostMapping
    public String prepare(
            @RequestParam("backupId") long backupId,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26InPlaceRestoreJob job = service.prepare(backupId, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("restoreSuccess", "In-place staging completed: " + job.publicId());
            return "redirect:/control-center/restores/in-place/" + job.id();
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
            return "redirect:/control-center/restores/in-place/new?backupId=" + backupId;
        }
    }

    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("activePage", "matrix26_inplace_restores");
        model.addAttribute("job", service.job(id));
        model.addAttribute("steps", service.steps(id));
        model.addAttribute("checks", service.checks(id));
        model.addAttribute("restoreProperties", service.properties());
        return "control_center/restores/in_place_detail";
    }

    @PostMapping("/{id:\\d+}/switch")
    public String switchInstance(
            @PathVariable long id,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26InPlaceRestoreJob job = service.switchInstance(id, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("restoreSuccess", "Instance switched and awaiting confirmation: " + job.publicId());
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
        }
        return "redirect:/control-center/restores/in-place/" + id;
    }

    @PostMapping("/{id:\\d+}/confirm")
    public String confirm(
            @PathVariable long id,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26InPlaceRestoreJob job = service.confirm(id, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("restoreSuccess", "In-place restore confirmed: " + job.publicId());
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
        }
        return "redirect:/control-center/restores/in-place/" + id;
    }

    @PostMapping("/{id:\\d+}/rollback")
    public String rollback(
            @PathVariable long id,
            @RequestParam("confirmation") String confirmation,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matrix26InPlaceRestoreJob job = service.rollback(id, confirmation, actor(principal));
            redirectAttributes.addFlashAttribute("restoreSuccess", "Rollback completed: " + job.publicId());
        } catch (Matrix26RestoreException ex) {
            redirectAttributes.addFlashAttribute("restoreError", ex.getMessage());
        }
        return "redirect:/control-center/restores/in-place/" + id;
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null ? "matrix26-admin" : principal.getName();
    }
}
