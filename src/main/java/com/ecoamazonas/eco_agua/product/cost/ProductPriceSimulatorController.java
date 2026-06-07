package com.ecoamazonas.eco_agua.product.cost;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/products/price-simulator")
public class ProductPriceSimulatorController {

    private final ProductPriceSimulatorService productPriceSimulatorService;

    public ProductPriceSimulatorController(ProductPriceSimulatorService productPriceSimulatorService) {
        this.productPriceSimulatorService = productPriceSimulatorService;
    }

    @GetMapping
    public String showPriceSimulator(
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "quantity", required = false) BigDecimal quantity,
            Model model
    ) {
        ProductPriceSimulatorSnapshot snapshot = productPriceSimulatorService.buildSnapshot(productId, price, quantity);
        model.addAttribute("snapshot", snapshot);
        return "admin/product_price_simulator";
    }
}
