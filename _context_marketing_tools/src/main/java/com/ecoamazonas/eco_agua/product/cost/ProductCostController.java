package com.ecoamazonas.eco_agua.product.cost;

import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/products") // <--- antes era "/admin"
public class ProductCostController {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ProductRepository productRepository;
    private final ProductCostService productCostService;

    public ProductCostController(
            ProductRepository productRepository,
            ProductCostService productCostService
    ) {
        this.productRepository = productRepository;
        this.productCostService = productCostService;
    }

    @GetMapping("/{id}/full-cost") // <--- antes era "/products/{id}/cost"
    public String viewProductCost(
            @PathVariable("id") Long productId,
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // Default period = current month
        if (start == null || end == null) {
            LocalDate today = LocalDate.now();
            start = today.withDayOfMonth(1);
            end = today.withDayOfMonth(today.lengthOfMonth());
        }

        // Industrial CVU
        ProductCostDetail industrialDetail = productCostService.calculateCostDetail(productId);

        // CVU completo (industrial + gastos del periodo)
        FullProductCostDetail fullDetail =
                productCostService.calculateFullCostDetail(productId, start, end);

        BigDecimal price = product.getPrice() != null ? product.getPrice() : ZERO;

        // Margin vs industrial CVU
        BigDecimal industrialCvu = industrialDetail.getCvu();
        BigDecimal marginVsIndustrial = ZERO;
        if (industrialCvu.compareTo(ZERO) > 0 && price.compareTo(ZERO) > 0) {
            marginVsIndustrial = price
                    .subtract(industrialCvu)
                    .divide(industrialCvu, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Margin vs full CVU
        BigDecimal fullCvu = fullDetail.getFullUnitCost(); // usa el getter que ya tienes
        BigDecimal marginVsFull = ZERO;
        if (fullCvu.compareTo(ZERO) > 0 && price.compareTo(ZERO) > 0) {
            marginVsFull = price
                    .subtract(fullCvu)
                    .divide(fullCvu, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        model.addAttribute("product", product);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("industrialDetail", industrialDetail);
        model.addAttribute("fullDetail", fullDetail);
        model.addAttribute("marginVsIndustrial", marginVsIndustrial);
        model.addAttribute("marginVsFull", marginVsFull);

        return "admin/product_cost_detail";
    }
}
