package com.ecoamazonas.eco_agua.product.cost;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/products/profitability")
public class ProductProfitabilityController {

    private final ProductProfitabilityService productProfitabilityService;

    public ProductProfitabilityController(ProductProfitabilityService productProfitabilityService) {
        this.productProfitabilityService = productProfitabilityService;
    }

    @GetMapping
    public String showProductProfitability(
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model
    ) {
        ProductProfitabilitySnapshot snapshot = productProfitabilityService.buildSnapshot(start, end);
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("startDate", snapshot.getStartDate());
        model.addAttribute("endDate", snapshot.getEndDate());
        return "admin/product_profitability";
    }
}
