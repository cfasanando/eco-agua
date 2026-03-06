package com.ecoamazonas.eco_agua.publicweb;

import com.ecoamazonas.eco_agua.blog.BlogPost;
import com.ecoamazonas.eco_agua.blog.BlogPostRepository;
import com.ecoamazonas.eco_agua.config.PlatformSettingService;
import com.ecoamazonas.eco_agua.delivery.DeliveryZone;
import com.ecoamazonas.eco_agua.delivery.DeliveryZoneRepository;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionRepository;
import com.ecoamazonas.eco_agua.marketing.Testimonial;
import com.ecoamazonas.eco_agua.marketing.TestimonialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
public class PublicSiteController {

    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final BlogPostRepository blogPostRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;
    private final PlatformSettingService platformSettingService;

    // default WhatsApp from application.properties (fallback)
    @Value("${ecoagua.whatsapp.number:51980542101}")
    private String defaultWhatsappNumber;

    @Value("${google.maps.api-key:}")
    private String googleMapsApiKey;

    private final TestimonialRepository testimonialRepository;

    public PublicSiteController(ProductRepository productRepository,
                                PromotionRepository promotionRepository,
                                BlogPostRepository blogPostRepository,
                                DeliveryZoneRepository deliveryZoneRepository,
                                PlatformSettingService platformSettingService,
                                TestimonialRepository testimonialRepository) {
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.blogPostRepository = blogPostRepository;
        this.deliveryZoneRepository = deliveryZoneRepository;
        this.platformSettingService = platformSettingService;
        this.testimonialRepository = testimonialRepository;
    }

    @GetMapping({"/", "/portal"})
    public String home(Model model, Authentication authentication) {
        // If user is authenticated, redirect to internal home/dashboard
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/home";
        }

        // --- Data normal de la página ---
        List<Promotion> promotions = promotionRepository.findActiveForPublic(LocalDate.now());

        List<Product> featuredProducts =
                productRepository.findTop4ByActiveTrueAndFeaturedTrueOrderByIdDesc();
        if (featuredProducts.isEmpty()) {
            featuredProducts =
                    productRepository.findTop4ByActiveTrueAndCategoryNameOrderByIdDesc("Agua de mesa");
        }

        List<BlogPost> latestPosts =
                blogPostRepository.findTop3ByStatusOrderByPublishedAtDesc(BlogPost.Status.PUBLISHED);

        List<DeliveryZone> deliveryZones = deliveryZoneRepository.findAllByOrderByNameAsc();

        // --- Settings desde platform_setting ---
        // Los nombres de variable son EXACTAMENTE los que tienes en la tabla:
        // platform.name, platform.tagline, platform.logo, public.topbar.phone,
        // public.whatsapp.number, public.footer.left, public.footer.right

        String platformName = platformSettingService.get("platform.name", "Eco del Amazonas");
        String platformTagline = platformSettingService.get("platform.tagline", "Agua de mesa");
        String platformLogo = platformSettingService.get("platform.logo", "/img/logo3-transparente.png");

        String topbarPhone = platformSettingService.get("public.topbar.phone", "(065) 000000");

        // número de WhatsApp: si no está en tabla, usa el del properties
        String whatsappNumber = platformSettingService.get("public.whatsapp.number", defaultWhatsappNumber);

        // Texto derecha footer (el que estabas cambiando)
        String footerRight = platformSettingService.get(
                "public.footer.right",
                "Servicio de agua purificada a domicilio - Iquitos, Perú"
        );

        // Estos aún no están en la tabla; si no hay registro, se quedan con el default
        String topbarLocation = platformSettingService.get(
                "public.topbar.location",
                "Reparto en Iquitos y alrededores"
        );
        String topbarWhatsappLabel = platformSettingService.get(
                "public.topbar.whatsapp_label",
                "Pedidos por WhatsApp"
        );

        String heroPill = platformSettingService.get(
                "public.hero.pill",
                "Agua purificada, segura y confiable"
        );
        String heroTitle = platformSettingService.get(
                "public.hero.title",
                "Refresca tu vida con agua de mesa Eco del Amazonas"
        );
        String heroSubtitle = platformSettingService.get(
                "public.hero.subtitle",
                "Llevamos bidones y botellas de agua purificada hasta tu hogar u oficina, " +
                        "con reparto programado y atención personalizada."
        );
        String heroBullet1 = platformSettingService.get(
                "public.hero.bullet_1",
                "Proceso de purificación certificado."
        );
        String heroBullet2 = platformSettingService.get(
                "public.hero.bullet_2",
                "Entrega puntual en los horarios acordados."
        );
        String heroBullet3 = platformSettingService.get(
                "public.hero.bullet_3",
                "Atención por WhatsApp, fácil y rápido."
        );
        String heroPrimaryCtaLabel = platformSettingService.get(
                "public.hero.primary_cta_label",
                "Pedir ahora por WhatsApp"
        );
        String heroSecondaryCtaLabel = platformSettingService.get(
                "public.hero.secondary_cta_label",
                "Ver catálogo de productos"
        );
        String heroStat1 = platformSettingService.get(
                "public.hero.stat_1",
                "+500 familias atendidas"
        );
        String heroStat2 = platformSettingService.get(
                "public.hero.stat_2",
                "Reparto diario en Iquitos"
        );
        String heroStat3 = platformSettingService.get(
                "public.hero.stat_3",
                "Calidad y confianza"
        );
        String heroCardTitle = platformSettingService.get(
                "public.hero.card_title",
                "Agua de mesa Eco del Amazonas"
        );
        String heroCardSubtitle = platformSettingService.get(
                "public.hero.card_subtitle",
                "Bidones, botellas y planes para empresas"
        );
        String heroBadgeLabel = platformSettingService.get(
                "public.hero.badge_label",
                "Servicio destacado"
        );

