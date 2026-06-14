package com.ecoamazonas.eco_agua.academy;

import com.ecoamazonas.eco_agua.config.BusinessProperties;
import com.ecoamazonas.eco_agua.config.PlatformSettingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AcademyLeadPublicController {

    private final AcademyCourseRepository courseRepository;
    private final AcademyLeadService leadService;
    private final PlatformSettingService platformSettingService;
    private final BusinessProperties businessProperties;

    @Value("${ecoagua.whatsapp.number:51980542101}")
    private String defaultWhatsappNumber;

    public AcademyLeadPublicController(AcademyCourseRepository courseRepository,
                                       AcademyLeadService leadService,
                                       PlatformSettingService platformSettingService,
                                       BusinessProperties businessProperties) {
        this.courseRepository = courseRepository;
        this.leadService = leadService;
        this.platformSettingService = platformSettingService;
        this.businessProperties = businessProperties;
    }

    @GetMapping("/academy/course/{slug}/request")
    public String requestForm(@PathVariable String slug, Model model) {
        AcademyCourse course = courseRepository.findBySlugAndActiveTrueAndStatus(slug, AcademyCourse.Status.PUBLISHED)
                .orElse(null);
        if (course == null) {
            return "redirect:/academy";
        }
        addPublicLayoutSettings(model);
        addPublicSeo(model, "Solicitar inscripción - " + course.getTitle(), course.getShortDescription(), "/academy/course/" + slug + "/request", course.getCoverImageUrl(), "website");
        model.addAttribute("course", course);
        if (!model.containsAttribute("leadForm")) {
            AcademyLeadRequestForm form = new AcademyLeadRequestForm();
            form.setSource(AcademyLead.Source.CATALOG);
            model.addAttribute("leadForm", form);
        }
        model.addAttribute("leadSources", AcademyLead.Source.values());
        return "academy/request";
    }

    @PostMapping("/academy/course/{slug}/request")
    public String submitRequest(@PathVariable String slug,
                                @ModelAttribute("leadForm") AcademyLeadRequestForm form,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            leadService.createPublicRequest(slug, form, request != null ? request.getRequestURI() : "/academy");
            redirectAttributes.addFlashAttribute("successMessage", "Solicitud enviada correctamente. Te contactaremos pronto.");
            return "redirect:/academy/course/" + slug + "/request?sent=1";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("warningMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("leadForm", form);
            return "redirect:/academy";
        }
    }

    private void addPublicLayoutSettings(Model model) {
        String platformName = setting("platform.name", businessProperties.getName());
        String platformTagline = setting("platform.tagline", businessProperties.getTagline());
        String platformLogo = setting("platform.logo", businessProperties.getLogo());
        String whatsappNumber = setting("public.whatsapp.number", defaultWhatsappNumber);

        model.addAttribute("platformName", platformName);
        model.addAttribute("platformTagline", platformTagline);
        model.addAttribute("platformLogo", platformLogo);
        model.addAttribute("topbarLocation", setting("public.topbar.location", businessProperties.getLocation()));
        model.addAttribute("topbarWhatsappLabel", setting("public.topbar.whatsapp_label", "WhatsApp"));
        model.addAttribute("whatsappNumber", whatsappNumber);
        model.addAttribute("publicPrimaryColor", setting("public.theme.primary_color", "#2e7d32"));
        model.addAttribute("publicSecondaryColor", setting("public.theme.secondary_color", "#0277bd"));
        model.addAttribute("publicNavHomeLabel", setting("public.nav.home_label", "Inicio"));
        model.addAttribute("publicNavCatalogLabel", setting("public.nav.catalog_label", "Catálogo"));
        model.addAttribute("publicNavBlogLabel", setting("public.nav.blog_label", "Blog & consejos"));
        model.addAttribute("publicNavAcademyLabel", setting("public.nav.academy_label", "Academia"));
    }

    private void addPublicSeo(Model model, String title, String description, String canonicalPath, String imagePath, String type) {
        String siteBaseUrl = trimTrailingSlash(setting("public.site.base_url", ""));
        String cleanPath = clean(canonicalPath).isBlank() ? "/academy" : clean(canonicalPath);
        model.addAttribute("seoTitle", clean(title));
        model.addAttribute("seoDescription", clean(description).isBlank() ? setting("platform.tagline", businessProperties.getTagline()) : clean(description));
        model.addAttribute("seoCanonicalPath", cleanPath);
        model.addAttribute("seoCanonicalUrl", siteBaseUrl.isBlank() ? cleanPath : siteBaseUrl + cleanPath);
        model.addAttribute("seoOgImage", clean(imagePath).isBlank() ? setting("platform.logo", businessProperties.getLogo()) : imagePath);
        model.addAttribute("seoOgType", clean(type).isBlank() ? "website" : type);
    }

    private String setting(String variable, String defaultValue) {
        return platformSettingService.get(variable, defaultValue);
    }

    private String clean(String value) {
        return value != null ? value.trim() : "";
    }

    private String trimTrailingSlash(String value) {
        String cleanValue = clean(value);
        while (cleanValue.endsWith("/") && cleanValue.length() > 1) {
            cleanValue = cleanValue.substring(0, cleanValue.length() - 1);
        }
        return cleanValue;
    }
}
