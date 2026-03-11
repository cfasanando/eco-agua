package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.product.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class MonthlyFollowupController {

    private final MonthlyFollowupService monthlyFollowupService;

    public MonthlyFollowupController(MonthlyFollowupService monthlyFollowupService) {
        this.monthlyFollowupService = monthlyFollowupService;
    }

    @GetMapping("/dashboard/monthly-followup")
    public String monthlyFollowup(
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            Model model
    ) {
        List<Product> products = monthlyFollowupService.findActiveProducts();
        model.addAttribute("products", products);
        model.addAttribute("activePage", "monthly_followup");

        if (products.isEmpty()) {
            return "dashboard/monthly_followup";
        }

        LocalDate today = LocalDate.now();
        int selectedYear = year != null ? year : today.getYear();
        int selectedMonth = month != null ? month : today.getMonthValue();

        if (selectedMonth < 1) {
            selectedMonth = 1;
        }
        if (selectedMonth > 12) {
            selectedMonth = 12;
        }

        if (productId == null) {
            productId = products.get(0).getId();
        }

        MonthlyFollowupSnapshot snapshot = monthlyFollowupService.buildSnapshot(productId, selectedYear, selectedMonth);

        model.addAttribute("selectedProductId", productId);
        model.addAttribute("year", selectedYear);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("snapshot", snapshot);

        return "dashboard/monthly_followup";
    }
}
