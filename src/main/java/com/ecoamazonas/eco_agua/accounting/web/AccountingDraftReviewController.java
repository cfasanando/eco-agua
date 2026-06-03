package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.service.AccountingDraftReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/accounting/draft-review")
public class AccountingDraftReviewController {

    private final AccountingDraftReviewService draftReviewService;

    public AccountingDraftReviewController(AccountingDraftReviewService draftReviewService) {
        this.draftReviewService = draftReviewService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("snapshot", draftReviewService.buildSnapshot());
        model.addAttribute("activePage", "accounting_draft_review");
        return "accounting/draft_review";
    }

    @PostMapping("/{id}/post")
    public String postDraft(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            draftReviewService.postDraft(id);
            redirectAttributes.addFlashAttribute("message", "Asiento registrado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo registrar el asiento.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/accounting/draft-review";
    }

    @PostMapping("/{id}/cancel")
    public String cancelDraft(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            draftReviewService.cancelDraft(id);
            redirectAttributes.addFlashAttribute("message", "Asiento anulado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo anular el asiento.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/accounting/draft-review";
    }
}
