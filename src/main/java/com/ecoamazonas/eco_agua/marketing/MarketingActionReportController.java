package com.ecoamazonas.eco_agua.marketing;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/marketing/admin")
public class MarketingActionReportController {

    private final MarketingActionReportService actionReportService;

    public MarketingActionReportController(MarketingActionReportService actionReportService) {
        this.actionReportService = actionReportService;
    }

    @GetMapping("/actions-report")
    public String actionsReport(@RequestParam(value = "id", required = false) Long id, Model model) {
        MarketingActionReportItem actionForm = actionReportService.findForm(id);

        model.addAttribute("activePage", "marketing_actions_report");
        model.addAttribute("actionForm", actionForm);
        model.addAttribute("actionRows", actionReportService.findRows());
        model.addAttribute("actionTypes", MarketingActionReportItem.ActionType.values());
        model.addAttribute("actionChannels", MarketingActionReportItem.Channel.values());
        model.addAttribute("actionStatuses", MarketingActionReportItem.Status.values());
        model.addAttribute("campaigns", actionReportService.findCampaigns());
        model.addAttribute("publicationPlans", actionReportService.findPublicationPlans());
        model.addAttribute("ideas", actionReportService.findIdeas());
        model.addAttribute("products", actionReportService.findActiveProducts());
        model.addAttribute("selectedCampaignId", actionReportService.selectedCampaignId(actionForm));
        model.addAttribute("selectedPublicationPlanId", actionReportService.selectedPublicationPlanId(actionForm));
        model.addAttribute("selectedIdeaId", actionReportService.selectedIdeaId(actionForm));
        model.addAttribute("selectedProductId", actionReportService.selectedProductId(actionForm));
        model.addAttribute("isActionEdit", actionForm.getId() != null);
        return "marketing/admin_actions_report";
    }

    @PostMapping("/actions-report/save")
    public String saveAction(@ModelAttribute("actionForm") MarketingActionReportItem actionForm,
                             @RequestParam(value = "campaignId", required = false) Long campaignId,
                             @RequestParam(value = "publicationPlanId", required = false) Long publicationPlanId,
                             @RequestParam(value = "ideaId", required = false) Long ideaId,
                             @RequestParam(value = "productId", required = false) Long productId,
                             RedirectAttributes redirectAttributes) {
        actionReportService.save(actionForm, campaignId, publicationPlanId, ideaId, productId);
        redirectAttributes.addFlashAttribute("successMessage", "Acción de marketing guardada correctamente.");
        return "redirect:/marketing/admin/actions-report";
    }

    @PostMapping("/actions-report/{id}/follow-up")
    public String followUp(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        actionReportService.followUp(id);
        redirectAttributes.addFlashAttribute("successMessage", "Acción movida a seguimiento.");
        return "redirect:/marketing/admin/actions-report";
    }

    @PostMapping("/actions-report/{id}/with-result")
    public String markWithResult(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        actionReportService.markWithResult(id);
        redirectAttributes.addFlashAttribute("successMessage", "Acción marcada con resultado.");
        return "redirect:/marketing/admin/actions-report";
    }

    @PostMapping("/actions-report/{id}/without-result")
    public String markWithoutResult(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        actionReportService.markWithoutResult(id);
        redirectAttributes.addFlashAttribute("successMessage", "Acción marcada sin resultado.");
        return "redirect:/marketing/admin/actions-report";
    }

    @PostMapping("/actions-report/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        actionReportService.archive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Acción archivada correctamente.");
        return "redirect:/marketing/admin/actions-report";
    }
}