        String finalCtaButtonLabel = platformSettingService.get(
                "public.final_cta.button_label",
                "Pedir recarga ahora"
        );
        String finalCtaSchedule = platformSettingService.get(
                "public.final_cta.schedule",
                "Atención de lunes a sábado, de 8:00 a.m. a 8:00 p.m."
        );

        // --- Model attributes ---
        model.addAttribute("promotions", promotions);
        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("latestBlogPosts", latestPosts);
        model.addAttribute("deliveryZones", deliveryZones);

        model.addAttribute("googleMapsApiKey", googleMapsApiKey);

        // Settings visibles en la vista
        model.addAttribute("platformName", platformName);
        model.addAttribute("platformTagline", platformTagline);
        model.addAttribute("platformLogo", platformLogo);

        model.addAttribute("topbarLocation", topbarLocation);
        model.addAttribute("topbarPhone", topbarPhone);
        model.addAttribute("topbarWhatsappLabel", topbarWhatsappLabel);
        model.addAttribute("whatsappNumber", whatsappNumber);

        model.addAttribute("heroPill", heroPill);
        model.addAttribute("heroTitle", heroTitle);
        model.addAttribute("heroSubtitle", heroSubtitle);
        model.addAttribute("heroBullet1", heroBullet1);
        model.addAttribute("heroBullet2", heroBullet2);
        model.addAttribute("heroBullet3", heroBullet3);
        model.addAttribute("heroPrimaryCtaLabel", heroPrimaryCtaLabel);
        model.addAttribute("heroSecondaryCtaLabel", heroSecondaryCtaLabel);
        model.addAttribute("heroStat1", heroStat1);
        model.addAttribute("heroStat2", heroStat2);
        model.addAttribute("heroStat3", heroStat3);
        model.addAttribute("heroCardTitle", heroCardTitle);
        model.addAttribute("heroCardSubtitle", heroCardSubtitle);
        model.addAttribute("heroBadgeLabel", heroBadgeLabel);

        model.addAttribute("finalCtaButtonLabel", finalCtaButtonLabel);
        model.addAttribute("finalCtaSchedule", finalCtaSchedule);
        model.addAttribute("footerRight", footerRight);
        
        List<Testimonial> testimonials =
        testimonialRepository.findTop5ByActiveTrueOrderByDisplayOrderAscIdAsc();

        model.addAttribute("testimonials", testimonials);


        return "public/home";
    }

    @GetMapping("/catalogo")
    public String catalog(Model model) {
        List<Product> products =
                productRepository.findByActiveTrueOrderByCategoryNameAscNameAsc();

        // mismo número de WhatsApp dinámico
        String whatsappNumber = platformSettingService.get("public.whatsapp.number", defaultWhatsappNumber);

        model.addAttribute("products", products);
        model.addAttribute("whatsappNumber", whatsappNumber);

        return "public/catalog";
    }

    @PostMapping("/order/whatsapp")
    public String sendOrderToWhatsApp(
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("customerName") String customerName,
            @RequestParam("customerPhone") String customerPhone,
            @RequestParam("customerAddress") String customerAddress,
            @RequestParam(value = "customerType", required = false) String customerType,
            @RequestParam(value = "extraNotes", required = false) String extraNotes
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // Build WhatsApp message that contains all the info needed
        String message = buildWhatsAppMessage(
                product,
                quantity,
                customerName,
                customerPhone,
                customerAddress,
                customerType,
                extraNotes
        );

        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);

        // WhatsApp number from DB settings or fallback
        String whatsappNumber = platformSettingService.get("public.whatsapp.number", defaultWhatsappNumber);

        return "redirect:https://wa.me/" + whatsappNumber + "?text=" + encoded;
    }

    private String buildWhatsAppMessage(
        Product product,
        Integer quantity,
        String customerName,
        String customerPhone,
        String customerAddress,
        String customerType,
        String extraNotes
    ) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("Hola 👋, quiero hacer un pedido desde la web de Eco Agua.%0A%0A");

        // Customer type
        sb.append("👤 Tipo de cliente: ");
        if ("EXISTING".equalsIgnoreCase(customerType)) {
            sb.append("Ya soy cliente");
        } else if ("NEW".equalsIgnoreCase(customerType)) {
            sb.append("Cliente nuevo");
        } else {
            sb.append("No indicado");
        }
        sb.append("%0A%0A");

        // Customer info
        sb.append("🧑 Datos del cliente:%0A");
        sb.append("- Nombre: ").append(customerName).append("%0A");
        sb.append("- Celular: ").append(customerPhone).append("%0A");
        sb.append("- Dirección (con referencia): ").append(customerAddress).append("%0A%0A");

        // Order info
        sb.append("🧴 Detalle del pedido:%0A");
        sb.append("- Producto: ").append(product.getName()).append("%0A");
        sb.append("- Cantidad: ").append(quantity).append("%0A");
        sb.append("- Precio unitario: S/. ").append(product.getPrice()).append("%0A%0A");

        // Extra notes
        if (extraNotes != null && !extraNotes.isBlank()) {
            sb.append("📝 Notas para el reparto:%0A");
            sb.append(extraNotes.trim()).append("%0A%0A");
        }

        sb.append("Por favor confirmar el total y el tiempo de entrega. Gracias.");

        return sb.toString();
    }

}
