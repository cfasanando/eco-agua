package com.ecoamazonas.eco_agua.supply;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/supplies")
public class SupplyController {

    private final SupplyService supplyService;

    public SupplyController(SupplyService supplyService) {
        this.supplyService = supplyService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("supplies", supplyService.findAll());
        return "admin/supplies";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam String unit,
            @RequestParam BigDecimal baseQuantity,
            @RequestParam BigDecimal baseCost,
            @RequestParam(required = false) BigDecimal stock,
            @RequestParam(defaultValue = "true") boolean active,
            RedirectAttributes redirectAttributes
    ) {
        try {
            supplyService.saveFromForm(
                    id, name, description, unit, baseQuantity, baseCost, stock, active
            );
            redirectAttributes.addFlashAttribute("message", "Insumo guardado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al guardar el insumo.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/supplies";
    }

    @PostMapping("/{id}/delete")
    public String deleteSingle(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            supplyService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Insumo eliminado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar el insumo.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/supplies";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(
            @RequestParam("ids") List<Long> ids,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (ids == null) {
                ids = new ArrayList<>();
            }

            supplyService.deleteBulk(ids);
            redirectAttributes.addFlashAttribute("message", "Insumos eliminados correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar los insumos seleccionados.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/supplies";
    }
}
