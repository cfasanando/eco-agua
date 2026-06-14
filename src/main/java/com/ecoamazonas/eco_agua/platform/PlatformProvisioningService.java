package com.ecoamazonas.eco_agua.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PlatformProvisioningService {

    private static final Set<String> DATABASE_CREATED_STATUSES = Set.of(
            "DATABASE_CREATED", "STRUCTURE_READY", "READY", "CREATED"
    );

    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformClientModuleRepository clientModuleRepository;
    private final PlatformModuleCatalogRepository moduleRepository;
    private final PlatformProvisioningLogRepository logRepository;
    private final JdbcTemplate jdbcTemplate;
    private final String sourceDatabaseName;

    public PlatformProvisioningService(PlatformBusinessClientRepository clientRepository,
                                       PlatformClientModuleRepository clientModuleRepository,
                                       PlatformModuleCatalogRepository moduleRepository,
                                       PlatformProvisioningLogRepository logRepository,
                                       JdbcTemplate jdbcTemplate,
                                       @Value("${ecoagua.platform.source-database:productos_selva_belen}") String sourceDatabaseName) {
        this.clientRepository = clientRepository;
        this.clientModuleRepository = clientModuleRepository;
        this.moduleRepository = moduleRepository;
        this.logRepository = logRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.sourceDatabaseName = sourceDatabaseName;
    }

    public PlatformProvisioningPlan buildPlan(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        List<String> activeModuleKeys = activeModuleKeys(clientId);
        String databaseStatus = safe(client.getDatabaseStatus());
        boolean databaseCreated = DATABASE_CREATED_STATUSES.contains(databaseStatus);
        boolean structureReady = "STRUCTURE_READY".equals(databaseStatus) || "READY".equals(databaseStatus) || "CREATED".equals(databaseStatus);
        boolean active = "ACTIVE".equalsIgnoreCase(safe(client.getStatus())) || "READY".equals(databaseStatus) || "CREATED".equals(databaseStatus);
        boolean ready = active && structureReady;
        String databaseName = normalizedDatabaseName(client.getDatabaseName());
        String bootstrapFileName = "bootstrap-" + databaseName + ".sql";
        String createDatabaseFileName = "create-database-" + databaseName + ".sql";
        List<String> commands = manualCommands(client);

        List<PlatformProvisioningStep> steps = new ArrayList<>();
        steps.add(step(1, "Configuración del negocio", !activeModuleKeys.isEmpty(),
                "Nombre, plantilla, base prevista y módulos activos registrados."));
        steps.add(step(2, "Crear base de datos vacía", databaseCreated,
                "Crea la base MySQL/MariaDB indicada para el negocio."));
        steps.add(step(3, "Copiar estructura del sistema", structureReady,
                "Copia solo la estructura de tablas del sistema base hacia la nueva base."));
        steps.add(step(4, "Aplicar configuración inicial", structureReady,
                "Ejecuta el SQL de configuración para branding, módulos y datos básicos."));
        steps.add(step(5, "Activar negocio", active,
                "Marca el negocio como listo para pruebas o demo comercial."));

        return new PlatformProvisioningPlan(
                client,
                steps,
                activeModuleKeys,
                createDatabaseSql(client),
                bootstrapSql(client, activeModuleKeys),
                commands,
                !databaseCreated,
                databaseCreated && !structureReady,
                structureReady && !active,
                warningFor(client, ready),
                databaseCreated,
                structureReady,
                active,
                ready,
                statusTitle(databaseCreated, structureReady, active, ready),
                statusDescription(databaseCreated, structureReady, active, ready),
                ready ? "text-bg-success" : databaseCreated ? "text-bg-warning" : "text-bg-secondary",
                ready ? "alert-success" : databaseCreated ? "alert-warning" : "alert-info",
                bootstrapFileName,
                createDatabaseFileName,
                String.join(System.lineSeparator(), commands),
                openBusinessUrl(client)
        );
    }

    public List<PlatformProvisioningLog> listLogs(Long clientId) {
        return logRepository.findByClient(clientId);
    }

    public void createDatabase(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        String databaseName = normalizedDatabaseName(client.getDatabaseName());
        String sql = createDatabaseSql(databaseName);

        try {
            jdbcTemplate.execute(sql);
            client.setDatabaseName(databaseName);
            client.setDatabaseStatus("DATABASE_CREATED");
            if ("DRAFT".equalsIgnoreCase(safe(client.getStatus()))) {
                client.setStatus("CONFIGURED");
            }
            clientRepository.save(client);
            saveLog(client, "CREATE_DATABASE", "SUCCESS", "Base de datos creada o ya existente: " + databaseName, sql);
        } catch (Exception ex) {
            saveLog(client, "CREATE_DATABASE", "ERROR", cleanError(ex.getMessage()), sql);
            throw new IllegalArgumentException("No se pudo crear la base de datos. Revisa permisos MySQL o ejecuta el SQL manualmente.");
        }
    }

    @Transactional
    public void markStructureReady(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        client.setDatabaseStatus("STRUCTURE_READY");
        if (!"ACTIVE".equalsIgnoreCase(safe(client.getStatus()))) {
            client.setStatus("PROVISIONING");
        }
        clientRepository.save(client);
        saveLog(client, "MARK_STRUCTURE_READY", "SUCCESS", "La estructura fue marcada como copiada en la base del cliente.", null);
    }

    @Transactional
    public void markActive(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        client.setDatabaseStatus("READY");
        client.setStatus("ACTIVE");
        clientRepository.save(client);
        saveLog(client, "MARK_ACTIVE", "SUCCESS", "Negocio activado para demo o pruebas internas.", null);
    }

    @Transactional
    public void resetProvisioning(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        client.setDatabaseStatus("PENDING_STRUCTURE");
        client.setStatus("CONFIGURED");
        clientRepository.save(client);
        saveLog(client, "RESET_PROVISIONING", "SUCCESS", "Estado de aprovisionamiento reiniciado sin eliminar base de datos.", null);
    }

    private PlatformBusinessClient getClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Business client not found."));
    }

    private List<String> activeModuleKeys(Long clientId) {
        return clientModuleRepository.findClientModules(clientId).stream()
                .filter(PlatformClientModule::isEnabled)
                .map(item -> item.getModule().getModuleKey())
                .toList();
    }

    private PlatformProvisioningStep step(int number, String title, boolean done, String description) {
        return new PlatformProvisioningStep(
                number,
                title,
                done ? "Listo" : "Pendiente",
                description,
                done ? "text-bg-success" : "text-bg-warning"
        );
    }

    private List<String> manualCommands(PlatformBusinessClient client) {
        String db = normalizedDatabaseName(client.getDatabaseName());
        return List.of(
                "mysqldump -u root -p --no-data --routines --triggers " + sourceDatabaseName + " > estructura-base.sql",
                "mysql -u root -p " + db + " < estructura-base.sql",
                "mysql -u root -p " + db + " < bootstrap-" + db + ".sql"
        );
    }

    private String createDatabaseSql(PlatformBusinessClient client) {
        return createDatabaseSql(normalizedDatabaseName(client.getDatabaseName())) + ";";
    }

    private String createDatabaseSql(String databaseName) {
        return "CREATE DATABASE IF NOT EXISTS `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
    }

    private String bootstrapSql(PlatformBusinessClient client, List<String> activeModuleKeys) {
        String db = normalizedDatabaseName(client.getDatabaseName());
        Set<String> activeKeys = new LinkedHashSet<>(activeModuleKeys);
        StringBuilder sql = new StringBuilder();
        sql.append("-- Bootstrap inicial generado desde Super Admin para: ").append(sqlComment(client.getBusinessName())).append("\n");
        sql.append("-- Ejecutar este script después de copiar la estructura base.\n\n");
        sql.append("USE `").append(db).append("`;\n\n");

        appendSetting(sql, "platform.name", client.getBusinessName(), "string", "platform", "Nombre comercial del negocio");
        appendSetting(sql, "platform.tagline", taglineFor(client), "string", "platform", "Lema inicial del negocio");
        appendSetting(sql, "public.whatsapp.number", client.getWhatsapp(), "string", "public_site", "WhatsApp comercial del negocio");
        appendSetting(sql, "public.topbar.phone", defaultValue(client.getContactPhone(), client.getWhatsapp()), "string", "public_site", "Teléfono principal del negocio");
        appendSetting(sql, "public.topbar.location", defaultValue(client.getCity(), "Iquitos"), "string", "public_site", "Ciudad o zona de atención");
        appendSetting(sql, "public.theme.primary_color", defaultValue(client.getPrimaryColor(), "#0d6efd"), "string", "public_site", "Color principal del portal público");
        appendSetting(sql, "public.nav.home_label", "Inicio", "string", "public_site", "Etiqueta de inicio");
        appendSetting(sql, "public.nav.catalog_label", navCatalogLabel(client), "string", "public_site", "Etiqueta de catálogo o carta");
        appendSetting(sql, "public.nav.access_label", "Acceso colaboradores", "string", "public_site", "Etiqueta de login interno");

        sql.append("\n-- Módulos activos según la plantilla seleccionada.\n");
        for (PlatformModuleCatalog module : moduleRepository.findAllByActiveTrueOrderByAreaAscDisplayOrderAscNameAsc()) {
            boolean enabled = activeKeys.contains(module.getModuleKey());
            appendSetting(sql,
                    "module." + module.getModuleKey() + ".enabled",
                    Boolean.toString(enabled),
                    "boolean",
                    "system_modules",
                    "Módulo " + module.getName());
        }

        sql.append("\n-- Usuario administrador inicial sugerido.\n");
        sql.append("-- Recomendado: crear desde la pantalla Usuarios del sistema destino.\n");
        sql.append("-- Usuario sugerido: admin_demo / clave temporal: Demo12345\n");
        sql.append("-- Luego cambiar la clave y asignar permisos según el rubro.\n\n");

        if (client.isDemoDataEnabled()) {
            sql.append("-- Datos demo: habilitados.\n");
            sql.append("-- Ejecutar luego el script demo específico de la plantilla cuando exista.\n");
        } else {
            sql.append("-- Datos demo: no solicitados para este negocio.\n");
        }

        return sql.toString();
    }

    private void appendSetting(StringBuilder sql, String variable, String value, String type, String category, String description) {
        sql.append("INSERT INTO platform_setting (`variable`, `value`, `type`, `category`, `description`) VALUES (")
                .append("'").append(sql(valueOrEmpty(variable))).append("', ")
                .append("'").append(sql(valueOrEmpty(value))).append("', ")
                .append("'").append(sql(valueOrEmpty(type))).append("', ")
                .append("'").append(sql(valueOrEmpty(category))).append("', ")
                .append("'").append(sql(valueOrEmpty(description))).append("') ")
                .append("ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `type` = VALUES(`type`), `category` = VALUES(`category`), `description` = VALUES(`description`);\n");
    }

    private void saveLog(PlatformBusinessClient client, String action, String status, String details, String sqlSnippet) {
        PlatformProvisioningLog log = new PlatformProvisioningLog();
        log.setClient(client);
        log.setAction(action);
        log.setStatus(status);
        log.setDetails(details);
        log.setSqlSnippet(sqlSnippet);
        logRepository.save(log);
    }

    private String warningFor(PlatformBusinessClient client, boolean ready) {
        if (ready) {
            return "Este negocio ya está listo. No es necesario repetir los pasos de aprovisionamiento, salvo que quieras reiniciar el flujo de pruebas.";
        }
        if (client.getDatabaseName() == null || client.getDatabaseName().isBlank()) {
            return "Este negocio no tiene nombre de base de datos configurado.";
        }
        if (activeModuleKeys(client.getId()).isEmpty()) {
            return "Este negocio no tiene módulos activos. Revisa la configuración antes de aprovisionar.";
        }
        return "El aprovisionamiento crea una base vacía. La estructura completa se copia con el comando manual sugerido para evitar romper el sistema actual.";
    }

    private String statusTitle(boolean databaseCreated, boolean structureReady, boolean active, boolean ready) {
        if (ready) {
            return "Negocio listo";
        }
        if (structureReady && !active) {
            return "Falta activar el negocio";
        }
        if (databaseCreated) {
            return "Base creada, falta copiar estructura";
        }
        return "Pendiente de aprovisionamiento";
    }

    private String statusDescription(boolean databaseCreated, boolean structureReady, boolean active, boolean ready) {
        if (ready) {
            return "La base, estructura y configuración inicial fueron marcadas como listas para demo o pruebas internas.";
        }
        if (structureReady && !active) {
            return "La estructura ya fue marcada como copiada. Aplica el SQL bootstrap y luego activa el negocio.";
        }
        if (databaseCreated) {
            return "La base vacía ya existe. Copia la estructura del sistema base y marca el paso como completado.";
        }
        return "Revisa los datos del negocio, crea la base vacía y continúa con los comandos manuales.";
    }

    private String openBusinessUrl(PlatformBusinessClient client) {
        String publicUrl = valueOrEmpty(client.getPublicUrl()).trim();
        if (!publicUrl.isBlank()) {
            return publicUrl;
        }
        if (client.getRuntimePort() != null && client.getRuntimePort() > 0) {
            return "http://localhost:" + client.getRuntimePort();
        }
        String slug = valueOrEmpty(client.getPublicSlug());
        if (!slug.isBlank()) {
            return "/?client=" + slug;
        }
        return "/";
    }

    private String normalizedDatabaseName(String value) {
        String normalized = valueOrEmpty(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Database name is required.");
        }
        if (!normalized.matches("[a-z][a-z0-9_]{1,62}")) {
            throw new IllegalArgumentException("Database name must start with a letter and contain only lowercase letters, numbers and underscores.");
        }
        return normalized;
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
        if (type.contains("tienda")) {
            return "Catálogo, promociones y pedidos por WhatsApp.";
        }
        return "Sistema modular para gestionar tu negocio.";
    }

    private String navCatalogLabel(PlatformBusinessClient client) {
        String type = valueOrEmpty(client.getBusinessType()).toLowerCase(Locale.ROOT);
        if (type.contains("restaurante")) {
            return "Carta";
        }
        if (type.contains("academia") || type.contains("curso")) {
            return "Cursos";
        }
        return "Catálogo";
    }

    private String defaultValue(String value, String fallback) {
        String clean = valueOrEmpty(value).trim();
        return clean.isBlank() ? valueOrEmpty(fallback).trim() : clean;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String sql(String value) {
        return valueOrEmpty(value).replace("'", "''");
    }

    private String sqlComment(String value) {
        return valueOrEmpty(value).replace("\n", " ").replace("\r", " ");
    }

    private String cleanError(String value) {
        String clean = valueOrEmpty(value).replace("\n", " ").replace("\r", " ");
        return clean.length() > 1000 ? clean.substring(0, 1000) : clean;
    }
}
