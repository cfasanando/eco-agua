package com.ecoamazonas.eco_agua.config;

import org.springframework.stereotype.Service;

@Service
public class PlatformSettingService {

    private final PlatformSettingRepository platformSettingRepository;
    private final BusinessProperties businessProperties;

    public PlatformSettingService(PlatformSettingRepository platformSettingRepository,
                                  BusinessProperties businessProperties) {
        this.platformSettingRepository = platformSettingRepository;
        this.businessProperties = businessProperties;
    }

    public String get(String variable, String defaultValue) {
        return platformSettingRepository.findByVariable(variable)
                .map(PlatformSetting::getValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }

    /**
     * Ensure a setting exists; if not, create it with given defaults.
     */
    public PlatformSetting ensure(
            String variable,
            String defaultValue,
            String type,
            String category,
            String description
    ) {
        return platformSettingRepository.findByVariable(variable).orElseGet(() -> {
            PlatformSetting setting = new PlatformSetting();
            setting.setVariable(variable);
            setting.setValue(defaultValue);
            setting.setType(type);
            setting.setCategory(category);
            setting.setDescription(description);
            return platformSettingRepository.save(setting);
        });
    }

    /**
     * Ensure all settings used by the public home exist so they appear
     * in the admin configuration screen.
     */
    public void ensureDefaultsForPublicSite() {
        // Plataforma
        ensure("platform.name", businessProperties.getName(),
                "string", "platform", "Nombre comercial mostrado en la web");
        ensure("platform.tagline", businessProperties.getTagline(),
                "string", "platform", "Lema debajo del nombre");
        ensure("platform.logo", businessProperties.getLogo(),
                "string", "public_site", "Ruta del logo público");

        // Top bar
        ensure("public.topbar.location", businessProperties.getLocation(),
                "string", "public_site", "Texto de ubicación en la barra superior");
        ensure("public.topbar.phone", businessProperties.getPhone(),
                "string", "public_site", "Número de central en el top bar");
        ensure("public.topbar.phone_label", "Central:",
                "string", "public_site", "Etiqueta del número de central en el top bar");
        ensure("public.topbar.whatsapp_label", businessProperties.getTopbarWhatsappLabel(),
                "string", "public_site", "Texto del enlace de WhatsApp en el top bar");

        // WhatsApp / footer
        ensure("public.whatsapp.number", businessProperties.getWhatsappNumber(),
                "string", "public_site", "Número para pedidos por WhatsApp");
        ensure("public.footer.right",
                businessProperties.getFooterRight(),
                "string", "public_site", "Texto footer derecha");

        // Hero
        ensure("public.hero.pill", businessProperties.getHeroPill(),
                "string", "public_site", "Texto del pill en el hero");
        ensure("public.hero.title", businessProperties.getHeroTitle(),
                "string", "public_site", "Título principal del hero");
        ensure("public.hero.subtitle",
                businessProperties.getHeroSubtitle(),
                "text", "public_site", "Subtítulo del hero");

        ensure("public.hero.bullet_1", businessProperties.getHeroBullet1(),
                "string", "public_site", "Bullet 1 del hero");
        ensure("public.hero.bullet_2", businessProperties.getHeroBullet2(),
                "string", "public_site", "Bullet 2 del hero");
        ensure("public.hero.bullet_3", businessProperties.getHeroBullet3(),
                "string", "public_site", "Bullet 3 del hero");

        ensure("public.hero.primary_cta_label", businessProperties.getHeroPrimaryCtaLabel(),
                "string", "public_site", "Texto del botón principal del hero");
        ensure("public.hero.secondary_cta_label", businessProperties.getHeroSecondaryCtaLabel(),
                "string", "public_site", "Texto del botón secundario del hero");

        ensure("public.hero.stat_1", businessProperties.getHeroStat1(),
                "string", "public_site", "Estadística 1 del hero");
        ensure("public.hero.stat_2", businessProperties.getHeroStat2(),
                "string", "public_site", "Estadística 2 del hero");
        ensure("public.hero.stat_3", businessProperties.getHeroStat3(),
                "string", "public_site", "Estadística 3 del hero");

        ensure("public.hero.card_title", businessProperties.getHeroCardTitle(),
                "string", "public_site", "Título de la tarjeta del hero");
        ensure("public.hero.card_subtitle", businessProperties.getHeroCardSubtitle(),
                "string", "public_site", "Subtítulo de la tarjeta del hero");
        ensure("public.hero.badge_label", businessProperties.getHeroBadgeLabel(),
                "string", "public_site", "Texto de la etiqueta del hero");

        // Theme and navigation
        ensure("public.theme.primary_color", "#2e7d32",
                "string", "public_site", "Primary public theme color");
        ensure("public.theme.secondary_color", "#0277bd",
                "string", "public_site", "Secondary public theme color");
        ensure("public.nav.home_label", "Inicio",
                "string", "public_site", "Public navigation home label");
        ensure("public.nav.catalog_label", "Catálogo",
                "string", "public_site", "Public navigation catalog label");
        ensure("public.nav.blog_label", "Blog & consejos",
                "string", "public_site", "Public navigation blog label");
        ensure("public.nav.whatsapp_label", "Pedir por WhatsApp",
                "string", "public_site", "Public navigation WhatsApp label");
        ensure("public.nav.access_label", "Acceso colaboradores",
                "string", "public_site", "Public login access label");

        // Hero media
        ensure("public.hero.background_image", "/img/banner01.jpg",
                "string", "public_site", "Hero background image path");
        ensure("public.hero.main_image", "/img/banner02.jpg",
                "string", "public_site", "Hero main card image path");
        ensure("public.hero.main_image_alt", businessProperties.getName(),
                "string", "public_site", "Hero main image alternative text");
        ensure("public.hero.thumb_image_1", "/img/banner01.jpg",
                "string", "public_site", "Hero thumbnail image 1 path");
        ensure("public.hero.thumb_image_2", "/img/bienvenida.png",
                "string", "public_site", "Hero thumbnail image 2 path");

        for (int i = 1; i <= 10; i++) {
            ensure("public.hero.carousel_image_" + i, "",
                    "image", "public_site", "Imagen " + i + " del carrusel principal del portal público");
        }

        // Trust cards
        ensure("public.trust.1.icon", "bi-droplet-half", "string", "public_site", "Trust card 1 icon");
        ensure("public.trust.1.title", "Agua purificada", "string", "public_site", "Trust card 1 title");
        ensure("public.trust.1.text", "Tratamiento y filtrado profesional.", "string", "public_site", "Trust card 1 text");
        ensure("public.trust.2.icon", "bi-truck-front", "string", "public_site", "Trust card 2 icon");
        ensure("public.trust.2.title", "Delivery puntual", "string", "public_site", "Trust card 2 title");
        ensure("public.trust.2.text", "Horarios de reparto programados.", "string", "public_site", "Trust card 2 text");
        ensure("public.trust.3.icon", "bi-whatsapp", "string", "public_site", "Trust card 3 icon");
        ensure("public.trust.3.title", "Pide por WhatsApp", "string", "public_site", "Trust card 3 title");
        ensure("public.trust.3.text", "Resolvemos tus dudas en línea.", "string", "public_site", "Trust card 3 text");
        ensure("public.trust.4.icon", "bi-shield-check", "string", "public_site", "Trust card 4 icon");
        ensure("public.trust.4.title", "Empresa confiable", "string", "public_site", "Trust card 4 title");
        ensure("public.trust.4.text", "Comprometidos con tu bienestar.", "string", "public_site", "Trust card 4 text");

        // Public home sections
        ensure("public.promotions.title", "Promociones del mes", "string", "public_site", "Promotions section title");
        ensure("public.promotions.subtitle", "Aprovecha los precios especiales por volumen y recarga frecuente.", "string", "public_site", "Promotions section subtitle");
        ensure("public.promotions.empty_text", "No hay promociones activas por ahora.", "string", "public_site", "Promotions empty text");
        ensure("public.featured_products.title", "Productos destacados", "string", "public_site", "Featured products section title");
        ensure("public.featured_products.subtitle", "Los productos más pedidos por nuestros clientes.", "string", "public_site", "Featured products section subtitle");
        ensure("public.featured_products.link_label", "Ver catálogo completo »", "string", "public_site", "Featured products link label");
        ensure("public.featured_products.empty_text", "No hay productos destacados configurados.", "string", "public_site", "Featured products empty text");
        ensure("public.how.title", "¿Cómo funciona el servicio?", "string", "public_site", "How it works title");
        ensure("public.how.subtitle", "Pedir tu agua Eco del Amazonas es muy sencillo. Solo sigue estos pasos.", "string", "public_site", "How it works subtitle");
        ensure("public.how.step_1.title", "Realiza tu pedido", "string", "public_site", "How it works step 1 title");
        ensure("public.how.step_1.text", "Elige tus productos desde el catálogo o escríbenos por WhatsApp con tu dirección y horario preferido.", "text", "public_site", "How it works step 1 text");
        ensure("public.how.step_2.title", "Confirmamos el reparto", "string", "public_site", "How it works step 2 title");
        ensure("public.how.step_2.text", "Nuestro equipo coordina contigo el día y franja horaria de entrega para que siempre tengas agua disponible.", "text", "public_site", "How it works step 2 text");
        ensure("public.how.step_3.title", "Recibe y disfruta", "string", "public_site", "How it works step 3 title");
        ensure("public.how.step_3.text", "Llevamos los bidones hasta tu puerta. Pagas contra entrega y puedes programar tus próximas recargas.", "text", "public_site", "How it works step 3 text");
        ensure("public.delivery.title", "Zonas de reparto", "string", "public_site", "Delivery section title");
        ensure("public.delivery.subtitle", "Consulta si ya llegamos a tu zona o coordina con nosotros por WhatsApp.", "text", "public_site", "Delivery section subtitle");
        ensure("public.delivery.empty_text", "Aún no se han configurado zonas de reparto. Consulta por WhatsApp.", "text", "public_site", "Delivery empty text");
        ensure("public.delivery.extra_text", "También podemos atender empresas, oficinas, colegios y negocios de comida. Escríbenos y armamos un plan a tu medida.", "text", "public_site", "Delivery extra text");
        ensure("public.delivery.cta_label", "Consultar mi zona »", "string", "public_site", "Delivery CTA label");
        ensure("public.blog_section.title", "Blog & consejos de hidratación", "string", "public_site", "Home blog section title");
        ensure("public.blog_section.subtitle", "Información simple y útil para cuidar tu salud y la de tu familia.", "text", "public_site", "Home blog section subtitle");
        ensure("public.blog_section.empty_text", "Próximamente publicaremos consejos y artículos sobre hidratación.", "text", "public_site", "Home blog empty text");
        ensure("public.testimonials.title", "Lo que dicen nuestros clientes", "string", "public_site", "Testimonials title");
        ensure("public.testimonials.subtitle", "Nos esforzamos por brindar siempre un servicio amable, puntual y de calidad.", "text", "public_site", "Testimonials subtitle");
        ensure("public.testimonials.empty_text", "Pronto compartiremos opiniones de nuestros clientes.", "text", "public_site", "Testimonials empty text");

        // Order modal and catalog
        ensure("public.order_modal.title", "Pedido por WhatsApp", "string", "public_site", "Order modal title");
        ensure("public.order_modal.intro", "Para coordinar la entrega, por favor completa tus datos:", "text", "public_site", "Order modal intro");
        ensure("public.order_modal.quantity_label", "Cantidad", "string", "public_site", "Order modal quantity label");
        ensure("public.order_modal.address_label", "Dirección (incluye distrito y referencia)", "string", "public_site", "Order modal address label");
        ensure("public.order_modal.notes_label", "Notas para la entrega (opcional)", "string", "public_site", "Order modal notes label");
        ensure("public.order_modal.notes_placeholder", "Ej: Prefiero entrega por la tarde", "string", "public_site", "Order modal notes placeholder");
        ensure("public.order_modal.submit_label", "Enviar por WhatsApp", "string", "public_site", "Order modal submit label");
        ensure("public.home.whatsapp_intro", "Hola, deseo hacer un pedido desde la web", "text", "public_site", "Home WhatsApp intro message");
        ensure("public.catalog.title", "Catálogo de productos", "string", "public_site", "Catalog title");
        ensure("public.catalog.subtitle", "Selecciona el producto y envía tu pedido por WhatsApp. Un asesor confirmará el precio total y la hora de entrega.", "text", "public_site", "Catalog subtitle");
        ensure("public.catalog.empty_text", "No hay productos activos configurados todavía.", "text", "public_site", "Catalog empty text");
        ensure("public.catalog.order_button_label", "Pedir por WhatsApp", "string", "public_site", "Catalog order button label");
        ensure("public.catalog.details_button_label", "Ver detalles", "string", "public_site", "Catalog details button label");
        ensure("public.catalog.product_placeholder_image", "/img/product-default.svg", "string", "public_site", "Catalog product placeholder image");
        ensure("public.catalog.whatsapp_intro", businessProperties.getCatalogWhatsappIntro(), "text", "public_site", "Catalog WhatsApp intro message");
        ensure("public.catalog.whatsapp_confirmation_text", "Por favor confirmar el total y el tiempo de entrega. Gracias.", "text", "public_site", "Catalog WhatsApp confirmation message");

        // Blog public pages
        ensure("public.blog.page_title", "Blog & consejos", "string", "public_site", "Blog page title");
        ensure("public.blog.subtitle", "Ideas, tips y recomendaciones para nuestros clientes.", "text", "public_site", "Blog page subtitle");
        ensure("public.blog.bullet_1", "Consejos prácticos para el día a día.", "string", "public_site", "Blog bullet 1");
        ensure("public.blog.bullet_2", "Recomendaciones para familias y negocios.", "string", "public_site", "Blog bullet 2");
        ensure("public.blog.bullet_3", "Información útil para comprar mejor.", "string", "public_site", "Blog bullet 3");
        ensure("public.blog.hero_image", "/img/banner02.jpg", "string", "public_site", "Blog hero image path");
        ensure("public.blog.empty_text", "Próximamente publicaremos nuestros primeros artículos.", "text", "public_site", "Blog empty text");
        ensure("public.blog.about_title", "Sobre " + businessProperties.getName(), "string", "public_site", "Blog about title");
        ensure("public.blog.about_text_1", "Compartimos información útil para nuestros clientes.", "text", "public_site", "Blog about text 1");
        ensure("public.blog.about_text_2", "Nuestro objetivo es ayudarte a elegir mejor y coordinar tus pedidos con confianza.", "text", "public_site", "Blog about text 2");
        ensure("public.blog.cta_title", "¿Necesitas hacer un pedido?", "string", "public_site", "Blog CTA title");
        ensure("public.blog.cta_text", "Escríbenos por WhatsApp y coordinamos tu atención.", "text", "public_site", "Blog CTA text");
        ensure("public.blog.cta_button_label", "Pedir por WhatsApp", "string", "public_site", "Blog CTA button label");
        ensure("public.blog.latest_title", "Últimos artículos", "string", "public_site", "Blog latest title");
        ensure("public.blog.article_cta_text", "¿Te sirvió esta información? Escríbenos por WhatsApp para coordinar tu pedido.", "text", "public_site", "Blog article CTA text");
        ensure("public.blog.article_cta_button_label", "Pedir por WhatsApp", "string", "public_site", "Blog article CTA button label");
        ensure("public.blog.quick_tip_title", "Consejo rápido", "string", "public_site", "Blog quick tip title");
        ensure("public.blog.quick_tip_text", "Consulta siempre la disponibilidad antes de coordinar tu pedido.", "text", "public_site", "Blog quick tip text");

        // Bloque final
        ensure("public.final_cta.title", "¿Listo para tu próxima recarga?",
                "string", "public_site", "Título del bloque final");
        ensure("public.final_cta.text", "Escríbenos por WhatsApp, cuéntanos cuántos bidones necesitas y coordinamos el horario. Nos encargamos del resto.",
                "text", "public_site", "Texto del bloque final");
        ensure("public.final_cta.button_label", businessProperties.getFinalCtaButtonLabel(),
                "string", "public_site", "Texto del botón del bloque final");
        ensure("public.final_cta.schedule",
                businessProperties.getFinalCtaSchedule(),
                "string", "public_site", "Texto de horario del bloque final");
    }
}
