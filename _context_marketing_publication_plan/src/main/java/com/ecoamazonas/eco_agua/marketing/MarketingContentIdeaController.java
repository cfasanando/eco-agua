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
public class MarketingContentIdeaController {

    private final MarketingContentIdeaService ideaService;

    public MarketingContentIdeaController(MarketingContentIdeaService ideaService) {
        this.ideaService = ideaService;
    }

    @GetMapping("/ideas")
    public String ideas(@RequestParam(value = "id", required = false) Long id, Model model) {
        MarketingContentIdea ideaForm = ideaService.findForm(id);

        model.addAttribute("activePage", "marketing_ideas");
        model.addAttribute("ideaForm", ideaForm);
        model.addAttribute("ideas", ideaService.findAll());
        model.addAttribute("ideaChannels", MarketingContentIdea.Channel.values());
        model.addAttribute("ideaTypes", MarketingContentIdea.ContentType.values());
        model.addAttribute("ideaStatuses", MarketingContentIdea.Status.values());
        model.addAttribute("ideaPriorities", MarketingContentIdea.Priority.values());
        model.addAttribute("products", ideaService.findActiveProducts());
        model.addAttribute("campaigns", ideaService.findCampaigns());
        model.addAttribute("strategies", ideaService.findStrategies());
        model.addAttribute("isIdeaEdit", ideaForm.getId() != null);
        return "marketing/admin_ideas";
    }

    @PostMapping("/ideas/save")
    public String saveIdea(@ModelAttribute("ideaForm") MarketingContentIdea ideaForm,
                           @RequestParam(value = "productId", required = false) Long productId,
                           @RequestParam(value = "campaignId", required = false) Long campaignId,
                           @RequestParam(value = "strategyId", required = false) Long strategyId,
                           RedirectAttributes redirectAttributes) {
        ideaService.save(ideaForm, productId, campaignId, strategyId);
        redirectAttributes.addFlashAttribute("successMessage", "Idea guardada correctamente.");
        return "redirect:/marketing/admin/ideas";
    }

    @PostMapping("/ideas/{id}/select")
    public String selectIdea(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ideaService.select(id);
        redirectAttributes.addFlashAttribute("successMessage", "Idea seleccionada correctamente.");
        return "redirect:/marketing/admin/ideas";
    }

    @PostMapping("/ideas/{id}/prepare")
    public String prepareIdea(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ideaService.prepare(id);
        redirectAttributes.addFlashAttribute("successMessage", "Idea movida a preparación.");
        return "redirect:/marketing/admin/ideas";
    }

    @PostMapping("/ideas/{id}/publish")
    public String publishIdea(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ideaService.publish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Idea marcada como publicada.");
        return "redirect:/marketing/admin/ideas";
    }

    @PostMapping("/ideas/{id}/discard")
    public String discardIdea(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ideaService.discard(id);
        redirectAttributes.addFlashAttribute("successMessage", "Idea descartada correctamente.");
        return "redirect:/marketing/admin/ideas";
    }
}
