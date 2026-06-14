package com.ecoamazonas.eco_agua.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PlatformRuntimeService {

    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformClientModuleRepository clientModuleRepository;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String runtimeClientsDirectory;

    public PlatformRuntimeService(PlatformBusinessClientRepository clientRepository,
                                  PlatformClientModuleRepository clientModuleRepository,
                                  @Value("${spring.datasource.username:root}") String datasourceUsername,
                                  @Value("${spring.datasource.password:}") String datasourcePassword,
                                  @Value("${ecoagua.platform.runtime-clients-dir:runtime-clients}") String runtimeClientsDirectory) {
        this.clientRepository = clientRepository;
        this.clientModuleRepository = clientModuleRepository;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.runtimeClientsDirectory = runtimeClientsDirectory;
    }

    public PlatformRuntimePlan buildPlan(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        String profile = runtimeProfile(client);
        int port = runtimePort(client);
        String localUrl = "http://localhost:" + port;
        String publicUrl = defaultValue(client.getPublicUrl(), localUrl);
        List<String> commands = runCommands(profile, port);
        boolean databaseReady = "READY".equalsIgnoreCase(valueOrEmpty(client.getDatabaseStatus()))
                || "CREATED".equalsIgnoreCase(valueOrEmpty(client.getDatabaseStatus()));
        boolean active = "ACTIVE".equalsIgnoreCase(valueOrEmpty(client.getStatus()));
        boolean runtimeConfigured = client.getRuntimeProfile() != null && !client.getRuntimeProfile().isBlank()
                && client.getRuntimePort() != null;

        return new PlatformRuntimePlan(
                client,
                profile,
                port,
                localUrl,
                publicUrl,
                "application-" + profile + ".properties",
                "run-" + profile + ".sh",
                applicationProperties(client, profile, port, publicUrl),
                runScript(profile, port),
                commands,
                String.join(System.lineSeparator(), commands),
                databaseReady,
                active,
                runtimeConfigured,
                statusTitle(databaseReady, active, runtimeConfigured),
                statusDescription(databaseReady, active, runtimeConfigured, localUrl),
                databaseReady && active && runtimeConfigured ? "alert-success" : "alert-warning"
        );
    }

    @Transactional
    public void saveRuntimeSettings(Long clientId, String runtimeProfile, Integer runtimePort, String publicUrl) {
        PlatformBusinessClient client = getClient(clientId);
        String profile = normalizeProfile(defaultValue(runtimeProfile, client.getCode()));
        int port = runtimePort == null ? suggestedPort(client) : runtimePort;
        validatePort(port);

        client.setRuntimeProfile(profile);
        client.setRuntimePort(port);
        client.setPublicUrl(defaultValue(publicUrl, "http://localhost:" + port));
        client.setRuntimeStatus("CONFIGURED");
        client.setLastRuntimeGeneratedAt(LocalDateTime.now());
        clientRepository.save(client);
    }

    private PlatformBusinessClient getClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Business client not found."));
    }

    private String runtimeProfile(PlatformBusinessClient client) {
        return normalizeProfile(defaultValue(client.getRuntimeProfile(), client.getCode()));
    }

    private int runtimePort(PlatformBusinessClient client) {
        if (client.getRuntimePort() != null && client.getRuntimePort() > 0) {
            return client.getRuntimePort();
        }
        return suggestedPort(client);
    }

    private int suggestedPort(PlatformBusinessClient client) {
        long id = client.getId() == null ? 1L : client.getId();
        int offset = (int) Math.min(Math.max(id, 1L), 500L);
        return 8081 + offset;
    }

    private void validatePort(int port) {
        if (port < 1024 || port > 65535) {
            throw new IllegalArgumentException("El puerto debe estar entre 1024 y 65535.");
        }
    }

    private List<String> runCommands(String profile, int port) {
        return List.of(
                "bash scripts/run-client.sh " + profile + " " + port,
                "bash " + runtimeClientsDirectory + "/" + profile + "/run.sh"
        );
    }

    private String runScript(String profile, int port) {
        return """
                #!/usr/bin/env bash
                set -euo pipefail

                SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
                PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
                CONFIG_FILE="$SCRIPT_DIR/application.properties"

                if [[ ! -f "$CONFIG_FILE" ]]; then
                  echo "[ERROR] Runtime config not found: $CONFIG_FILE"
                  exit 1
                fi

                if command -v cygpath >/dev/null 2>&1; then
                  CONFIG_PATH="$(cygpath -m "$CONFIG_FILE")"
                else
                  CONFIG_PATH="$CONFIG_FILE"
                fi

                cd "$PROJECT_DIR"

                echo "[INFO] Starting client runtime from: $CONFIG_PATH"

                mvn spring-boot:run \
                  -Dspring-boot.run.arguments="--spring.config.additional-location=file:$CONFIG_PATH --server.port=%d"
                """.formatted(port).stripLeading();
    }

    private String applicationProperties(PlatformBusinessClient client, String profile, int port, String publicUrl) {
        String databaseName = normalizeDatabase(defaultValue(client.getDatabaseName(), profile));
        String businessName = defaultValue(client.getBusinessName(), profile);
        String shortName = businessName.length() > 30 ? businessName.substring(0, 30).trim() : businessName;
        String whatsapp = defaultValue(client.getWhatsapp(), "51988888888");
        String city = defaultValue(client.getCity(), "Iquitos");
        String primaryColor = defaultValue(client.getPrimaryColor(), "#0d6efd");
        String catalogLabel = catalogLabel(client);

        StringBuilder out = new StringBuilder();
        out.append("# Runtime profile generated from Super Admin for ").append(businessName).append("\n");
        out.append("# Stored outside src/main/resources to avoid touching source code.\n\n");
        out.append("server.port=").append(port).append("\n");
        out.append("spring.datasource.url=jdbc:mysql://localhost:3306/").append(databaseName).append("?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima\n");
        out.append("spring.datasource.username=").append(escapeProperty(datasourceUsername)).append("\n");
        out.append("spring.datasource.password=").append(escapeProperty(datasourcePassword)).append("\n");
        out.append("spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver\n\n");
        out.append("spring.jpa.hibernate.ddl-auto=none\n");
        out.append("spring.jpa.show-sql=true\n");
        out.append("spring.jpa.properties.hibernate.format_sql=true\n");
        out.append("spring.thymeleaf.cache=false\n");
        out.append("server.error.include-message=always\n");
        out.append("server.error.include-stacktrace=always\n\n");
        out.append("ecoagua.platform.client-code=").append(valueOrEmpty(client.getCode())).append("\n");
        out.append("ecoagua.platform.runtime-profile=").append(profile).append("\n");
        out.append("ecoagua.platform.public-url=").append(publicUrl).append("\n");
        out.append("ecoagua.whatsapp.number=").append(whatsapp).append("\n\n");
        out.append("ecoagua.business.profile-code=").append(profile).append("\n");
        out.append("ecoagua.business.name=").append(escapeProperty(businessName)).append("\n");
        out.append("ecoagua.business.short-name=").append(escapeProperty(shortName)).append("\n");
        out.append("ecoagua.business.tagline=").append(escapeProperty(taglineFor(client))).append("\n");
        out.append("ecoagua.business.type=").append(normalizeProfile(defaultValue(client.getBusinessType(), "business"))).append("\n");
        out.append("ecoagua.business.admin-title=Sistema integral\n");
        out.append("ecoagua.business.logo=").append(defaultValue(client.getLogoUrl(), "/img/logo3-transparente.png")).append("\n");
        out.append("ecoagua.business.admin-logo=").append(defaultValue(client.getLogoUrl(), "/img/logo-eco.png")).append("\n");
        out.append("ecoagua.business.whatsapp-number=").append(whatsapp).append("\n");
        out.append("ecoagua.business.location=").append(escapeProperty(city)).append("\n");
        out.append("ecoagua.business.phone=").append(escapeProperty(defaultValue(client.getContactPhone(), whatsapp))).append("\n");
        out.append("ecoagua.business.footer-right=").append(escapeProperty("Sistema modular para " + businessName)).append("\n");
        out.append("ecoagua.business.topbar-whatsapp-label=Consultas por WhatsApp\n");
        out.append("ecoagua.business.featured-category-name=").append(catalogLabel).append("\n");
        out.append("ecoagua.business.catalog-whatsapp-intro=Hola, deseo consultar desde el sistema de ").append(escapeProperty(businessName)).append("\n");
        out.append("ecoagua.business.hero-pill=").append(escapeProperty(heroPill(client))).append("\n");
        out.append("ecoagua.business.hero-title=").append(escapeProperty(heroTitle(client))).append("\n");
        out.append("ecoagua.business.hero-subtitle=").append(escapeProperty(heroSubtitle(client))).append("\n");
        out.append("ecoagua.business.hero-primary-cta-label=Consultar por WhatsApp\n");
        out.append("ecoagua.business.hero-secondary-cta-label=Ver ").append(catalogLabel.toLowerCase(Locale.ROOT)).append("\n");
        out.append("ecoagua.business.final-cta-button-label=Consultar ahora\n");
        out.append("ecoagua.business.final-cta-schedule=Atención según disponibilidad y coordinación del negocio.\n");
        out.append("ecoagua.business.product-label=").append(productLabel(client, false)).append("\n");
        out.append("ecoagua.business.product-plural-label=").append(productLabel(client, true)).append("\n");
        out.append("ecoagua.business.customer-label=Cliente\n");
        out.append("ecoagua.business.customer-plural-label=Clientes\n");
        out.append("ecoagua.business.supplier-label=Proveedor\n");
        out.append("ecoagua.business.supplier-plural-label=Proveedores\n");
        out.append("ecoagua.business.supply-label=Insumo\n");
        out.append("ecoagua.business.supply-plural-label=Insumos\n");
        out.append("ecoagua.business.delivery-person-label=Responsable de entrega\n");
        out.append("ecoagua.business.delivery-label=Entregas\n");
        out.append("ecoagua.business.container-label=Envase\n");
        out.append("ecoagua.business.container-plural-label=Envases\n");
        out.append("ecoagua.business.production-label=Producción\n");
        out.append("ecoagua.business.reorder-label=Seguimiento\n\n");
        out.append("# Feature flags generated from active modules\n");
        Map<String, Boolean> flags = featureFlags(client.getId());
        for (Map.Entry<String, Boolean> entry : flags.entrySet()) {
            out.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        out.append("\n# Theme\n");
        out.append("public.theme.primary_color=").append(primaryColor).append("\n");
        return out.toString();
    }

    private Map<String, Boolean> featureFlags(Long clientId) {
        List<String> keys = clientModuleRepository.findClientModules(clientId).stream()
                .filter(PlatformClientModule::isEnabled)
                .map(item -> item.getModule().getModuleKey())
                .toList();
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("ecoagua.features.containers", keys.contains("containers"));
        flags.put("ecoagua.features.delivery", keys.contains("delivery") || keys.contains("routes") || keys.contains("rutapack"));
        flags.put("ecoagua.features.production", keys.contains("production") || keys.contains("recipes") || keys.contains("kitchen"));
        flags.put("ecoagua.features.reorder", keys.contains("reorder") || keys.contains("followup"));
        flags.put("ecoagua.features.marketing", keys.contains("marketing"));
        flags.put("ecoagua.features.blog", keys.contains("blog") || keys.contains("content"));
        flags.put("ecoagua.features.testimonials", keys.contains("testimonials"));
        flags.put("ecoagua.features.public-catalog", keys.contains("public_catalog") || keys.contains("catalog") || keys.contains("ecommerce_filters") || keys.contains("qr_menu"));
        flags.put("ecoagua.features.supplies", keys.contains("supplies") || keys.contains("warehouse") || keys.contains("ingredients"));
        flags.put("ecoagua.features.fixed-costs", keys.contains("income") || keys.contains("finance") || keys.contains("accounting"));
        flags.put("ecoagua.features.break-even", keys.contains("income") || keys.contains("finance") || keys.contains("accounting"));
        flags.put("ecoagua.features.price-simulator", keys.contains("sales") || keys.contains("products") || keys.contains("public_catalog"));
        return flags;
    }

    private String statusTitle(boolean databaseReady, boolean active, boolean runtimeConfigured) {
        if (databaseReady && active && runtimeConfigured) {
            return "Perfil listo para ejecutar";
        }
        if (!databaseReady) {
            return "Primero termina el aprovisionamiento";
        }
        if (!active) {
            return "Falta activar el negocio";
        }
        return "Configura puerto y perfil";
    }

    private String statusDescription(boolean databaseReady, boolean active, boolean runtimeConfigured, String localUrl) {
        if (databaseReady && active && runtimeConfigured) {
            return "El negocio tiene perfil, puerto y URL local. Puedes descargar los archivos y levantarlo en " + localUrl + ".";
        }
        if (!databaseReady) {
            return "Antes de ejecutar una instancia independiente, crea la base, copia la estructura y aplica el bootstrap.";
        }
        if (!active) {
            return "La base ya está lista, pero el negocio todavía no está activo para demo o pruebas.";
        }
        return "Guarda el perfil de ejecución para generar application-{perfil}.properties y el script run-{perfil}.sh.";
    }

    private String normalizeProfile(String value) {
        String normalized = valueOrEmpty(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized.isBlank() ? "cliente_demo" : normalized;
    }

    private String normalizeDatabase(String value) {
        return normalizeProfile(value);
    }

    private String taglineFor(PlatformBusinessClient client) {
        String type = valueOrEmpty(client.getBusinessType()).toLowerCase(Locale.ROOT);
        if (type.contains("restaurante")) {
            return "Carta, pedidos y delivery para tu restaurante.";
        }
        if (type.contains("academia") || type.contains("curso")) {
            return "Cursos, alumnos y certificados en un solo lugar.";
        }
        if (type.contains("courier") || type.contains("ruta")) {
            return "Rutas, entregas y evidencias para tu operación diaria.";
        }
        if (type.contains("tienda") || type.contains("commerce")) {
            return "Catálogo, promociones y pedidos por WhatsApp.";
        }
        return "Sistema modular para gestionar tu negocio.";
    }

    private String catalogLabel(PlatformBusinessClient client) {
        String type = valueOrEmpty(client.getBusinessType()).toLowerCase(Locale.ROOT);
        if (type.contains("restaurante")) {
            return "Carta";
        }
        if (type.contains("academia") || type.contains("curso")) {
            return "Cursos";
        }
        return "Catálogo";
    }

    private String productLabel(PlatformBusinessClient client, boolean plural) {
        String type = valueOrEmpty(client.getBusinessType()).toLowerCase(Locale.ROOT);
        if (type.contains("restaurante")) {
            return plural ? "Platos" : "Plato";
        }
        if (type.contains("academia") || type.contains("curso")) {
            return plural ? "Cursos" : "Curso";
        }
        return plural ? "Productos" : "Producto";
    }

    private String heroPill(PlatformBusinessClient client) {
        String type = valueOrEmpty(client.getBusinessType()).toLowerCase(Locale.ROOT);
        if (type.contains("restaurante")) {
            return "Carta digital, pedidos y delivery";
        }
        if (type.contains("academia") || type.contains("curso")) {
            return "Cursos, alumnos y certificados";
        }
        if (type.contains("tienda") || type.contains("commerce")) {
            return "Catálogo comercial y promociones";
        }
        return "Sistema modular para negocios";
    }

    private String heroTitle(PlatformBusinessClient client) {
        String type = valueOrEmpty(client.getBusinessType()).toLowerCase(Locale.ROOT);
        String name = defaultValue(client.getBusinessName(), "tu negocio");
        if (type.contains("restaurante")) {
            return "Disfruta la carta de " + name;
        }
        if (type.contains("academia") || type.contains("curso")) {
            return "Aprende con " + name;
        }
        return "Bienvenido a " + name;
    }

    private String heroSubtitle(PlatformBusinessClient client) {
        String type = valueOrEmpty(client.getBusinessType()).toLowerCase(Locale.ROOT);
        if (type.contains("restaurante")) {
            return "Consulta platos, promociones y coordina pedidos por WhatsApp desde una carta digital simple.";
        }
        if (type.contains("academia") || type.contains("curso")) {
            return "Explora cursos, solicita inscripción y accede a contenidos organizados.";
        }
        if (type.contains("tienda") || type.contains("commerce")) {
            return "Explora productos, filtros y promociones con atención directa por WhatsApp.";
        }
        return "Gestiona clientes, ventas, marketing y operación desde una plataforma modular.";
    }

    private String defaultValue(String value, String fallback) {
        String clean = valueOrEmpty(value).trim();
        return clean.isBlank() ? valueOrEmpty(fallback).trim() : clean;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escapeProperty(String value) {
        return valueOrEmpty(value).replace("\\", "\\\\").replace("\n", " ").replace("\r", " ");
    }
}
