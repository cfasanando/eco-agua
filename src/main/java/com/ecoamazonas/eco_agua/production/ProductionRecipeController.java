package com.ecoamazonas.eco_agua.production;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/production/recipes")
public class ProductionRecipeController {

    private final ProductionRecipeService productionRecipeService;

    public ProductionRecipeController(ProductionRecipeService productionRecipeService) {
        this.productionRecipeService = productionRecipeService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "production_recipes");
        model.addAttribute("snapshot", productionRecipeService.buildListSnapshot());
        return "production/recipes";
    }

    @GetMapping("/{productId}")
    public String detail(
            @PathVariable Long productId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            model.addAttribute("activePage", "production_recipes");
            model.addAttribute("snapshot", productionRecipeService.buildDetailSnapshot(productId));
            return "production/recipe_form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/production/recipes";
        }
    }

    @PostMapping("/{productId}")
    public String save(
            @PathVariable Long productId,
            @RequestParam(value = "supplyId", required = false) List<Long> supplyIds,
            @RequestParam(value = "quantityUsed", required = false) List<BigDecimal> quantitiesUsed,
            RedirectAttributes redirectAttributes
    ) {
        try {
            productionRecipeService.saveRecipe(productId, supplyIds, quantitiesUsed);
            redirectAttributes.addFlashAttribute("successMessage", "Receta de producción actualizada correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo guardar la receta de producción.");
        }

        return "redirect:/production/recipes/" + productId;
    }
}
