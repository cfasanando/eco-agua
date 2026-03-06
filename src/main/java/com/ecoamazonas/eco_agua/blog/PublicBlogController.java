package com.ecoamazonas.eco_agua.blog;

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

    // Default WhatsApp number from application.properties (fallback)
    @Value("${ecoagua.whatsapp.number:51980542101}")
    private String defaultWhatsappNumber;

    public PublicBlogController(BlogPostRepository blogPostRepository,
                                PlatformSettingService platformSettingService) {
        this.blogPostRepository = blogPostRepository;
        this.platformSettingService = platformSettingService;
    }

    @GetMapping
    public String blogIndex(Model model) {
        // All published posts, newest first
        List<BlogPost> posts =
                blogPostRepository.findAllByStatusOrderByPublishedAtDesc(BlogPost.Status.PUBLISHED);

        model.addAttribute("posts", posts);

        // Add shared public layout settings (header/footer/topbar)
        addPublicLayoutSettings(model);

        // Matches templates/public/blog/list.html
        return "public/blog/list";
    }

    @GetMapping("/{slug}")
    public String blogDetail(@PathVariable String slug, Model model) {
        // Only published posts
        BlogPost post = blogPostRepository
                .findBySlugAndStatus(slug, BlogPost.Status.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Sidebar: last 3 posts
        List<BlogPost> latest =
                blogPostRepository.findTop3ByStatusOrderByPublishedAtDesc(BlogPost.Status.PUBLISHED);

        model.addAttribute("post", post);
        model.addAttribute("latestPosts", latest);

        // Shared layout settings
        addPublicLayoutSettings(model);

        // Matches templates/public/blog/detail.html
        return "public/blog/detail";
    }

    /**
     * Adds common public layout settings to the model
     * so all blog pages share the same header/footer configuration.
     */
    private void addPublicLayoutSettings(Model model) {
        // Basic platform info
        String platformName = platformSettingService.get("platform.name", "Eco del Amazonas");
        String platformTagline = platformSettingService.get("platform.tagline", "Agua de mesa");
        String platformLogo = platformSettingService.get("platform.logo", "/img/logo3-transparente.png");

        // Top bar settings
        String topbarLocation = platformSettingService.get(
                "public.topbar.location",
                "Reparto en Iquitos y alrededores"
        );
        String topbarPhone = platformSettingService.get(
                "public.topbar.phone",
                "(065) 000000"
        );
        String topbarWhatsappLabel = platformSettingService.get(
                "public.topbar.whatsapp_label",
                "Pedidos por WhatsApp"
        );

        // WhatsApp number (DB or fallback)
        String whatsappNumber = platformSettingService.get(
                "public.whatsapp.number",
                defaultWhatsappNumber
        );

        // Footer right text
        String footerRight = platformSettingService.get(
                "public.footer.right",
                "Servicio de agua purificada a domicilio - Iquitos, Perú"
        );

        // Add attributes to model (used by blog/list.html y blog/detail.html)
        model.addAttribute("platformName", platformName);
        model.addAttribute("platformTagline", platformTagline);
        model.addAttribute("platformLogo", platformLogo);

        model.addAttribute("topbarLocation", topbarLocation);
        model.addAttribute("topbarPhone", topbarPhone);
        model.addAttribute("topbarWhatsappLabel", topbarWhatsappLabel);
        model.addAttribute("whatsappNumber", whatsappNumber);

        model.addAttribute("footerRight", footerRight);
    }
}
