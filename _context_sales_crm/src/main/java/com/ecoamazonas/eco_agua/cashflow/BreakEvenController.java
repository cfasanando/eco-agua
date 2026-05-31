package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/cashflow")
public class BreakEvenController {

    private final BreakEvenService breakEvenService;
    private final ProductRepository productRepository;

    public BreakEvenController(
            BreakEvenService breakEvenService,
            ProductRepository productRepository
    ) {
        this.breakEvenService = breakEvenService;
        this.productRepository = productRepository;
    }

    @GetMapping("/break-even")
    public String showBreakEven(
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            Model model
    ) {
        // Product list for the filter
        List<Product> products = productRepository.findByActiveTrueOrderByNameAsc();
        model.addAttribute("products", products);

        if (products.isEmpty()) {
            // No products -> show empty state
            return "cashflow/break_even";
        }

        // Default product: first active
        if (productId == null) {
            productId = products.get(0).getId();
        }

        LocalDate today = LocalDate.now();
        if (year == null) {
            year = today.getYear();
        }
        if (month == null) {
            month = today.getMonthValue();
        }

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        BreakEvenResult result = breakEvenService.calculateForProductAndPeriod(productId, start, end);

        model.addAttribute("selectedProductId", productId);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("result", result);

        return "cashflow/break_even";
    }
}
