package com.ecoamazonas.eco_agua.warehouse;

import com.ecoamazonas.eco_agua.inventory.InventoryMovementRepository;
import com.ecoamazonas.eco_agua.inventory.InventoryMovementType;
import com.ecoamazonas.eco_agua.inventory.InventoryService;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/warehouse/products-stock")
public class ProductStockController {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final InventoryMovementRepository movementRepository;

    public ProductStockController(ProductService productService,
                                  InventoryService inventoryService,
                                  InventoryMovementRepository movementRepository) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.movementRepository = movementRepository;
    }

    @GetMapping
    public String index(Model model) {
        var products = productService.findAll();
        model.addAttribute("products", products);
        model.addAttribute("summary", buildSummary(products));
        return "warehouse/products_stock";
    }

    @PostMapping("/minimum-stock")
    public String updateMinimumStock(
            @RequestParam("productId") Long productId,
            @RequestParam("minimumStock") BigDecimal minimumStock,
            RedirectAttributes redirectAttributes
    ) {
        try {
            productService.updateMinimumStock(productId, minimumStock);
            redirectAttributes.addFlashAttribute("message", "Stock mínimo actualizado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al actualizar el stock mínimo: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/warehouse/products-stock";
    }

    @PostMapping("/adjust")
    public String adjustStock(
            @RequestParam("productId") Long productId,
            @RequestParam("action") String action, // IN or OUT
            @RequestParam("quantity") BigDecimal quantity,
            @RequestParam("movementDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate movementDate,
            @RequestParam(value = "observation", required = false) String observation,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }

            boolean isIn = "IN".equalsIgnoreCase(action);

            inventoryService.registerProductMovement(
                    productId,
                    isIn ? quantity : BigDecimal.ZERO,
                    isIn ? BigDecimal.ZERO : quantity,
                    InventoryMovementType.ADJUSTMENT,
                    "MANUAL",
                    null,
                    observation,
                    movementDate
            );

            redirectAttributes.addFlashAttribute("message", "Stock actualizado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al actualizar el stock: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/warehouse/products-stock";
    }

    private ProductStockSummary buildSummary(Iterable<Product> products) {
        int activeProducts = 0;
        int outOfStockProducts = 0;
        int lowStockProducts = 0;
        int enoughStockProducts = 0;

        for (Product product : products) {
            if (product == null || !product.isActive()) {
                continue;
            }

            activeProducts++;

            if (product.isOutOfStock()) {
                outOfStockProducts++;
            } else if (product.isBelowMinimumStock()) {
                lowStockProducts++;
            } else {
                enoughStockProducts++;
            }
        }

        return new ProductStockSummary(activeProducts, outOfStockProducts, lowStockProducts, enoughStockProducts);
    }

    @GetMapping("/{id}/movements")
    public String viewMovements(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("movements", movementRepository.findByProductOrderByMovementDateDesc(product));

        return "warehouse/product_movements";
    }
}
