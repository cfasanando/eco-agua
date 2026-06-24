package com.ecoamazonas.eco_agua.appearance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@ConditionalOnProperty(
        name = "matrix26.control-center.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class InstanceBrandingModelAdvice {

    private final InstanceBrandingConfigurationService configurationService;

    public InstanceBrandingModelAdvice(InstanceBrandingConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @ModelAttribute
    public void addBranding(Model model) {
        InstanceBrandingConfiguration configuration = configurationService.current();
        model.addAttribute("appearanceBrandingManaged", configuration.managed());
        model.addAttribute("appearanceBrandDisplayName", configuration.branding("displayName", null));
        model.addAttribute("appearanceBrandShortName", configuration.branding("shortName", null));
        model.addAttribute("appearanceBrandTagline", configuration.branding("tagline", null));
        model.addAttribute("appearanceWelcomeMessage", configuration.branding("welcomeMessage", null));
        model.addAttribute("appearanceHeroTitle", configuration.branding("heroTitle", null));
        model.addAttribute("appearanceHeroSubtitle", configuration.branding("heroSubtitle", null));
        model.addAttribute("appearancePrimaryCtaLabel", configuration.branding("primaryCtaLabel", null));
        model.addAttribute("appearanceSecondaryCtaLabel", configuration.branding("secondaryCtaLabel", null));
        model.addAttribute("appearanceContactPhone", configuration.branding("contactPhone", null));
        model.addAttribute("appearanceWhatsapp", configuration.branding("whatsapp", null));
        model.addAttribute("appearanceLocation", configuration.branding("location", null));

        model.addAttribute("appearanceLogoPrimaryUrl", assetUrl(configuration, "logo-primary"));
        model.addAttribute("appearanceLogoCompactUrl", assetUrl(configuration, "logo-compact"));
        model.addAttribute("appearanceFaviconUrl", assetUrl(configuration, "favicon"));
        model.addAttribute("appearanceLoginCoverUrl", assetUrl(configuration, "login-cover"));
        model.addAttribute("appearanceHeroPrimaryUrl", assetUrl(configuration, "hero-primary"));
        model.addAttribute("appearanceHeroSecondaryUrl", assetUrl(configuration, "hero-secondary"));
        model.addAttribute("appearanceProductPlaceholderUrl", assetUrl(configuration, "product-placeholder"));
        model.addAttribute("appearanceSocialShareUrl", assetUrl(configuration, "social-share"));
    }

    private String assetUrl(InstanceBrandingConfiguration configuration, String key) {
        String fileName = configuration.asset(key);
        return fileName == null || fileName.isBlank()
                ? null
                : "/runtime-assets/" + fileName;
    }
}
