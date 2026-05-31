package com.ecoamazonas.eco_agua.marketing;

import com.ecoamazonas.eco_agua.promotion.Promotion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/marketing/admin")
public class MarketingSeasonalPromotionController {

    private final MarketingSeasonalPromotionService seasonalPromotionService;

    public MarketingSeasonalPromotionController(MarketingSeasonalPromotionService seasonalPromotionService) {
        this.seasonalPromotionService = seasonalPromotionService;
    }

    @GetMapping("/promotions")
    public String promotions(@RequestParam(value = "id", required = false) Long id, Model model) {
        Promotion promotionForm = seasonalPromotionService.findForm(id);

        model.addAttribute("activePage", "marketing_promotions");
        model.addAttribute("promotionForm", promotionForm);
        model.addAttribute("promotionRows", seasonalPromotionService.findRows());
        model.addAttribute("products", seasonalPromotionService.findActiveProducts());
        model.addAttribute("isPromotionEdit", promotionForm.getId() != null);
        return "marketing/admin_promotions";
    }

    @PostMapping("/promotions/save")
    public String savePromotion(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer promoNumber,
            @RequestParam(required = false) String colorBorder,
            @RequestParam(value = "enabled", required = false) Boolean enabledParam,
            @RequestParam(required = false) Integer maxCounter,
            RedirectAttributes redirectAttributes
    ) {
        seasonalPromotionService.save(
                id,
                name,
                description,
                startDate,
                endDate,
                promoNumber,
                colorBorder,
                enabledParam,
                maxCounter
        );
        redirectAttributes.addFlashAttribute("successMessage", "Promoción de temporada guardada correctamente.");
        return "redirect:/marketing/admin/promotions";
    }

    @PostMapping("/promotions/configure-products")
    public String configureProducts(
            @RequestParam("promotionId") Long promotionId,
            @RequestParam(value = "productId", required = false) List<Long> productIds,
            @RequestParam(value = "quantity", required = false) List<Integer> quantities,
            @RequestParam(value = "amount", required = false) List<BigDecimal> amounts,
            RedirectAttributes redirectAttributes
    ) {
        if (productIds == null) {
            productIds = new ArrayList<>();
        }
        seasonalPromotionService.configureProducts(promotionId, productIds, quantities, amounts);
        redirectAttributes.addFlashAttribute("successMessage", "Productos de la promoción actualizados correctamente.");
        return "redirect:/marketing/admin/promotions";
    }

    @PostMapping("/promotions/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        seasonalPromotionService.activate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Promoción activada correctamente.");
        return "redirect:/marketing/admin/promotions";
    }

    @PostMapping("/promotions/{id}/pause")
    public String pause(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        seasonalPromotionService.pause(id);
        redirectAttributes.addFlashAttribute("successMessage", "Promoción pausada correctamente.");
        return "redirect:/marketing/admin/promotions";
    }

    @PostMapping("/promotions/{id}/finish")
    public String finish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        seasonalPromotionService.finish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Promoción finalizada correctamente.");
        return "redirect:/marketing/admin/promotions";
    }
}
