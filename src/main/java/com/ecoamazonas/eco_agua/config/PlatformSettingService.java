package com.ecoamazonas.eco_agua.config;

import org.springframework.stereotype.Service;

@Service
public class PlatformSettingService {

    private final PlatformSettingRepository platformSettingRepository;

    public PlatformSettingService(PlatformSettingRepository platformSettingRepository) {
        this.platformSettingRepository = platformSettingRepository;
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
        ensure("platform.name", "Eco del Amazonas",
                "string", "platform", "Nombre comercial mostrado en la web");
        ensure("platform.tagline", "Agua de mesa",
                "string", "platform", "Lema debajo del nombre");
        ensure("platform.logo", "/img/logo3-transparente.png",
                "string", "public_site", "Ruta del logo público");

        // Top bar
        ensure("public.topbar.location", "Reparto en Iquitos y alrededores",
                "string", "public_site", "Texto de ubicación en la barra superior");
        ensure("public.topbar.phone", "(065) 000000",
                "string", "public_site", "Número de central en el top bar");
        ensure("public.topbar.whatsapp_label", "Pedidos por WhatsApp",
                "string", "public_site", "Texto del enlace de WhatsApp en el top bar");

        // WhatsApp / footer
        ensure("public.whatsapp.number", "51980542101",
                "string", "public_site", "Número para pedidos por WhatsApp");
        ensure("public.footer.right",
                "Servicio de agua purificada a domicilio - Iquitos, Perú",
                "string", "public_site", "Texto footer derecha");

        // Hero
        ensure("public.hero.pill", "Agua purificada, segura y confiable",
                "string", "public_site", "Texto del pill en el hero");
        ensure("public.hero.title", "Refresca tu vida con agua de mesa Eco del Amazonas",
                "string", "public_site", "Título principal del hero");
        ensure("public.hero.subtitle",
                "Llevamos bidones y botellas de agua purificada hasta tu hogar u oficina, con reparto programado y atención personalizada.",
                "text", "public_site", "Subtítulo del hero");

        ensure("public.hero.bullet_1", "Proceso de purificación certificado.",
                "string", "public_site", "Bullet 1 del hero");
        ensure("public.hero.bullet_2", "Entrega puntual en los horarios acordados.",
                "string", "public_site", "Bullet 2 del hero");
        ensure("public.hero.bullet_3", "Atención por WhatsApp, fácil y rápido.",
                "string", "public_site", "Bullet 3 del hero");

        ensure("public.hero.primary_cta_label", "Pedir ahora por WhatsApp",
                "string", "public_site", "Texto del botón principal del hero");
        ensure("public.hero.secondary_cta_label", "Ver catálogo de productos",
                "string", "public_site", "Texto del botón secundario del hero");

        ensure("public.hero.stat_1", "+500 familias atendidas",
                "string", "public_site", "Estadística 1 del hero");
        ensure("public.hero.stat_2", "Reparto diario en Iquitos",
                "string", "public_site", "Estadística 2 del hero");
        ensure("public.hero.stat_3", "Calidad y confianza",
                "string", "public_site", "Estadística 3 del hero");

        ensure("public.hero.card_title", "Agua de mesa Eco del Amazonas",
                "string", "public_site", "Título de la tarjeta del hero");
        ensure("public.hero.card_subtitle", "Bidones, botellas y planes para empresas",
                "string", "public_site", "Subtítulo de la tarjeta del hero");
        ensure("public.hero.badge_label", "Servicio destacado",
                "string", "public_site", "Texto de la etiqueta del hero");

        // Bloque final
        ensure("public.final_cta.button_label", "Pedir recarga ahora",
                "string", "public_site", "Texto del botón del bloque final");
        ensure("public.final_cta.schedule",
                "Atención de lunes a sábado, de 8:00 a.m. a 8:00 p.m.",
                "string", "public_site", "Texto de horario del bloque final");
    }
}
