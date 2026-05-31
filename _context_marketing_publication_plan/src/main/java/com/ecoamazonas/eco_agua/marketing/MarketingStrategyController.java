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
@RequestMapping("/marketing/admin/strategy")
public class MarketingStrategyController {

    private final MarketingStrategyService marketingStrategyService;

    public MarketingStrategyController(MarketingStrategyService marketingStrategyService) {
        this.marketingStrategyService = marketingStrategyService;
    }

    @GetMapping
    public String index(@RequestParam(value = "id", required = false) Long id, Model model) {
        MarketingStrategy strategyForm = marketingStrategyService.findForm(id);
        model.addAttribute("activePage", "marketing_strategy");
        model.addAttribute("strategyForm", strategyForm);
        model.addAttribute("strategies", marketingStrategyService.findAll());
        model.addAttribute("products", marketingStrategyService.findActiveProducts());
        model.addAttribute("promotions", marketingStrategyService.findEnabledPromotions());
        model.addAttribute("statuses", MarketingStrategy.Status.values());
        model.addAttribute("isEdit", strategyForm.getId() != null);
        return "marketing/admin_strategy";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("strategyForm") MarketingStrategy strategyForm,
                       @RequestParam(value = "productId", required = false) Long productId,
                       @RequestParam(value = "promotionId", required = false) Long promotionId,
                       RedirectAttributes redirectAttributes) {
        marketingStrategyService.save(strategyForm, productId, promotionId);
        redirectAttributes.addFlashAttribute("successMessage", "Estrategia de marketing guardada correctamente.");
        return "redirect:/marketing/admin/strategy";
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        marketingStrategyService.archive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Estrategia de marketing archivada correctamente.");
        return "redirect:/marketing/admin/strategy";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        marketingStrategyService.activate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Estrategia de marketing activada correctamente.");
        return "redirect:/marketing/admin/strategy";
    }
}
