package com.ecoamazonas.eco_agua.publicweb;

import com.ecoamazonas.eco_agua.blog.BlogPost;
import com.ecoamazonas.eco_agua.blog.BlogPostRepository;
import com.ecoamazonas.eco_agua.config.BusinessProperties;
import com.ecoamazonas.eco_agua.config.PlatformSettingService;
import com.ecoamazonas.eco_agua.delivery.DeliveryZone;
import com.ecoamazonas.eco_agua.delivery.DeliveryZoneRepository;
import com.ecoamazonas.eco_agua.marketing.Testimonial;
import com.ecoamazonas.eco_agua.marketing.TestimonialRepository;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.promotion.Promotion;
import com.ecoamazonas.eco_agua.promotion.PromotionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Controller
public class PublicSiteController {

    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final BlogPostRepository blogPostRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;
    private final PlatformSettingService platformSettingService;
    private final BusinessProperties businessProperties;
    private final TestimonialRepository testimonialRepository;

    @Value("${ecoagua.whatsapp.number:51980542101}")
    private String defaultWhatsappNumber;

    @Value("${google.maps.api-key:}")
    private String googleMapsApiKey;

    public PublicSiteController(ProductRepository productRepository,
                                PromotionRepository promotionRepository,
                                BlogPostRepository blogPostRepository,
                                DeliveryZoneRepository deliveryZoneRepository,
                                PlatformSettingService platformSettingService,
                                BusinessProperties businessProperties,
                                TestimonialRepository testimonialRepository) {
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.blogPostRepository = blogPostRepository;
        this.deliveryZoneRepository = deliveryZoneRepository;
        this.platformSettingService = platformSettingService;
        this.businessProperties = businessProperties;
        this.testimonialRepository = testimonialRepository;
    }

    @GetMapping({"/", "/portal"})
    public String home(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/home";
        }

        List<Promotion> promotions = promotionRepository.findActiveForPublic(LocalDate.now());

        List<Product> featuredProducts = productRepository.findTop4ByActiveTrueAndFeaturedTrueOrderByIdDesc();
        if (featuredProducts.isEmpty()) {
            featuredProducts = productRepository.findTop4ByActiveTrueAndCategoryNameOrderByIdDesc(
                    businessProperties.getFeaturedCategoryName()
            );
        }

        List<BlogPost> latestPosts = blogPostRepository.findTop3ByStatusOrderByPublishedAtDesc(BlogPost.Status.PUBLISHED);
        List<DeliveryZone> deliveryZones = deliveryZoneRepository.findAllByOrderByNameAsc();
        List<Testimonial> testimonials = testimonialRepository.findTop5ByActiveTrueOrderByDisplayOrderAscIdAsc();

