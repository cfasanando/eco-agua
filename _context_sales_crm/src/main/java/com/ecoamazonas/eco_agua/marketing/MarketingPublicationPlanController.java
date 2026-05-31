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
public class MarketingPublicationPlanController {

    private final MarketingPublicationPlanService publicationPlanService;

    public MarketingPublicationPlanController(MarketingPublicationPlanService publicationPlanService) {
        this.publicationPlanService = publicationPlanService;
    }

    @GetMapping("/publication-plan")
    public String publicationPlan(@RequestParam(value = "id", required = false) Long id, Model model) {
        MarketingPublicationPlanItem publicationForm = publicationPlanService.findForm(id);

        model.addAttribute("activePage", "marketing_publication_plan");
        model.addAttribute("publicationForm", publicationForm);
        model.addAttribute("publicationRows", publicationPlanService.findRows());
        model.addAttribute("publicationChannels", MarketingPublicationPlanItem.Channel.values());
        model.addAttribute("publicationTypes", MarketingPublicationPlanItem.PublicationType.values());
        model.addAttribute("publicationStatuses", MarketingPublicationPlanItem.Status.values());
        model.addAttribute("ideas", publicationPlanService.findPlanningIdeas());
        model.addAttribute("campaigns", publicationPlanService.findCampaigns());
        model.addAttribute("strategies", publicationPlanService.findStrategies());
        model.addAttribute("products", publicationPlanService.findActiveProducts());
        model.addAttribute("isPublicationEdit", publicationForm.getId() != null);
        return "marketing/admin_publication_plan";
    }

    @PostMapping("/publication-plan/save")
    public String savePublication(@ModelAttribute("publicationForm") MarketingPublicationPlanItem publicationForm,
                                  @RequestParam(value = "ideaId", required = false) Long ideaId,
                                  @RequestParam(value = "campaignId", required = false) Long campaignId,
                                  @RequestParam(value = "strategyId", required = false) Long strategyId,
                                  @RequestParam(value = "productId", required = false) Long productId,
                                  RedirectAttributes redirectAttributes) {
        publicationPlanService.save(publicationForm, ideaId, campaignId, strategyId, productId);
        redirectAttributes.addFlashAttribute("successMessage", "Publicación guardada correctamente.");
        return "redirect:/marketing/admin/publication-plan";
    }

    @PostMapping("/publication-plan/{id}/prepare")
    public String preparePublication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        publicationPlanService.prepare(id);
        redirectAttributes.addFlashAttribute("successMessage", "Publicación movida a preparación.");
        return "redirect:/marketing/admin/publication-plan";
    }

    @PostMapping("/publication-plan/{id}/ready")
    public String markReadyPublication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        publicationPlanService.markReady(id);
        redirectAttributes.addFlashAttribute("successMessage", "Publicación marcada como lista.");
        return "redirect:/marketing/admin/publication-plan";
    }

    @PostMapping("/publication-plan/{id}/publish")
    public String publishPublication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        publicationPlanService.publish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Publicación marcada como publicada.");
        return "redirect:/marketing/admin/publication-plan";
    }

    @PostMapping("/publication-plan/{id}/cancel")
    public String cancelPublication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        publicationPlanService.cancel(id);
        redirectAttributes.addFlashAttribute("successMessage", "Publicación cancelada correctamente.");
        return "redirect:/marketing/admin/publication-plan";
    }
}
