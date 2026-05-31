package com.ecoamazonas.eco_agua.promotion;

import com.ecoamazonas.eco_agua.product.ProductService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/admin/promotions")
public class PromotionController {

    private static final String PROMO_UPLOAD_DIR = "uploads/promotions";

    private final PromotionService promotionService;
    private final ProductService productService;
    private final PromotionRepository promotionRepository;

    public PromotionController(PromotionService promotionService,
                               ProductService productService,
                               PromotionRepository promotionRepository) {
        this.promotionService = promotionService;
        this.productService = productService;
        this.promotionRepository = promotionRepository;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("promotions", promotionService.findAll());
        model.addAttribute("products", productService.findAll());
        return "admin/promotions";
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer promoNumber,
            @RequestParam(required = false) String colorBorder,
            @RequestParam(value = "enabled", required = false) Boolean enabledParam,
            @RequestParam(required = false) Integer maxCounter,
            @RequestParam(value = "bannerImageFile", required = false) MultipartFile bannerImageFile,
            RedirectAttributes redirectAttributes
    ) {
        try {
            boolean enabled = Boolean.TRUE.equals(enabledParam);

            // Save basic promotion data using existing service
            promotionService.saveFromForm(
                    id, name, description, startDate, endDate,
                    promoNumber, colorBorder, enabled, maxCounter
            );

            // Handle banner upload (supports create and edit)
            if (bannerImageFile != null && !bannerImageFile.isEmpty()) {
                Promotion promotion;
                if (id != null) {
                    promotion = promotionRepository.findById(id).orElse(null);
                } else {
                    // For new promotions we get the latest record with the same name
                    promotion = promotionRepository.findTopByNameOrderByCreatedAtDesc(name);
                }

                if (promotion != null) {
                    String bannerPath = storePromotionBanner(bannerImageFile, promotion.getId());
                    promotion.setBannerImagePath(bannerPath);
                    promotionRepository.save(promotion);
                }
            }

            redirectAttributes.addFlashAttribute("message", "Promoción guardada correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al guardar la promoción.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            promotionService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Promoción eliminada correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar la promoción.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/promotions";
    }

    @PostMapping("/configure-products")
    public String configureProducts(
            @RequestParam("promotionId") Long promotionId,
            @RequestParam(value = "productId", required = false) List<Long> productIds,
            @RequestParam(value = "quantity", required = false) List<Integer> quantities,
            @RequestParam(value = "amount", required = false) List<BigDecimal> amounts,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (productIds == null) {
                productIds = new ArrayList<>();
            }

            promotionService.saveProductsConfig(promotionId, productIds, quantities, amounts);

            redirectAttributes.addFlashAttribute("message", "Productos de la promoción actualizados.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error al actualizar productos de la promoción.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/promotions";
    }

    /** Store banner file and return public path. */
    private String storePromotionBanner(MultipartFile file, Long promotionId) throws IOException {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        }

        String baseName = "promo-" + (promotionId != null ? promotionId : System.currentTimeMillis());
        String fileName = baseName + extension;

        Path uploadDir = Paths.get(PROMO_UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path target = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // Assuming /uploads/** is mapped to the "uploads" folder
        return "/uploads/promotions/" + fileName;
    }
}
