package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

@Controller
@RequestMapping("/admin/academy/leads")
public class AcademyLeadAdminController {

    private final AcademyLeadService leadService;

    public AcademyLeadAdminController(AcademyLeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public String leads(Model model) {
        model.addAttribute("activePage", "academy_leads");
        model.addAttribute("leads", leadService.findAdminRows());
        model.addAttribute("summary", leadService.summary());
        model.addAttribute("statuses", Arrays.stream(AcademyLead.Status.values()).map(StatusOption::from).toList());
        return "admin/academy/leads";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        AcademyLeadService.LeadDetailView detail = leadService.findDetail(id);
        model.addAttribute("activePage", "academy_leads");
        model.addAttribute("lead", detail.row());
        model.addAttribute("publicMessage", detail.publicMessage());
        model.addAttribute("internalNotes", detail.internalNotes());
        model.addAttribute("statuses", Arrays.stream(AcademyLead.Status.values()).map(StatusOption::from).toList());
        model.addAttribute("students", leadService.findActiveUsers());
        model.addAttribute("courses", leadService.findCoursesForEnrollment());
        model.addAttribute("whatsappUrl", leadService.buildWhatsappUrl(detail.row()));
        return "admin/academy/lead_detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam("status") AcademyLead.Status status,
                               @RequestParam(value = "notes", required = false) String notes,
                               RedirectAttributes redirectAttributes) {
        leadService.updateStatus(id, status, notes);
        redirectAttributes.addFlashAttribute("successMessage", "Estado del interesado actualizado.");
        return "redirect:/admin/academy/leads/" + id;
    }

    @PostMapping("/{id}/notes")
    public String saveNotes(@PathVariable Long id,
                            @RequestParam(value = "notes", required = false) String notes,
                            RedirectAttributes redirectAttributes) {
        leadService.saveNotes(id, notes);
        redirectAttributes.addFlashAttribute("successMessage", "Notas comerciales guardadas.");
        return "redirect:/admin/academy/leads/" + id;
    }

    @PostMapping("/{id}/convert")
    public String convert(@PathVariable Long id,
                          @RequestParam("studentId") Integer studentId,
                          @RequestParam("courseId") Long courseId,
                          @RequestParam(value = "notes", required = false) String notes,
                          RedirectAttributes redirectAttributes) {
        try {
            AcademyEnrollment enrollment = leadService.convertToEnrollment(id, studentId, courseId, notes);
            String username = enrollment.getStudent() != null ? enrollment.getStudent().getUsername() : "alumno";
            redirectAttributes.addFlashAttribute("successMessage", "Interesado convertido e inscrito como " + username + ".");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("warningMessage", e.getMessage());
        }
        return "redirect:/admin/academy/leads/" + id;
    }

    public record StatusOption(String value, String label) {
        static StatusOption from(AcademyLead.Status status) {
            return new StatusOption(status.name(), status.getLabel());
        }
    }
}
