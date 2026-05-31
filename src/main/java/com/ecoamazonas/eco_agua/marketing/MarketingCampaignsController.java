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
public class MarketingCampaignsController {

    private final MarketingCampaignsService marketingCampaignsService;
    private final MarketingCampaignCalendarService campaignCalendarService;

    public MarketingCampaignsController(MarketingCampaignsService marketingCampaignsService,
                                        MarketingCampaignCalendarService campaignCalendarService) {
        this.marketingCampaignsService = marketingCampaignsService;
        this.campaignCalendarService = campaignCalendarService;
    }

    @GetMapping("/campaigns")
    public String dashboard(@RequestParam(value = "id", required = false) Long id, Model model) {
        MarketingCampaignCalendarItem campaignForm = campaignCalendarService.findForm(id);

        model.addAttribute("activePage", "marketing_campaigns");
        model.addAttribute("snapshot", marketingCampaignsService.buildSnapshot());
        model.addAttribute("campaignForm", campaignForm);
        model.addAttribute("campaigns", campaignCalendarService.findAll());
        model.addAttribute("campaignTypes", MarketingCampaignCalendarItem.CampaignType.values());
        model.addAttribute("campaignStatuses", MarketingCampaignCalendarItem.Status.values());
        model.addAttribute("products", campaignCalendarService.findActiveProducts());
        model.addAttribute("promotions", campaignCalendarService.findEnabledPromotions());
        model.addAttribute("strategies", campaignCalendarService.findStrategies());
        model.addAttribute("isCampaignEdit", campaignForm.getId() != null);
        return "marketing/admin_campaigns";
    }

    @PostMapping("/campaigns/save")
    public String saveCampaign(@ModelAttribute("campaignForm") MarketingCampaignCalendarItem campaignForm,
                               @RequestParam(value = "productId", required = false) Long productId,
                               @RequestParam(value = "promotionId", required = false) Long promotionId,
                               @RequestParam(value = "strategyId", required = false) Long strategyId,
                               RedirectAttributes redirectAttributes) {
        campaignCalendarService.save(campaignForm, productId, promotionId, strategyId);
        redirectAttributes.addFlashAttribute("successMessage", "Campaña guardada correctamente.");
        return "redirect:/marketing/admin/campaigns";
    }

    @PostMapping("/campaigns/{id}/activate")
    public String activateCampaign(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        campaignCalendarService.activate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Campaña activada correctamente.");
        return "redirect:/marketing/admin/campaigns";
    }

    @PostMapping("/campaigns/{id}/finish")
    public String finishCampaign(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        campaignCalendarService.finish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Campaña finalizada correctamente.");
        return "redirect:/marketing/admin/campaigns";
    }

    @PostMapping("/campaigns/{id}/archive")
    public String archiveCampaign(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        campaignCalendarService.archive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Campaña archivada correctamente.");
        return "redirect:/marketing/admin/campaigns";
    }
}
