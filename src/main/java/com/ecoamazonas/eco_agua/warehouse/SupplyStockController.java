package com.ecoamazonas.eco_agua.warehouse;

import com.ecoamazonas.eco_agua.inventory.InventoryMovementRepository;
import com.ecoamazonas.eco_agua.inventory.InventoryMovementType;
import com.ecoamazonas.eco_agua.inventory.InventoryService;
import com.ecoamazonas.eco_agua.supply.Supply;
import com.ecoamazonas.eco_agua.supply.SupplyService;
import com.ecoamazonas.eco_agua.supply.SupplyRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/warehouse/supplies-stock")
public class SupplyStockController {

    private final SupplyService supplyService;
    private final InventoryService inventoryService;
    private final InventoryMovementRepository movementRepository;
    private final SupplyRepository supplyRepository;

    public SupplyStockController(SupplyService supplyService,
                                 InventoryService inventoryService,
                                 InventoryMovementRepository movementRepository,
                                 SupplyRepository supplyRepository) {
        this.supplyService = supplyService;
        this.inventoryService = inventoryService;
        this.movementRepository = movementRepository;
        this.supplyRepository = supplyRepository;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("supplies", supplyService.findAll());
        return "warehouse/supplies_stock";
    }

    @PostMapping("/adjust")
    public String adjustStock(
            @RequestParam("supplyId") Long supplyId,
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

            inventoryService.registerSupplyMovement(
                    supplyId,
                    isIn ? quantity : BigDecimal.ZERO,
                    isIn ? BigDecimal.ZERO : quantity,
                    InventoryMovementType.ADJUSTMENT,
                    "MANUAL",
                    null,
                    observation,
                    movementDate
            );

            redirectAttributes.addFlashAttribute("message", "Stock updated successfully.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error while updating stock: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/warehouse/supplies-stock";
    }

    @GetMapping("/{id}/movements")
    public String viewMovements(@PathVariable Long id, Model model) {
        Supply supply = supplyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supply not found: " + id));

        model.addAttribute("supply", supply);
        model.addAttribute("movements", movementRepository.findBySupplyOrderByMovementDateDesc(supply));

        return "warehouse/supply_movements";
    }
}
