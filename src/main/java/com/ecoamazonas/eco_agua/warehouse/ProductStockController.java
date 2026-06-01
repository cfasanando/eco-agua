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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
    public String viewMovements(
            @PathVariable Long id,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        Product product = productService.findById(id);

        DateRange range = normalizeDateRange(startDate, endDate);
        ProductKardexView kardexView = buildProductKardex(
                product,
                movementRepository.findByProductOrderByMovementDateAscIdAsc(product),
                range.startDate(),
                range.endDate()
        );

        model.addAttribute("product", product);
        model.addAttribute("rows", kardexView.rows());
        model.addAttribute("summary", kardexView.summary());
        model.addAttribute("startDate", range.startDate());
        model.addAttribute("endDate", range.endDate());

        return "warehouse/product_movements";
    }

    private ProductKardexView buildProductKardex(
            Product product,
            List<com.ecoamazonas.eco_agua.inventory.InventoryMovement> movements,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<ProductKardexRow> rows = new ArrayList<>();
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal openingBalance = BigDecimal.ZERO;
        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        int rowNumber = 1;

        for (var movement : movements) {
            LocalDateTime movementDate = movement.getMovementDate();
            BigDecimal quantityIn = movement.getQuantityIn() != null ? movement.getQuantityIn() : BigDecimal.ZERO;
            BigDecimal quantityOut = movement.getQuantityOut() != null ? movement.getQuantityOut() : BigDecimal.ZERO;

            boolean beforeStart = startDateTime != null && movementDate != null && movementDate.isBefore(startDateTime);
            boolean afterEnd = endDateTime != null && movementDate != null && movementDate.isAfter(endDateTime);

            if (beforeStart) {
                balance = balance.add(quantityIn).subtract(quantityOut);
                openingBalance = balance;
                continue;
            }

            if (afterEnd) {
                break;
            }

            balance = balance.add(quantityIn).subtract(quantityOut);
            totalIn = totalIn.add(quantityIn);
            totalOut = totalOut.add(quantityOut);
            rows.add(new ProductKardexRow(rowNumber++, movement, balance));
        }

        ProductKardexSummary summary = new ProductKardexSummary(
                openingBalance,
                totalIn,
                totalOut,
                balance,
                product != null ? product.getStock() : BigDecimal.ZERO,
                rows.size()
        );

        return new ProductKardexView(rows, summary);
    }

    private DateRange normalizeDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            return new DateRange(endDate, startDate);
        }
        return new DateRange(startDate, endDate);
    }

    private record ProductKardexView(List<ProductKardexRow> rows, ProductKardexSummary summary) {
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
