package com.ecoamazonas.eco_agua.config;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final BusinessProperties businessProperties;
    private final ClientFeatureProperties clientFeatureProperties;

    public GlobalModelAttributes(
            BusinessProperties businessProperties,
            ClientFeatureProperties clientFeatureProperties
    ) {
        this.businessProperties = businessProperties;
        this.clientFeatureProperties = clientFeatureProperties;
    }

    @ModelAttribute
    public void addBusinessAttributes(Model model) {
        model.addAttribute("businessProfileCode", businessProperties.getProfileCode());
        model.addAttribute("businessName", businessProperties.getName());
        model.addAttribute("businessShortName", businessProperties.getShortName());
        model.addAttribute("businessTagline", businessProperties.getTagline());
        model.addAttribute("businessType", businessProperties.getType());
        model.addAttribute("businessAdminTitle", businessProperties.getAdminTitle());
        model.addAttribute("businessLogo", businessProperties.getLogo());
        model.addAttribute("businessAdminLogo", businessProperties.getAdminLogo());
        model.addAttribute("businessWhatsappNumber", businessProperties.getWhatsappNumber());
        model.addAttribute("businessCatalogWhatsappIntro", businessProperties.getCatalogWhatsappIntro());
        model.addAttribute("businessProductLabel", businessProperties.getProductLabel());
        model.addAttribute("businessProductPluralLabel", businessProperties.getProductPluralLabel());
        model.addAttribute("businessCustomerLabel", businessProperties.getCustomerLabel());
        model.addAttribute("businessCustomerPluralLabel", businessProperties.getCustomerPluralLabel());
        model.addAttribute("businessSupplierLabel", businessProperties.getSupplierLabel());
        model.addAttribute("businessSupplierPluralLabel", businessProperties.getSupplierPluralLabel());
        model.addAttribute("businessSupplyLabel", businessProperties.getSupplyLabel());
        model.addAttribute("businessSupplyPluralLabel", businessProperties.getSupplyPluralLabel());
        model.addAttribute("businessDeliveryPersonLabel", businessProperties.getDeliveryPersonLabel());
        model.addAttribute("businessDeliveryLabel", businessProperties.getDeliveryLabel());
        model.addAttribute("businessContainerLabel", businessProperties.getContainerLabel());
        model.addAttribute("businessContainerPluralLabel", businessProperties.getContainerPluralLabel());
        model.addAttribute("businessProductionLabel", businessProperties.getProductionLabel());
        model.addAttribute("businessReorderLabel", businessProperties.getReorderLabel());
        model.addAttribute("businessProfilePriceHelpText", businessProperties.getProfilePriceHelpText());

        model.addAttribute("clientFeatures", clientFeatureProperties);
        model.addAttribute("featureContainers", clientFeatureProperties.isContainers());
        model.addAttribute("featureDelivery", clientFeatureProperties.isDelivery());
        model.addAttribute("featureProduction", clientFeatureProperties.isProduction());
        model.addAttribute("featureReorder", clientFeatureProperties.isReorder());
        model.addAttribute("featureMarketing", clientFeatureProperties.isMarketing());
        model.addAttribute("featureBlog", clientFeatureProperties.isBlog());
        model.addAttribute("featureTestimonials", clientFeatureProperties.isTestimonials());
        model.addAttribute("featurePublicCatalog", clientFeatureProperties.isPublicCatalog());
        model.addAttribute("featureSupplies", clientFeatureProperties.isSupplies());
        model.addAttribute("featureFixedCosts", clientFeatureProperties.isFixedCosts());
        model.addAttribute("featureBreakEven", clientFeatureProperties.isBreakEven());
        model.addAttribute("featurePriceSimulator", clientFeatureProperties.isPriceSimulator());
        model.addAttribute("featureMarketingSection", clientFeatureProperties.isMarketingSectionEnabled());
    }
}
