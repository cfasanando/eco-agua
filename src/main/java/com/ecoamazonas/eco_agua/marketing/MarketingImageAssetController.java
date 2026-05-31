package com.ecoamazonas.eco_agua.marketing;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/marketing/admin")
public class MarketingImageAssetController {

    private final MarketingImageAssetService imageAssetService;

    public MarketingImageAssetController(MarketingImageAssetService imageAssetService) {
        this.imageAssetService = imageAssetService;
    }

    @GetMapping("/image-library")
    public String imageLibrary(@RequestParam(value = "id", required = false) Long id, Model model) {
        MarketingImageAsset imageForm = imageAssetService.findForm(id);

        model.addAttribute("activePage", "marketing_image_library");
        model.addAttribute("imageForm", imageForm);
        model.addAttribute("imageRows", imageAssetService.findRows());
        model.addAttribute("products", imageAssetService.findActiveProducts());
        model.addAttribute("campaigns", imageAssetService.findCampaigns());
        model.addAttribute("promotions", imageAssetService.findPromotions());
        model.addAttribute("assetTypes", MarketingImageAsset.AssetType.values());
        model.addAttribute("recommendedChannels", MarketingImageAsset.RecommendedChannel.values());
        model.addAttribute("imageStatuses", MarketingImageAsset.Status.values());
        model.addAttribute("selectedProductId", imageAssetService.selectedProductId(imageForm));
        model.addAttribute("selectedCampaignId", imageAssetService.selectedCampaignId(imageForm));
        model.addAttribute("selectedPromotionId", imageAssetService.selectedPromotionId(imageForm));
        model.addAttribute("isImageEdit", imageForm.getId() != null);
        return "marketing/admin_image_library";
    }

    @PostMapping("/image-library/save")
    public String saveImageAsset(@ModelAttribute("imageForm") MarketingImageAsset imageForm,
                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                 @RequestParam(value = "productId", required = false) Long productId,
                                 @RequestParam(value = "campaignId", required = false) Long campaignId,
                                 @RequestParam(value = "promotionId", required = false) Long promotionId,
                                 RedirectAttributes redirectAttributes) {
        try {
            imageAssetService.save(imageForm, imageFile, productId, campaignId, promotionId);
            redirectAttributes.addFlashAttribute("successMessage", "Imagen guardada correctamente.");
        } catch (IllegalArgumentException | IOException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo guardar la imagen. Revisa el archivo o la ruta ingresada.");
        }
        return "redirect:/marketing/admin/image-library";
    }

    @PostMapping("/image-library/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        imageAssetService.archive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Imagen archivada correctamente.");
        return "redirect:/marketing/admin/image-library";
    }

    @PostMapping("/image-library/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        imageAssetService.restore(id);
        redirectAttributes.addFlashAttribute("successMessage", "Imagen reactivada correctamente.");
        return "redirect:/marketing/admin/image-library";
    }
}
