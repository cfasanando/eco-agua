package com.ecoamazonas.eco_agua.blog;

import com.ecoamazonas.eco_agua.config.BusinessProperties;
import com.ecoamazonas.eco_agua.config.PlatformSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/blog")
public class PublicBlogController {

    private final BlogPostRepository blogPostRepository;
    private final PlatformSettingService platformSettingService;
    private final BusinessProperties businessProperties;

    @Value("${ecoagua.whatsapp.number:51980542101}")
    private String defaultWhatsappNumber;

    public PublicBlogController(BlogPostRepository blogPostRepository,
                                PlatformSettingService platformSettingService,
                                BusinessProperties businessProperties) {
        this.blogPostRepository = blogPostRepository;
        this.platformSettingService = platformSettingService;
        this.businessProperties = businessProperties;
    }

    @GetMapping
    public String blogIndex(Model model) {
        List<BlogPost> posts = blogPostRepository.findAllByStatusOrderByPublishedAtDesc(BlogPost.Status.PUBLISHED);

        model.addAttribute("posts", posts);
        addPublicLayoutSettings(model);
        addBlogPageSettings(model);

        return "public/blog/list";
    }

    @GetMapping("/{slug}")
    public String blogDetail(@PathVariable String slug, Model model) {
        BlogPost post = blogPostRepository
                .findBySlugAndStatus(slug, BlogPost.Status.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<BlogPost> latest = blogPostRepository.findTop3ByStatusOrderByPublishedAtDesc(BlogPost.Status.PUBLISHED);

        model.addAttribute("post", post);
        model.addAttribute("latestPosts", latest);
        addPublicLayoutSettings(model);
        addBlogPageSettings(model);

        return "public/blog/detail";
    }

    private void addPublicLayoutSettings(Model model) {
        String platformName = setting("platform.name", businessProperties.getName());
        String platformTagline = setting("platform.tagline", businessProperties.getTagline());
        String platformLogo = setting("platform.logo", businessProperties.getLogo());
        String topbarLocation = setting("public.topbar.location", businessProperties.getLocation());
        String topbarPhone = setting("public.topbar.phone", businessProperties.getPhone());
        String topbarWhatsappLabel = setting("public.topbar.whatsapp_label", businessProperties.getTopbarWhatsappLabel());
        String whatsappNumber = setting("public.whatsapp.number", getDefaultWhatsappNumber());
        String footerRight = setting("public.footer.right", businessProperties.getFooterRight());

        model.addAttribute("platformName", platformName);
        model.addAttribute("platformTagline", platformTagline);
        model.addAttribute("platformLogo", platformLogo);
        model.addAttribute("topbarLocation", topbarLocation);
        model.addAttribute("topbarPhone", topbarPhone);
        model.addAttribute("topbarPhoneLabel", setting("public.topbar.phone_label", "Central:"));
        model.addAttribute("topbarWhatsappLabel", topbarWhatsappLabel);
        model.addAttribute("whatsappNumber", whatsappNumber);
        model.addAttribute("footerRight", footerRight);

        model.addAttribute("publicPrimaryColor", setting("public.theme.primary_color", "#2e7d32"));
        model.addAttribute("publicSecondaryColor", setting("public.theme.secondary_color", "#0277bd"));
        model.addAttribute("publicNavHomeLabel", setting("public.nav.home_label", "Inicio"));
        model.addAttribute("publicNavCatalogLabel", setting("public.nav.catalog_label", "Catálogo"));
        model.addAttribute("publicNavBlogLabel", setting("public.nav.blog_label", "Blog & consejos"));
        model.addAttribute("publicNavWhatsappLabel", setting("public.nav.whatsapp_label", "Pedir por WhatsApp"));
        model.addAttribute("publicAccessLabel", setting("public.nav.access_label", "Acceso colaboradores"));
    }

    private void addBlogPageSettings(Model model) {
        model.addAttribute("blogPageTitle", setting("public.blog.page_title", "Blog & consejos"));
        model.addAttribute("blogPageSubtitle", setting("public.blog.subtitle", "Ideas, tips y recomendaciones para nuestros clientes."));
        model.addAttribute("blogBullet1", setting("public.blog.bullet_1", "Consejos prácticos para el día a día."));
        model.addAttribute("blogBullet2", setting("public.blog.bullet_2", "Recomendaciones para familias y negocios."));
        model.addAttribute("blogBullet3", setting("public.blog.bullet_3", "Información útil para comprar mejor."));
        model.addAttribute("blogHeroImage", setting("public.blog.hero_image", "/img/banner02.jpg"));
        model.addAttribute("blogEmptyText", setting("public.blog.empty_text", "Próximamente publicaremos nuestros primeros artículos."));
        model.addAttribute("blogAboutTitle", setting("public.blog.about_title", "Sobre " + businessProperties.getName()));
        model.addAttribute("blogAboutText1", setting("public.blog.about_text_1", "Compartimos información útil para nuestros clientes."));
        model.addAttribute("blogAboutText2", setting("public.blog.about_text_2", "Nuestro objetivo es ayudarte a elegir mejor y coordinar tus pedidos con confianza."));
        model.addAttribute("blogCtaTitle", setting("public.blog.cta_title", "¿Necesitas hacer un pedido?"));
        model.addAttribute("blogCtaText", setting("public.blog.cta_text", "Escríbenos por WhatsApp y coordinamos tu atención."));
        model.addAttribute("blogCtaButtonLabel", setting("public.blog.cta_button_label", "Pedir por WhatsApp"));
        model.addAttribute("blogLatestTitle", setting("public.blog.latest_title", "Últimos artículos"));
        model.addAttribute("blogArticleCtaText", setting("public.blog.article_cta_text", "¿Te sirvió esta información? Escríbenos por WhatsApp para coordinar tu pedido."));
        model.addAttribute("blogArticleCtaButtonLabel", setting("public.blog.article_cta_button_label", "Pedir por WhatsApp"));
        model.addAttribute("blogQuickTipTitle", setting("public.blog.quick_tip_title", "Consejo rápido"));
        model.addAttribute("blogQuickTipText", setting("public.blog.quick_tip_text", "Consulta siempre la disponibilidad antes de coordinar tu pedido."));
    }

    private String setting(String variable, String defaultValue) {
        return platformSettingService.get(variable, defaultValue);
    }

    private String getDefaultWhatsappNumber() {
        String configuredNumber = businessProperties.getWhatsappNumber();

        if (configuredNumber != null && !configuredNumber.isBlank()) {
            return configuredNumber;
        }

        return defaultWhatsappNumber;
    }
}
