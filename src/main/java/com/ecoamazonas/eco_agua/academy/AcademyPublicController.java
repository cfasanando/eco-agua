package com.ecoamazonas.eco_agua.academy;

import com.ecoamazonas.eco_agua.config.BusinessProperties;
import com.ecoamazonas.eco_agua.config.PlatformSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class AcademyPublicController {

    private final AcademyCourseService courseService;
    private final PlatformSettingService platformSettingService;
    private final BusinessProperties businessProperties;

    @Value("${ecoagua.whatsapp.number:51980542101}")
    private String defaultWhatsappNumber;

    public AcademyPublicController(AcademyCourseService courseService,
                                   PlatformSettingService platformSettingService,
                                   BusinessProperties businessProperties) {
        this.courseService = courseService;
        this.platformSettingService = platformSettingService;
        this.businessProperties = businessProperties;
    }

    @GetMapping("/academy")
    public String academyCatalog(@RequestParam(value = "q", required = false) String query,
                                 @RequestParam(value = "category", required = false) String category,
                                 Model model) {
        List<AcademyCourse> courses = courseService.findPublishedForCatalog(query, category);
        List<AcademyCourse> featuredCourses = courseService.findFeaturedPublished();

        addPublicLayoutSettings(model);
        addAcademySettings(model);
        addPublicSeo(model,
                setting("public.academy.seo.title", setting("public.academy.title", "Academia y cursos") + " - " + setting("platform.name", businessProperties.getName())),
                setting("public.academy.seo.description", "Cursos prácticos, talleres y capacitaciones para emprendedores, negocios e instituciones."),
                "/academy",
                setting("public.academy.seo.image", setting("platform.logo", businessProperties.getLogo())),
                "website");

        model.addAttribute("courses", courses);
        model.addAttribute("featuredCourses", featuredCourses);
        model.addAttribute("courseCategories", courseService.findCategories());
        model.addAttribute("q", clean(query));
        model.addAttribute("selectedCategory", clean(category));
        model.addAttribute("filteredCoursesCount", courses.size());
        return "academy/catalog";
    }

    @GetMapping("/academy/course/{slug}")
    public String courseDetail(@PathVariable String slug, Model model) {
        AcademyCourse course = courseService.findPublishedBySlug(slug);
        if (course == null) {
            return "redirect:/academy";
        }

        addPublicLayoutSettings(model);
        addAcademySettings(model);
        addPublicSeo(model,
                course.getTitle() + " - " + setting("platform.name", businessProperties.getName()),
                course.getShortDescription(),
                course.getPublicPath(),
                course.getCoverImageUrl(),
                "article");

        model.addAttribute("course", course);
        model.addAttribute("relatedCourses", courseService.findPublishedForCatalog(null, course.getCategory()).stream()
                .filter(item -> !item.getId().equals(course.getId()))
                .limit(3)
                .toList());
        model.addAttribute("encodedCourseWhatsappMessage", URLEncoder.encode(courseService.buildWhatsappMessage(course), StandardCharsets.UTF_8));
        return "academy/detail";
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
        model.addAttribute("topbarPhone", setting("public.topbar.phone", businessProperties.getPhone()));
        model.addAttribute("topbarPhoneLabel", setting("public.topbar.phone_label", "Central:"));
        model.addAttribute("topbarWhatsappLabel", setting("public.topbar.whatsapp_label", "WhatsApp"));
        model.addAttribute("whatsappNumber", whatsappNumber);
        model.addAttribute("footerRight", setting("public.footer.right", businessProperties.getFooterRight()));
        model.addAttribute("publicPrimaryColor", setting("public.theme.primary_color", "#2e7d32"));
        model.addAttribute("publicSecondaryColor", setting("public.theme.secondary_color", "#0277bd"));
        model.addAttribute("publicNavHomeLabel", setting("public.nav.home_label", "Inicio"));
        model.addAttribute("publicNavCatalogLabel", setting("public.nav.catalog_label", "Catálogo"));
        model.addAttribute("publicNavBlogLabel", setting("public.nav.blog_label", "Blog & consejos"));
        model.addAttribute("publicNavAcademyLabel", setting("public.nav.academy_label", "Academia"));
        model.addAttribute("publicNavWhatsappLabel", setting("public.nav.whatsapp_label", "Pedir por WhatsApp"));
        model.addAttribute("publicAccessLabel", setting("public.nav.access_label", "Acceso colaboradores"));
    }

    private void addAcademySettings(Model model) {
        model.addAttribute("academyTitle", setting("public.academy.title", "Academia y cursos"));
        model.addAttribute("academySubtitle", setting("public.academy.subtitle", "Cursos prácticos para aprender, emprender y mejorar la gestión de negocios."));
        model.addAttribute("academyKicker", setting("public.academy.kicker", "Capacitación digital"));
        model.addAttribute("academyEmptyText", setting("public.academy.empty_text", "Aún no hay cursos publicados."));
        model.addAttribute("academyWhatsappCta", setting("public.academy.whatsapp_cta", "Solicitar información"));
    }

    private void addPublicSeo(Model model, String title, String description, String canonicalPath, String imagePath, String type) {
        String cleanTitle = clean(title).isBlank() ? setting("platform.name", businessProperties.getName()) : clean(title);
        String cleanDescription = limit(stripHtml(description), 160);
        if (cleanDescription.isBlank()) {
            cleanDescription = setting("platform.tagline", businessProperties.getTagline());
        }
        String cleanPath = clean(canonicalPath).isBlank() ? "/academy" : clean(canonicalPath);
        String siteBaseUrl = trimTrailingSlash(setting("public.site.base_url", ""));

        model.addAttribute("seoTitle", cleanTitle);
        model.addAttribute("seoDescription", cleanDescription);
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

    private String limit(String value, int maxLength) {
        String cleanValue = clean(value);
        if (cleanValue.length() <= maxLength) {
            return cleanValue;
        }
        return cleanValue.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }

    private String stripHtml(String value) {
        return clean(value).replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private String trimTrailingSlash(String value) {
        String cleanValue = clean(value);
        while (cleanValue.endsWith("/") && cleanValue.length() > 1) {
            cleanValue = cleanValue.substring(0, cleanValue.length() - 1);
        }
        return cleanValue;
    }
}