        model.addAttribute("promotions", promotions);
        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("latestBlogPosts", latestPosts);
        model.addAttribute("deliveryZones", deliveryZones);
        model.addAttribute("testimonials", testimonials);
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);

        addPublicLayoutSettings(model);
        addHomeContentSettings(model);
        addCatalogContentSettings(model);

        return "public/home";
    }

    @GetMapping("/catalogo")
    public String catalog(Model model) {
        List<Product> products = productRepository.findByActiveTrueOrderByCategoryNameAscNameAsc();

        model.addAttribute("products", products);
        addPublicLayoutSettings(model);
        addCatalogContentSettings(model);

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
        String whatsappNumber = setting("public.whatsapp.number", getDefaultWhatsappNumber());

        return "redirect:https://wa.me/" + whatsappNumber + "?text=" + encoded;
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

    private void addHomeContentSettings(Model model) {
        model.addAttribute("publicPageTitle", setting("public.page.title", businessProperties.getName() + " - " + businessProperties.getTagline()));

        model.addAttribute("heroPill", setting("public.hero.pill", businessProperties.getHeroPill()));
        model.addAttribute("heroTitle", setting("public.hero.title", businessProperties.getHeroTitle()));
        model.addAttribute("heroSubtitle", setting("public.hero.subtitle", businessProperties.getHeroSubtitle()));
        model.addAttribute("heroBullet1", setting("public.hero.bullet_1", businessProperties.getHeroBullet1()));
        model.addAttribute("heroBullet2", setting("public.hero.bullet_2", businessProperties.getHeroBullet2()));
        model.addAttribute("heroBullet3", setting("public.hero.bullet_3", businessProperties.getHeroBullet3()));
        model.addAttribute("heroPrimaryCtaLabel", setting("public.hero.primary_cta_label", businessProperties.getHeroPrimaryCtaLabel()));
        model.addAttribute("heroSecondaryCtaLabel", setting("public.hero.secondary_cta_label", businessProperties.getHeroSecondaryCtaLabel()));
        model.addAttribute("heroStat1", setting("public.hero.stat_1", businessProperties.getHeroStat1()));
        model.addAttribute("heroStat2", setting("public.hero.stat_2", businessProperties.getHeroStat2()));
        model.addAttribute("heroStat3", setting("public.hero.stat_3", businessProperties.getHeroStat3()));
        model.addAttribute("heroCardTitle", setting("public.hero.card_title", businessProperties.getHeroCardTitle()));
        model.addAttribute("heroCardSubtitle", setting("public.hero.card_subtitle", businessProperties.getHeroCardSubtitle()));
        model.addAttribute("heroBadgeLabel", setting("public.hero.badge_label", businessProperties.getHeroBadgeLabel()));
        model.addAttribute("heroBackgroundImage", setting("public.hero.background_image", "/img/banner01.jpg"));

        String heroMainImage = setting("public.hero.main_image", "/img/banner02.jpg");
        String heroMainImageAlt = setting("public.hero.main_image_alt", businessProperties.getName());
        String heroThumbImage1 = setting("public.hero.thumb_image_1", "/img/banner01.jpg");
        String heroThumbImage2 = setting("public.hero.thumb_image_2", "/img/bienvenida.png");
        List<HeroCarouselSlide> heroCarouselSlides = buildHeroCarouselSlides(heroMainImage, heroMainImageAlt, heroThumbImage1, heroThumbImage2);

        model.addAttribute("heroMainImage", heroMainImage);
        model.addAttribute("heroMainImageAlt", heroMainImageAlt);
        model.addAttribute("heroThumbImage1", heroThumbImage1);
        model.addAttribute("heroThumbImage2", heroThumbImage2);
        model.addAttribute("heroCarouselSlides", heroCarouselSlides);
        model.addAttribute("heroInitialImage", heroCarouselSlides.get(0).getImage());
        model.addAttribute("heroInitialAlt", heroCarouselSlides.get(0).getAlt());

        for (int i = 1; i <= 4; i++) {
            model.addAttribute("trust" + i + "Icon", setting("public.trust." + i + ".icon", defaultTrustIcon(i)));
            model.addAttribute("trust" + i + "Title", setting("public.trust." + i + ".title", defaultTrustTitle(i)));
            model.addAttribute("trust" + i + "Text", setting("public.trust." + i + ".text", defaultTrustText(i)));
        }

        model.addAttribute("promotionsTitle", setting("public.promotions.title", "Promociones del mes"));
        model.addAttribute("promotionsSubtitle", setting("public.promotions.subtitle", "Aprovecha los precios especiales por volumen y recarga frecuente."));
        model.addAttribute("promotionsEmptyText", setting("public.promotions.empty_text", "No hay promociones activas por ahora."));

        model.addAttribute("featuredProductsTitle", setting("public.featured_products.title", "Productos destacados"));
        model.addAttribute("featuredProductsSubtitle", setting("public.featured_products.subtitle", "Los productos más pedidos por nuestros clientes."));
        model.addAttribute("featuredProductsLinkLabel", setting("public.featured_products.link_label", "Ver catálogo completo »"));
        model.addAttribute("featuredProductsEmptyText", setting("public.featured_products.empty_text", "No hay productos destacados configurados."));

        model.addAttribute("orderModalTitle", setting("public.order_modal.title", "Pedido por WhatsApp"));
        model.addAttribute("orderModalIntro", setting("public.order_modal.intro", "Para coordinar la entrega, por favor completa tus datos:"));
        model.addAttribute("orderModalQuantityLabel", setting("public.order_modal.quantity_label", "Cantidad"));
        model.addAttribute("orderModalAddressLabel", setting("public.order_modal.address_label", "Dirección (incluye distrito y referencia)"));
        model.addAttribute("orderModalNotesLabel", setting("public.order_modal.notes_label", "Notas para la entrega (opcional)"));
        model.addAttribute("orderModalNotesPlaceholder", setting("public.order_modal.notes_placeholder", "Ej: Prefiero entrega por la tarde"));
        model.addAttribute("orderModalSubmitLabel", setting("public.order_modal.submit_label", "Enviar por WhatsApp"));
        model.addAttribute("homeWhatsappIntro", setting("public.home.whatsapp_intro", "Hola, deseo hacer un pedido desde la web"));

        model.addAttribute("howTitle", setting("public.how.title", "¿Cómo funciona el servicio?"));
        model.addAttribute("howSubtitle", setting("public.how.subtitle", "Pedir tu agua Eco del Amazonas es muy sencillo. Solo sigue estos pasos."));
        for (int i = 1; i <= 3; i++) {
            model.addAttribute("howStep" + i + "Title", setting("public.how.step_" + i + ".title", defaultHowTitle(i)));
            model.addAttribute("howStep" + i + "Text", setting("public.how.step_" + i + ".text", defaultHowText(i)));
        }

        model.addAttribute("deliveryTitle", setting("public.delivery.title", "Zonas de reparto"));
        model.addAttribute("deliverySubtitle", setting("public.delivery.subtitle", "Consulta si ya llegamos a tu zona o coordina con nosotros por WhatsApp."));
        model.addAttribute("deliveryEmptyText", setting("public.delivery.empty_text", "Aún no se han configurado zonas de reparto. Consulta por WhatsApp."));
        model.addAttribute("deliveryExtraText", setting("public.delivery.extra_text", "También podemos atender empresas, oficinas, colegios y negocios de comida. Escríbenos y armamos un plan a tu medida."));
        model.addAttribute("deliveryCtaLabel", setting("public.delivery.cta_label", "Consultar mi zona »"));

        model.addAttribute("blogSectionTitle", setting("public.blog_section.title", "Blog & consejos de hidratación"));
        model.addAttribute("blogSectionSubtitle", setting("public.blog_section.subtitle", "Información simple y útil para cuidar tu salud y la de tu familia."));
        model.addAttribute("blogSectionEmptyText", setting("public.blog_section.empty_text", "Próximamente publicaremos consejos y artículos sobre hidratación."));

        model.addAttribute("testimonialsTitle", setting("public.testimonials.title", "Lo que dicen nuestros clientes"));
        model.addAttribute("testimonialsSubtitle", setting("public.testimonials.subtitle", "Nos esforzamos por brindar siempre un servicio amable, puntual y de calidad."));
        model.addAttribute("testimonialsEmptyText", setting("public.testimonials.empty_text", "Pronto compartiremos opiniones de nuestros clientes."));

        model.addAttribute("finalCtaTitle", setting("public.final_cta.title", "¿Listo para tu próxima recarga?"));
        model.addAttribute("finalCtaText", setting("public.final_cta.text", "Escríbenos por WhatsApp, cuéntanos cuántos bidones necesitas y coordinamos el horario. Nos encargamos del resto."));
        model.addAttribute("finalCtaButtonLabel", setting("public.final_cta.button_label", businessProperties.getFinalCtaButtonLabel()));
        model.addAttribute("finalCtaSchedule", setting("public.final_cta.schedule", businessProperties.getFinalCtaSchedule()));
    }

    private void addCatalogContentSettings(Model model) {
        model.addAttribute("catalogPageTitle", setting("public.catalog.title", "Catálogo de productos"));
        model.addAttribute("catalogSubtitle", setting("public.catalog.subtitle", "Selecciona el producto y envía tu pedido por WhatsApp. Un asesor confirmará el precio total y la hora de entrega."));
        model.addAttribute("catalogEmptyText", setting("public.catalog.empty_text", "No hay productos activos configurados todavía."));
        model.addAttribute("catalogOrderButtonLabel", setting("public.catalog.order_button_label", "Pedir por WhatsApp"));
        model.addAttribute("catalogDetailsButtonLabel", setting("public.catalog.details_button_label", "Ver detalles"));
        model.addAttribute("catalogProductPlaceholderImage", setting("public.catalog.product_placeholder_image", "/img/product-default.svg"));
        model.addAttribute("businessCatalogWhatsappIntro", setting("public.catalog.whatsapp_intro", businessProperties.getCatalogWhatsappIntro()));
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

        sb.append(setting("public.catalog.whatsapp_intro", businessProperties.getCatalogWhatsappIntro()))
                .append("\n\n");

        sb.append("👤 Tipo de cliente: ");
        if ("EXISTING".equalsIgnoreCase(customerType)) {
            sb.append("Ya soy cliente");
        } else if ("NEW".equalsIgnoreCase(customerType)) {
            sb.append("Cliente nuevo");
        } else {
            sb.append("No indicado");
        }
        sb.append("\n\n");

        sb.append("🧑 Datos del cliente:\n");
        sb.append("- Nombre: ").append(customerName).append("\n");
        sb.append("- Celular: ").append(customerPhone).append("\n");
        sb.append("- Dirección / referencia: ").append(customerAddress).append("\n\n");

        sb.append("🛒 Detalle del pedido:\n");
        sb.append("- Producto: ").append(product.getName()).append("\n");
        sb.append("- Cantidad: ").append(quantity).append("\n");
        sb.append("- Precio referencial: S/. ").append(product.getPrice()).append("\n\n");

        if (extraNotes != null && !extraNotes.isBlank()) {
            sb.append("📝 Notas:\n");
            sb.append(extraNotes.trim()).append("\n\n");
        }

        sb.append(setting("public.catalog.whatsapp_confirmation_text", "Por favor confirmar el total y el tiempo de entrega. Gracias."));

        return sb.toString();
    }

    private List<HeroCarouselSlide> buildHeroCarouselSlides(
            String heroMainImage,
            String heroMainImageAlt,
            String heroThumbImage1,
            String heroThumbImage2
    ) {
        List<HeroCarouselSlide> configuredSlides = new ArrayList<>();
        Set<String> configuredImages = new LinkedHashSet<>();

        for (int i = 1; i <= 10; i++) {
            String imagePath = setting("public.hero.carousel_image_" + i, "");
            addHeroCarouselSlide(configuredSlides, configuredImages, imagePath, heroMainImageAlt + " - imagen " + i);
        }

        if (!configuredSlides.isEmpty()) {
            return configuredSlides;
        }

        List<HeroCarouselSlide> fallbackSlides = new ArrayList<>();
        Set<String> fallbackImages = new LinkedHashSet<>();

        addHeroCarouselSlide(fallbackSlides, fallbackImages, heroMainImage, heroMainImageAlt);
        addHeroCarouselSlide(fallbackSlides, fallbackImages, heroThumbImage1, heroMainImageAlt + " - imagen secundaria 1");
        addHeroCarouselSlide(fallbackSlides, fallbackImages, heroThumbImage2, heroMainImageAlt + " - imagen secundaria 2");

        if (fallbackSlides.isEmpty()) {
            addHeroCarouselSlide(fallbackSlides, fallbackImages, "/img/banner02.jpg", heroMainImageAlt);
        }

        return fallbackSlides;
    }

    private void addHeroCarouselSlide(List<HeroCarouselSlide> slides, Set<String> images, String imagePath, String altText) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        String normalizedImagePath = imagePath.trim();
        if (!images.add(normalizedImagePath)) {
            return;
        }

        slides.add(new HeroCarouselSlide(normalizedImagePath, altText));
    }

    private static final class HeroCarouselSlide {
        private final String image;
        private final String alt;

        private HeroCarouselSlide(String image, String alt) {
            this.image = image;
            this.alt = alt;
        }

        public String getImage() {
            return image;
        }

        public String getAlt() {
            return alt;
        }
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

    private String defaultTrustIcon(int index) {
        return switch (index) {
            case 2 -> "bi-truck-front";
            case 3 -> "bi-whatsapp";
            case 4 -> "bi-shield-check";
            default -> "bi-droplet-half";
        };
    }

    private String defaultTrustTitle(int index) {
        return switch (index) {
            case 2 -> "Delivery puntual";
            case 3 -> "Pide por WhatsApp";
            case 4 -> "Empresa confiable";
            default -> "Agua purificada";
        };
    }

    private String defaultTrustText(int index) {
        return switch (index) {
            case 2 -> "Horarios de reparto programados.";
            case 3 -> "Resolvemos tus dudas en línea.";
            case 4 -> "Comprometidos con tu bienestar.";
            default -> "Tratamiento y filtrado profesional.";
        };
    }

    private String defaultHowTitle(int index) {
        return switch (index) {
            case 2 -> "Confirmamos el reparto";
            case 3 -> "Recibe y disfruta";
            default -> "Realiza tu pedido";
        };
    }

    private String defaultHowText(int index) {
        return switch (index) {
            case 2 -> "Nuestro equipo coordina contigo el día y franja horaria de entrega para que siempre tengas agua disponible.";
            case 3 -> "Llevamos los bidones hasta tu puerta. Pagas contra entrega y puedes programar tus próximas recargas.";
            default -> "Elige tus productos desde el catálogo o escríbenos por WhatsApp con tu dirección y horario preferido.";
        };
    }
}
