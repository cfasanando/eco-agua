package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/job-positions")
public class JobPositionController {

    private final JobPositionService jobPositionService;

    public JobPositionController(JobPositionService jobPositionService) {
        this.jobPositionService = jobPositionService;
    }

    // Endpoint de prueba para validar que el controlador está mapeado
    @GetMapping("/ping")
    @ResponseBody
    public String ping() {
        return "job-positions OK";
    }

    @GetMapping
    public String list(
            @RequestParam(name = "id", required = false) Long id,
            Model model
    ) {
        JobPositionForm form = jobPositionService.buildForm(id);

        model.addAttribute("activePage", "job_positions");
        model.addAttribute("form", form);
        model.addAttribute("positions", jobPositionService.findAll());
        model.addAttribute("salaryPeriods", SalaryPeriod.values());
        model.addAttribute("paymentModes", PaymentMode.values());

        return "admin/job_positions";
    }

    @PostMapping("/save")
    public String save(
            @ModelAttribute("form") JobPositionForm form,
            RedirectAttributes redirectAttributes
    ) {
        try {
            jobPositionService.saveFromForm(form);
            redirectAttributes.addFlashAttribute("message", "Cargo guardado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al guardar cargo: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/job-positions";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            jobPositionService.deleteSoft(id);
            redirectAttributes.addFlashAttribute("message", "Cargo desactivado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al desactivar cargo: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("message", "error");
        }

        return "redirect:/admin/job-positions";
    }
}
