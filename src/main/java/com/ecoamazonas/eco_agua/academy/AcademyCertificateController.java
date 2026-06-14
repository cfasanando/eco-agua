package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AcademyCertificateController {

    private final AcademyCertificateService certificateService;

    public AcademyCertificateController(AcademyCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/admin/academy/certificates")
    public String adminCertificates(Model model) {
        model.addAttribute("activePage", "academy_certificates");
        model.addAttribute("certificates", certificateService.findAdminRows());
        model.addAttribute("candidates", certificateService.findCandidateRows());
        return "admin/academy/certificates";
    }

    @PostMapping("/admin/academy/certificates/enrollments/{enrollmentId}/issue")
    public String issueFromAdmin(@PathVariable Long enrollmentId, RedirectAttributes redirectAttributes) {
        try {
            AcademyCertificate certificate = certificateService.issueForEnrollment(enrollmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Certificado emitido: " + certificate.getCertificateCode());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("warningMessage", e.getMessage());
        }
        return "redirect:/admin/academy/certificates";
    }

    @PostMapping("/admin/academy/certificates/{certificateId}/revoke")
    public String revoke(@PathVariable Long certificateId,
                         @RequestParam(name = "notes", required = false) String notes,
                         RedirectAttributes redirectAttributes) {
        certificateService.revoke(certificateId, notes);
        redirectAttributes.addFlashAttribute("successMessage", "Certificado anulado correctamente.");
        return "redirect:/admin/academy/certificates";
    }

    @GetMapping("/academy/certificate/verify/{code}")
    public String verify(@PathVariable String code, Model model) {
        model.addAttribute("certificate", certificateService.findVerification(code));
        return "academy/certificate_verify";
    }
}
