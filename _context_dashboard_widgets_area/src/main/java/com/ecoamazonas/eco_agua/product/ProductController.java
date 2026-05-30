package com.ecoamazonas.eco_agua.product;

import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.category.CategoryType;
import com.ecoamazonas.eco_agua.product.cost.ProductCostDetail;
import com.ecoamazonas.eco_agua.product.cost.ProductCostService;
import com.ecoamazonas.eco_agua.supply.SupplyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/admin/products")
public class ProductController {

    private final ProductService productService;
    private final SupplyService supplyService;
    private final CategoryRepository categoryRepository;
    // --- NEW: cost service ---
    private final ProductCostService productCostService;

    // Directory where product images will be stored (relative to project root)
    private static final String PRODUCT_UPLOAD_DIR = "uploads/products";

    public ProductController(
            ProductService productService,
            SupplyService supplyService,
            CategoryRepository categoryRepository,
            ProductCostService productCostService
    ) {
        this.productService = productService;
        this.supplyService = supplyService;
        this.categoryRepository = categoryRepository;
        this.productCostService = productCostService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("supplies", supplyService.findAll());
        // Only categories of type "product"
        model.addAttribute("productCategories",
                categoryRepository.findByTypeAndActiveTrueOrderByNameAsc(CategoryType.PRODUCT));

        return "admin/products";
    }

    // --- NEW: cost detail page ---
    @GetMapping("/{id}/cost")
    public String showCostDetail(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ProductCostDetail detail = productCostService.calculateCostDetail(id);
            model.addAttribute("detail", detail);
            return "admin/product_cost_detail";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", "Product not found.");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(defaultValue = "false") boolean active,     // <-- was "true"
            @RequestParam(value = "featured", defaultValue = "false") boolean featured, // <-- NEW
            @RequestParam(required = false) Long categoryId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String storedImagePath = null;

            if (imageFile != null && !imageFile.isEmpty()) {
                storedImagePath = storeProductImage(imageFile, id);
            }

            productService.saveFromForm(
                    id,
                    name,
                    description,
                    price,
                    active,
                    featured,
                    categoryId,
                    storedImagePath
            );

            redirectAttributes.addFlashAttribute("message", "Producto guardado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al guardar el producto.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String deleteSingle(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Producto eliminado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar el producto.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/products";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(
            @RequestParam("ids") List<Long> ids,
            RedirectAttributes redirectAttributes
    ) {
        try {
            productService.deleteBulk(ids);
            redirectAttributes.addFlashAttribute("message", "Productos eliminados correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar los productos seleccionados.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/products";
    }

    @PostMapping("/configure-supplies")
    public String configureSupplies(
            @RequestParam("productId") Long productId,
            @RequestParam(value = "supplyId", required = false) List<Long> supplyIds,
            @RequestParam(value = "quantityUsed", required = false) List<BigDecimal> quantitiesUsed,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (supplyIds == null) {
                supplyIds = new ArrayList<>();
            }

            productService.saveSuppliesConfig(productId, supplyIds, quantitiesUsed);

            redirectAttributes.addFlashAttribute("message", "Insumos del producto actualizados.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al actualizar insumos del producto.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/products";
    }

    // -------- Helpers --------

    private String storeProductImage(MultipartFile file, Long productId) throws IOException {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        }

        String baseName;
        if (productId != null) {
            baseName = "product-" + productId;
        } else {
            baseName = "product-" + System.currentTimeMillis();
        }

        String fileName = baseName + extension;
        Path uploadDir = Path.of(PRODUCT_UPLOAD_DIR);
        Files.createDirectories(uploadDir);

        Path target = uploadDir.resolve(fileName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        // URL that will be used in templates. You can map /uploads/** to this folder.
        return "/uploads/products/" + fileName;
    }
}
