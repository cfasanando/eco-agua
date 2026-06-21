package com.ecoamazonas.eco_agua.restaurant;

import com.ecoamazonas.eco_agua.config.BrandingTextSanitizer;
import com.ecoamazonas.eco_agua.config.BusinessProperties;
import com.ecoamazonas.eco_agua.config.PlatformSetting;
import com.ecoamazonas.eco_agua.config.PlatformSettingRepository;
import com.ecoamazonas.eco_agua.config.PlatformSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RestaurantSettingsService {

    public static final String CATEGORY = "restaurant";

    private final PlatformSettingService platformSettingService;
    private final PlatformSettingRepository platformSettingRepository;
    private final BusinessProperties businessProperties;
    private volatile boolean defaultsEnsured;

    public RestaurantSettingsService(PlatformSettingService platformSettingService,
                                     PlatformSettingRepository platformSettingRepository,
                                     BusinessProperties businessProperties) {
        this.platformSettingService = platformSettingService;
        this.platformSettingRepository = platformSettingRepository;
        this.businessProperties = businessProperties;
    }

    @Transactional
    public void ensureDefaults() {
        if (defaultsEnsured) {
            return;
        }
        synchronized (this) {
            if (defaultsEnsured) {
                return;
            }
            ensure("restaurant.identity.trade_name", businessProperties.getName(), "string", "Nombre comercial del restaurante");
            ensure("restaurant.identity.legal_name", businessProperties.getName(), "string", "Razón social impresa en cuentas y recibos");
            ensure("restaurant.identity.ruc", "", "string", "RUC del restaurante");
            ensure("restaurant.identity.address", businessProperties.getLocation(), "string", "Dirección comercial");
            ensure("restaurant.identity.phone", businessProperties.getPhone(), "string", "Teléfono del restaurante");
            ensure("restaurant.identity.whatsapp", businessProperties.getWhatsappNumber(), "string", "WhatsApp del restaurante");
            ensure("restaurant.identity.logo", businessProperties.getLogo(), "image", "Ruta del logo del restaurante");

            ensure("restaurant.currency.symbol", "S/", "string", "Símbolo de moneda usado en Restaurante");
            ensure("restaurant.tax.igv_enabled", "false", "boolean", "Mostrar desglose de IGV incluido");
            ensure("restaurant.tax.igv_rate", "18.00", "decimal", "Porcentaje de IGV incluido");
            ensure("restaurant.service_charge.enabled", "false", "boolean", "Aplicar cargo por servicio a pedidos en mesa");
            ensure("restaurant.service_charge.rate", "10.00", "decimal", "Porcentaje de cargo por servicio para pedidos en mesa");

            ensure("restaurant.order.prefix", "CMD", "string", "Prefijo de comandas y pedidos");
            ensure("restaurant.preparation.minutes", "30", "integer", "Tiempo estimado de preparación en minutos");
            ensure("restaurant.hours.open", "09:00", "time", "Hora de apertura");
            ensure("restaurant.hours.close", "23:00", "time", "Hora de cierre");
            ensure("restaurant.public.welcome_message", "Explora nuestros platos, bebidas y promociones. Desde tu mesa puedes solicitar atención o enviar un pedido para aprobación.", "text", "Mensaje principal de la carta QR");
            ensure("restaurant.receipt.footer", "Gracias por tu preferencia.", "text", "Texto al pie de la cuenta y el recibo");

            ensure("restaurant.qr_orders.enabled", "true", "boolean", "Permitir pedidos desde la carta QR");
            ensure("restaurant.table_requests.enabled", "true", "boolean", "Permitir solicitudes de atención desde mesa");
            ensure("restaurant.takeaway.enabled", "true", "boolean", "Habilitar pedidos para llevar");
            ensure("restaurant.delivery.enabled", "true", "boolean", "Habilitar pedidos delivery");
            ensure("restaurant.delivery.default_fee", "0.00", "decimal", "Costo de delivery predeterminado");
            ensure("restaurant.qr.max_items", "20", "integer", "Máximo de platos distintos por pedido QR");
            ensure("restaurant.qr.max_quantity_per_item", "10", "integer", "Cantidad máxima por plato en un pedido QR");
            ensure("restaurant.ticket.show_logo", "true", "boolean", "Mostrar logo en cuenta, ticket y recibo");

            defaultsEnsured = true;
        }
    }

    public RestaurantSettings getSettings() {
        ensureDefaults();
        Map<String, String> values = new HashMap<>();
        for (PlatformSetting setting : platformSettingRepository
                .findByCategoryInOrderByCategoryAscVariableAsc(List.of(CATEGORY))) {
            values.put(setting.getVariable(), BrandingTextSanitizer.clean(setting.getValue()));
        }
        return new RestaurantSettings(
                get(values, "restaurant.identity.trade_name", businessProperties.getName()),
                get(values, "restaurant.identity.legal_name", businessProperties.getName()),
                get(values, "restaurant.identity.ruc", ""),
                get(values, "restaurant.identity.address", businessProperties.getLocation()),
                get(values, "restaurant.identity.phone", businessProperties.getPhone()),
                get(values, "restaurant.identity.whatsapp", businessProperties.getWhatsappNumber()),
                get(values, "restaurant.identity.logo", businessProperties.getLogo()),
                get(values, "restaurant.currency.symbol", "S/"),
                getBoolean(values, "restaurant.tax.igv_enabled", false),
                getDecimal(values, "restaurant.tax.igv_rate", new BigDecimal("18.00")),
                getBoolean(values, "restaurant.service_charge.enabled", false),
                getDecimal(values, "restaurant.service_charge.rate", new BigDecimal("10.00")),
                get(values, "restaurant.order.prefix", "CMD"),
                getInt(values, "restaurant.preparation.minutes", 30),
                get(values, "restaurant.hours.open", "09:00"),
                get(values, "restaurant.hours.close", "23:00"),
                get(values, "restaurant.public.welcome_message", "Explora nuestros platos, bebidas y promociones."),
                get(values, "restaurant.receipt.footer", "Gracias por tu preferencia."),
                getBoolean(values, "restaurant.qr_orders.enabled", true),
                getBoolean(values, "restaurant.table_requests.enabled", true),
                getBoolean(values, "restaurant.takeaway.enabled", true),
                getBoolean(values, "restaurant.delivery.enabled", true),
                getDecimal(values, "restaurant.delivery.default_fee", BigDecimal.ZERO),
                getInt(values, "restaurant.qr.max_items", 20),
                getInt(values, "restaurant.qr.max_quantity_per_item", 10),
                getBoolean(values, "restaurant.ticket.show_logo", true)
        );
    }

    @Transactional
    public void update(Map<String, String> values) {
        ensureDefaults();

        String tradeName = required(values.get("tradeName"), "Ingresa el nombre comercial.", 180);
        String legalName = optional(values.get("legalName"), 180);
        String ruc = optional(values.get("ruc"), 20);
        String address = optional(values.get("address"), 255);
        String phone = optional(values.get("phone"), 40);
        String whatsapp = optional(values.get("whatsapp"), 40).replaceAll("[^0-9+]", "");
        String logoPath = optional(values.get("logoPath"), 255);
        String currencySymbol = required(values.get("currencySymbol"), "Ingresa el símbolo de moneda.", 8);
        String orderPrefix = normalizePrefix(values.get("orderPrefix"));
        int preparationMinutes = boundedInt(values.get("preparationMinutes"), 30, 0, 1440, "El tiempo de preparación debe estar entre 0 y 1440 minutos.");
        String openingTime = validTime(values.get("openingTime"), "09:00");
        String closingTime = validTime(values.get("closingTime"), "23:00");
        String publicWelcomeMessage = optional(values.get("publicWelcomeMessage"), 1000);
        String receiptFooter = optional(values.get("receiptFooter"), 1000);
        BigDecimal igvRate = boundedDecimal(values.get("igvRate"), new BigDecimal("18.00"), BigDecimal.ZERO, new BigDecimal("100.00"), "La tasa de IGV debe estar entre 0 y 100.");
        BigDecimal serviceChargeRate = boundedDecimal(values.get("serviceChargeRate"), new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("100.00"), "El cargo por servicio debe estar entre 0 y 100.");
        BigDecimal defaultDeliveryFee = boundedDecimal(values.get("defaultDeliveryFee"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("99999.99"), "El costo de delivery no puede ser negativo.");
        int qrMaxItems = boundedInt(values.get("qrMaxItems"), 20, 1, 100, "El máximo de productos QR debe estar entre 1 y 100.");
        int qrMaxQuantityPerItem = boundedInt(values.get("qrMaxQuantityPerItem"), 10, 1, 99, "La cantidad máxima por plato debe estar entre 1 y 99.");

        set("restaurant.identity.trade_name", tradeName);
        set("restaurant.identity.legal_name", legalName.isBlank() ? tradeName : legalName);
        set("restaurant.identity.ruc", ruc);
        set("restaurant.identity.address", address);
        set("restaurant.identity.phone", phone);
        set("restaurant.identity.whatsapp", whatsapp);
        set("restaurant.identity.logo", logoPath);
        set("restaurant.currency.symbol", currencySymbol);
        set("restaurant.tax.igv_enabled", booleanValue(values, "igvEnabled"));
        set("restaurant.tax.igv_rate", decimalText(igvRate));
        set("restaurant.service_charge.enabled", booleanValue(values, "serviceChargeEnabled"));
        set("restaurant.service_charge.rate", decimalText(serviceChargeRate));
        set("restaurant.order.prefix", orderPrefix);
        set("restaurant.preparation.minutes", Integer.toString(preparationMinutes));
        set("restaurant.hours.open", openingTime);
        set("restaurant.hours.close", closingTime);
        set("restaurant.public.welcome_message", publicWelcomeMessage);
        set("restaurant.receipt.footer", receiptFooter);
        set("restaurant.qr_orders.enabled", booleanValue(values, "qrOrdersEnabled"));
        set("restaurant.table_requests.enabled", booleanValue(values, "tableRequestsEnabled"));
        set("restaurant.takeaway.enabled", booleanValue(values, "takeawayEnabled"));
        set("restaurant.delivery.enabled", booleanValue(values, "deliveryEnabled"));
        set("restaurant.delivery.default_fee", decimalText(defaultDeliveryFee));
        set("restaurant.qr.max_items", Integer.toString(qrMaxItems));
        set("restaurant.qr.max_quantity_per_item", Integer.toString(qrMaxQuantityPerItem));
        set("restaurant.ticket.show_logo", booleanValue(values, "ticketShowLogo"));
    }

    public BigDecimal calculateServiceCharge(BigDecimal subtotal, String serviceType) {
        RestaurantSettings settings = getSettings();
        BigDecimal safeSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal.max(BigDecimal.ZERO);
        if (!settings.applyServiceChargeTo(serviceType)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return safeSubtotal
                .multiply(settings.safeServiceChargeRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public void assertQrOrderingEnabled() {
        if (!getSettings().qrOrdersEnabled()) {
            throw new IllegalArgumentException("Los pedidos desde QR están desactivados temporalmente.");
        }
    }

    public void assertTableRequestsEnabled() {
        if (!getSettings().tableRequestsEnabled()) {
            throw new IllegalArgumentException("Las solicitudes desde mesa están desactivadas temporalmente.");
        }
    }

    public void assertExternalServiceEnabled(String serviceType) {
        RestaurantSettings settings = getSettings();
        if ("DELIVERY".equalsIgnoreCase(serviceType) && !settings.deliveryEnabled()) {
            throw new IllegalArgumentException("El servicio delivery está desactivado.");
        }
        if ("TAKEAWAY".equalsIgnoreCase(serviceType) && !settings.takeawayEnabled()) {
            throw new IllegalArgumentException("El servicio para llevar está desactivado.");
        }
    }

    public void validateQrQuantities(Map<Long, Integer> quantities) {
        RestaurantSettings settings = getSettings();
        if (quantities.size() > settings.qrMaxItems()) {
            throw new IllegalArgumentException("El pedido QR supera el máximo de " + settings.qrMaxItems() + " platos distintos.");
        }
        for (Integer quantity : quantities.values()) {
            if (quantity != null && quantity > settings.qrMaxQuantityPerItem()) {
                throw new IllegalArgumentException("La cantidad máxima por plato en un pedido QR es " + settings.qrMaxQuantityPerItem() + ".");
            }
        }
    }

    private void ensure(String variable, String defaultValue, String type, String description) {
        platformSettingService.ensure(variable, defaultValue == null ? "" : defaultValue, type, CATEGORY, description);
    }

    private String get(Map<String, String> values, String variable, String defaultValue) {
        String value = values.get(variable);
        return value == null || value.isBlank() ? (defaultValue == null ? "" : defaultValue) : value;
    }

    private boolean getBoolean(Map<String, String> values, String variable, boolean defaultValue) {
        return Boolean.parseBoolean(get(values, variable, Boolean.toString(defaultValue)));
    }

    private BigDecimal getDecimal(Map<String, String> values, String variable, BigDecimal defaultValue) {
        try {
            return new BigDecimal(get(values, variable, decimalText(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private int getInt(Map<String, String> values, String variable, int defaultValue) {
        try {
            return Integer.parseInt(get(values, variable, Integer.toString(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private void set(String variable, String value) {
        PlatformSetting setting = platformSettingRepository.findByVariable(variable)
                .orElseThrow(() -> new IllegalStateException("Missing restaurant setting: " + variable));
        setting.setValue(BrandingTextSanitizer.clean(value == null ? "" : value.trim()));
        platformSettingRepository.save(setting);
    }

    private String booleanValue(Map<String, String> values, String key) {
        return Boolean.toString(values.containsKey(key));
    }

    private String required(String value, String message, int maxLength) {
        String clean = optional(value, maxLength);
        if (clean.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return clean;
    }

    private String optional(String value, int maxLength) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() > maxLength) {
            return clean.substring(0, maxLength);
        }
        return clean;
    }

    private String normalizePrefix(String value) {
        String clean = optional(value, 12).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "");
        if (clean.length() < 2) {
            throw new IllegalArgumentException("El prefijo de comandas debe tener al menos 2 caracteres alfanuméricos.");
        }
        return clean;
    }

    private String validTime(String value, String fallback) {
        String clean = value == null || value.isBlank() ? fallback : value.trim();
        try {
            return LocalTime.parse(clean).toString();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("El horario debe usar el formato HH:mm.");
        }
    }

    private int boundedInt(String value, int fallback, int min, int max, String errorMessage) {
        int parsed;
        try {
            parsed = value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException(errorMessage);
        }
        return parsed;
    }

    private BigDecimal boundedDecimal(String value,
                                      BigDecimal fallback,
                                      BigDecimal min,
                                      BigDecimal max,
                                      String errorMessage) {
        BigDecimal parsed;
        try {
            parsed = value == null || value.isBlank() ? fallback : new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
        if (parsed.compareTo(min) < 0 || parsed.compareTo(max) > 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return parsed.setScale(2, RoundingMode.HALF_UP);
    }

    private String decimalText(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
