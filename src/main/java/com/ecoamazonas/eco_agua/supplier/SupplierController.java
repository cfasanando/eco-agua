package com.ecoamazonas.eco_agua.supplier;

import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.category.CategoryType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final CategoryRepository categoryRepository;

    public SupplierController(SupplierService supplierService,
                              CategoryRepository categoryRepository) {
        this.supplierService = supplierService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("supplierCategories",
                categoryRepository.findByTypeAndActiveTrueOrderByNameAsc(CategoryType.SUPPLIER));

        return "admin/suppliers";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam String docType,
            @RequestParam String docNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String contactName,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(defaultValue = "true") boolean active,
            @RequestParam(required = false) Long categoryId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            supplierService.saveFromForm(
                    id,
                    name,
                    docType,
                    docNumber,
                    address,
                    phone,
                    contactName,
                    contactPhone,
                    active,
                    categoryId
            );

            redirectAttributes.addFlashAttribute("message", "Proveedor guardado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al guardar el proveedor.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/suppliers";
    }

    @PostMapping("/{id}/delete")
    public String deleteSingle(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            supplierService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Proveedor eliminado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar el proveedor.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/suppliers";
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
            supplierService.deleteBulk(ids);
            redirectAttributes.addFlashAttribute("message", "Proveedores eliminados correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar los proveedores seleccionados.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/suppliers";
    }
}
