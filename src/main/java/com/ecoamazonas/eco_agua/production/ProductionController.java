package com.ecoamazonas.eco_agua.production;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/production")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping
    public String index(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "status", required = false) ProductionStatus status,
            Model model
    ) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);
        List<ProductionOrder> rows = productionService.findByDateRange(effectiveStart, effectiveEnd, status);

        model.addAttribute("activePage", "production");
        model.addAttribute("rows", rows);
        model.addAttribute("startDate", effectiveStart);
        model.addAttribute("endDate", effectiveEnd);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ProductionStatus.values());
        model.addAttribute("confirmedCount", rows.stream().filter(r -> r.getStatus() == ProductionStatus.CONFIRMED).count());
        model.addAttribute("draftCount", rows.stream().filter(r -> r.getStatus() == ProductionStatus.DRAFT).count());

        return "production/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("activePage", "production");
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("products", productionService.findActiveProducts());

        return "production/form";
    }

    @GetMapping("/product/{productId}/recipe")
    @ResponseBody
    public List<ProductionRecipeLine> getRecipe(
            @PathVariable Long productId,
            @RequestParam(value = "quantityProduced", required = false) BigDecimal quantityProduced
    ) {
        return productionService.buildRecipeLines(productId, quantityProduced);
    }

    @PostMapping
    public String createDraft(
            @RequestParam("productionDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate productionDate,
            @RequestParam("productId") Long productId,
            @RequestParam("quantityProduced") BigDecimal quantityProduced,
            @RequestParam(value = "observation", required = false) String observation,
            @RequestParam(value = "supplyId", required = false) List<Long> supplyIds,
            @RequestParam(value = "quantityUsed", required = false) List<BigDecimal> quantitiesUsed,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ProductionOrder order = productionService.createDraft(
                    productionDate,
                    productId,
                    quantityProduced,
                    observation,
                    supplyIds,
                    quantitiesUsed
            );

            redirectAttributes.addFlashAttribute("successMessage", "Production draft created successfully.");
            return "redirect:/production/" + order.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/production/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("activePage", "production");
        model.addAttribute("order", productionService.findDetailedById(id));

        return "production/detail";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productionService.confirm(id);
            redirectAttributes.addFlashAttribute("successMessage", "Production confirmed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/production/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productionService.cancel(id);
            redirectAttributes.addFlashAttribute("successMessage", "Production canceled successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/production/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteDraft(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productionService.deleteDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", "Production draft deleted successfully.");
            return "redirect:/production";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/production/" + id;
        }
    }
}
