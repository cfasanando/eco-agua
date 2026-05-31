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
public class MarketingFeaturedProductController {

    private final MarketingFeaturedProductService featuredProductService;

    public MarketingFeaturedProductController(MarketingFeaturedProductService featuredProductService) {
        this.featuredProductService = featuredProductService;
    }

    @GetMapping("/featured-products")
    public String featuredProducts(@RequestParam(value = "id", required = false) Long id, Model model) {
        MarketingFeaturedProduct featuredForm = featuredProductService.findForm(id);

        model.addAttribute("activePage", "marketing_featured_products");
        model.addAttribute("featuredForm", featuredForm);
        model.addAttribute("featuredRows", featuredProductService.findRows());
        model.addAttribute("products", featuredProductService.findActiveProducts());
        model.addAttribute("featuredStatuses", MarketingFeaturedProduct.Status.values());
        model.addAttribute("displayPlaces", MarketingFeaturedProduct.DisplayPlace.values());
        model.addAttribute("selectedProductId", featuredProductService.selectedProductId(featuredForm));
        model.addAttribute("isFeaturedEdit", featuredForm.getId() != null);
        return "marketing/admin_featured_products";
    }

    @PostMapping("/featured-products/save")
    public String saveFeaturedProduct(@ModelAttribute("featuredForm") MarketingFeaturedProduct featuredForm,
                                      @RequestParam(value = "productId", required = false) Long productId,
                                      RedirectAttributes redirectAttributes) {
        featuredProductService.save(featuredForm, productId);
        redirectAttributes.addFlashAttribute("successMessage", "Producto destacado guardado correctamente.");
        return "redirect:/marketing/admin/featured-products";
    }

    @PostMapping("/featured-products/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        featuredProductService.activate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Producto destacado activado correctamente.");
        return "redirect:/marketing/admin/featured-products";
    }

    @PostMapping("/featured-products/{id}/pause")
    public String pause(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        featuredProductService.pause(id);
        redirectAttributes.addFlashAttribute("successMessage", "Producto destacado pausado correctamente.");
        return "redirect:/marketing/admin/featured-products";
    }

    @PostMapping("/featured-products/{id}/finish")
    public String finish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        featuredProductService.finish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Producto destacado finalizado correctamente.");
        return "redirect:/marketing/admin/featured-products";
    }

    @PostMapping("/featured-products/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        featuredProductService.archive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Producto destacado archivado correctamente.");
        return "redirect:/marketing/admin/featured-products";
    }
}
