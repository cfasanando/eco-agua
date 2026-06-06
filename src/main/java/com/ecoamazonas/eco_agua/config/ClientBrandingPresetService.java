package com.ecoamazonas.eco_agua.config;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ClientBrandingPresetService {

    private final PlatformSettingRepository platformSettingRepository;
    private final PlatformSettingService platformSettingService;

    public ClientBrandingPresetService(PlatformSettingRepository platformSettingRepository,
                                       PlatformSettingService platformSettingService) {
        this.platformSettingRepository = platformSettingRepository;
        this.platformSettingService = platformSettingService;
    }

    public void applyPreset(String presetKey) {
        String normalizedPreset = normalizePresetKey(presetKey);
        platformSettingService.ensureDefaultsForPublicSite();

        Map<String, String> values = switch (normalizedPreset) {
            case "aguaeco" -> aguaEcoValues();
            case "belen" -> belenValues();
            default -> throw new IllegalArgumentException("Unsupported branding preset: " + presetKey);
        };

        values.forEach(this::saveSettingValue);
    }

    public String presetLabel(String presetKey) {
        return switch (normalizePresetKey(presetKey)) {
            case "aguaeco" -> "Agua Eco";
            case "belen" -> "Productos de la Selva Belén";
            default -> "Preset desconocido";
        };
    }

    private void saveSettingValue(String variable, String value) {
        PlatformSetting setting = platformSettingService.ensure(
                variable,
                value,
                typeFor(variable),
                categoryFor(variable),
                descriptionFor(variable)
        );
        setting.setValue(BrandingTextSanitizer.clean(value));
        platformSettingRepository.save(setting);
    }

    private String normalizePresetKey(String presetKey) {
        if (presetKey == null || presetKey.isBlank()) {
            throw new IllegalArgumentException("Branding preset is required.");
        }

        String normalized = presetKey.trim().toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "");

        if (!"aguaeco".equals(normalized) && !"belen".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported branding preset: " + presetKey);
        }

        return normalized;
    }

    private Map<String, String> aguaEcoValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("platform.name", "Agua Eco del Amazonas");
        values.put("platform.tagline", "Agua de mesa purificada en Iquitos");
        values.put("platform.logo", "/img/logo3-transparente.png");
        values.put("admin.brand.title", "Agua Eco del Amazonas");
        values.put("admin.brand.subtitle", "Sistema integral de agua de mesa");
        values.put("admin.brand.logo", "/img/logo-eco.png");
        values.put("public.whatsapp.number", "51980542101");
        values.put("public.topbar.location", "Reparto en Iquitos y alrededores");
        values.put("public.topbar.whatsapp_label", "Pedidos por WhatsApp");
        values.put("public.footer.right", "Servicio de agua purificada a domicilio - Iquitos, Perú");
        values.put("public.hero.pill", "Agua purificada, segura y confiable");
        values.put("public.hero.title", "Refresca tu vida con agua de mesa Eco del Amazonas");
        values.put("public.hero.subtitle", "Llevamos bidones y botellas de agua purificada hasta tu hogar u oficina, con reparto programado y atención personalizada.");
        values.put("public.hero.bullet_1", "Proceso de purificación controlado.");
        values.put("public.hero.bullet_2", "Entrega puntual en los horarios acordados.");
        values.put("public.hero.bullet_3", "Atención por WhatsApp, fácil y rápido.");
        values.put("public.hero.primary_cta_label", "Pedir ahora por WhatsApp");
        values.put("public.hero.secondary_cta_label", "Ver catálogo de productos");
        values.put("public.final_cta.title", "¿Listo para tu próxima recarga?");
        values.put("public.final_cta.text", "Escríbenos por WhatsApp, cuéntanos cuántos bidones necesitas y coordinamos el horario. Nos encargamos del resto.");
        values.put("public.final_cta.button_label", "Pedir recarga ahora");
        values.put("business.label.product", "Producto");
        values.put("business.label.product_plural", "Productos");
        values.put("business.label.customer", "Cliente");
        values.put("business.label.customer_plural", "Clientes");
        values.put("business.label.supplier", "Proveedor");
        values.put("business.label.supplier_plural", "Proveedores");
        values.put("business.label.supply", "Insumo");
        values.put("business.label.supply_plural", "Insumos");
        values.put("business.label.delivery_person", "Repartidor");
        values.put("business.label.delivery", "Reparto");
        values.put("business.label.container", "Envase");
        values.put("business.label.container_plural", "Envases");
        values.put("business.label.production", "Producción");
        values.put("business.label.reorder", "Reposición");
        return values;
    }

    private Map<String, String> belenValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("platform.name", "Productos de la Selva Belén");
        values.put("platform.tagline", "Productos amazónicos para familias de la selva en Lima");
        values.put("platform.logo", "/img/logo3-transparente.png");
        values.put("admin.brand.title", "Productos de la Selva Belén");
        values.put("admin.brand.subtitle", "Catálogo, pedidos y control del negocio");
        values.put("admin.brand.logo", "/img/logo-eco.png");
        values.put("public.whatsapp.number", "51928527493");
        values.put("public.topbar.location", "Atención y envíos coordinados por WhatsApp");
        values.put("public.topbar.whatsapp_label", "Pedidos por WhatsApp");
        values.put("public.footer.right", "Productos amazónicos seleccionados para familias de la selva.");
        values.put("public.hero.pill", "Sabores y productos de la Amazonía");
        values.put("public.hero.title", "Productos de la Selva Belén para quienes extrañan su tierra");
        values.put("public.hero.subtitle", "Consulta por pescados, paiche seco, hojas de bijao, hojas de plátano y productos disponibles para tus comidas amazónicas.");
        values.put("public.hero.bullet_1", "Productos reales y disponibilidad confirmada.");
        values.put("public.hero.bullet_2", "Atención directa por WhatsApp.");
        values.put("public.hero.bullet_3", "Ideal para familias de la selva que viven en Lima.");
        values.put("public.hero.primary_cta_label", "Consultar disponibilidad");
        values.put("public.hero.secondary_cta_label", "Ver catálogo");
        values.put("public.final_cta.title", "¿Quieres consultar productos disponibles?");
        values.put("public.final_cta.text", "Escríbenos por WhatsApp y coordinamos disponibilidad, cantidades y entrega según el producto.");
        values.put("public.final_cta.button_label", "Consultar por WhatsApp");
        values.put("business.label.product", "Producto");
        values.put("business.label.product_plural", "Catálogo");
        values.put("business.label.customer", "Cliente");
        values.put("business.label.customer_plural", "Clientes");
        values.put("business.label.supplier", "Proveedor");
        values.put("business.label.supplier_plural", "Proveedores");
        values.put("business.label.supply", "Material");
        values.put("business.label.supply_plural", "Materiales");
        values.put("business.label.delivery_person", "Responsable");
        values.put("business.label.delivery", "Entrega");
        values.put("business.label.container", "Empaque");
        values.put("business.label.container_plural", "Empaques");
        values.put("business.label.production", "Preparación");
        values.put("business.label.reorder", "Seguimiento");
        return values;
    }

    private String typeFor(String variable) {
        return variable.contains("logo") || variable.contains("image") ? "image" : "string";
    }

    private String categoryFor(String variable) {
        if (variable.startsWith("business.label.")) {
            return "platform";
        }
        return variable.startsWith("platform.") ? "platform" : "public_site";
    }

    private String descriptionFor(String variable) {
        return switch (variable) {
            case "platform.name" -> "Nombre comercial activo del cliente";
            case "platform.tagline" -> "Lema comercial activo del cliente";
            case "platform.logo" -> "Logo público activo del cliente";
            case "admin.brand.title" -> "Nombre mostrado en el menú lateral del sistema";
            case "admin.brand.subtitle" -> "Subtítulo mostrado en el menú lateral del sistema";
            case "admin.brand.logo" -> "Logo mostrado en el menú lateral del sistema";
            default -> "Texto configurable por cliente";
        };
    }
}
