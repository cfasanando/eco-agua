package com.ecoamazonas.eco_agua.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PlatformProvisioningService {

    private static final Set<String> DATABASE_CREATED_STATUSES = Set.of(
            "DATABASE_CREATED", "STRUCTURE_READY", "BOOTSTRAP_APPLIED", "DEMO_DATA_LOADED", "READY", "CREATED"
    );
    private static final Set<String> STRUCTURE_READY_STATUSES = Set.of(
            "STRUCTURE_READY", "BOOTSTRAP_APPLIED", "DEMO_DATA_LOADED", "READY", "CREATED"
    );
    private static final Set<String> BOOTSTRAP_READY_STATUSES = Set.of(
            "BOOTSTRAP_APPLIED", "DEMO_DATA_LOADED", "READY", "CREATED"
    );
    private static final Set<String> DEMO_DATA_READY_STATUSES = Set.of(
            "DEMO_DATA_LOADED", "READY", "CREATED"
    );
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?is)^CREATE\\s+TABLE\\s+`([^`]+)`");

    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformClientModuleRepository clientModuleRepository;
    private final PlatformModuleCatalogRepository moduleRepository;
    private final PlatformProvisioningLogRepository logRepository;
    private final PlatformRuntimeService runtimeService;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String sourceDatabaseName;
    private final String runtimeClientsDirectory;

    public PlatformProvisioningService(PlatformBusinessClientRepository clientRepository,
                                       PlatformClientModuleRepository clientModuleRepository,
                                       PlatformModuleCatalogRepository moduleRepository,
                                       PlatformProvisioningLogRepository logRepository,
                                       PlatformRuntimeService runtimeService,
                                       JdbcTemplate jdbcTemplate,
                                       PasswordEncoder passwordEncoder,
                                       @Value("${ecoagua.platform.source-database:productos_selva_belen}") String sourceDatabaseName,
                                       @Value("${ecoagua.platform.runtime-clients-dir:runtime-clients}") String runtimeClientsDirectory) {
        this.clientRepository = clientRepository;
        this.clientModuleRepository = clientModuleRepository;
        this.moduleRepository = moduleRepository;
        this.logRepository = logRepository;
        this.runtimeService = runtimeService;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.sourceDatabaseName = sourceDatabaseName;
        this.runtimeClientsDirectory = runtimeClientsDirectory;
    }

    public PlatformProvisioningPlan buildPlan(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        List<String> activeModuleKeys = activeModuleKeys(clientId);
        String databaseStatus = safe(client.getDatabaseStatus()).toUpperCase(Locale.ROOT);
        boolean databaseCreated = DATABASE_CREATED_STATUSES.contains(databaseStatus);
        boolean structureReady = STRUCTURE_READY_STATUSES.contains(databaseStatus);
        boolean bootstrapApplied = BOOTSTRAP_READY_STATUSES.contains(databaseStatus);
        boolean demoDataRequested = client.isDemoDataEnabled();
        boolean demoDataLoaded = DEMO_DATA_READY_STATUSES.contains(databaseStatus);
        boolean demoDataReady = !demoDataRequested || demoDataLoaded;
        boolean active = "ACTIVE".equalsIgnoreCase(safe(client.getStatus())) || "READY".equals(databaseStatus) || "CREATED".equals(databaseStatus);
        boolean ready = active && bootstrapApplied && demoDataReady;
        String databaseName = normalizedDatabaseName(client.getDatabaseName());
        String bootstrapFileName = "bootstrap-" + databaseName + ".sql";
        String demoDataFileName = "demo-data-" + databaseName + ".sql";
        String createDatabaseFileName = "create-database-" + databaseName + ".sql";
        String runtimeProfile = normalizeRuntimeProfile(defaultValue(client.getRuntimeProfile(), client.getCode()));
        Path runtimeFolderPath = runtimeFolder(runtimeProfile);
        String applicationPath = runtimeFolderPath.resolve("application.properties").toString();
        String runScriptPath = runtimeFolderPath.resolve("run.sh").toString();
        boolean runtimeFilesGenerated = Files.exists(runtimeFolderPath.resolve("application.properties"))
                && Files.exists(runtimeFolderPath.resolve("run.sh"));
        List<String> commands = manualCommands(client);

        List<PlatformProvisioningStep> steps = new ArrayList<>();
        steps.add(step(1, "Configuración del negocio", !activeModuleKeys.isEmpty(),
                "Nombre, plantilla, base prevista y módulos activos registrados."));
        steps.add(step(2, "Crear base de datos vacía", databaseCreated,
                "Crea la base MySQL/MariaDB indicada para el negocio."));
        steps.add(step(3, "Copiar estructura del sistema", structureReady,
                "Copia automáticamente la estructura de tablas desde la base modelo."));
        steps.add(step(4, "Aplicar configuración inicial", bootstrapApplied,
                "Ejecuta el bootstrap generado para branding, módulos y datos básicos."));
        steps.add(step(5, "Cargar datos demo", demoDataReady,
                demoDataRequested ? "Inserta productos, clientes, marketing y operación inicial según la plantilla." : "Omitido: este negocio fue creado sin datos demo."));
        steps.add(step(6, "Generar runtime y activar", ready,
                "Genera archivos runtime y marca el negocio listo para demo."));

        return new PlatformProvisioningPlan(
                client,
                steps,
                activeModuleKeys,
                createDatabaseSql(client),
                bootstrapSql(client, activeModuleKeys),
                commands,
                !databaseCreated,
                databaseCreated && !structureReady,
                structureReady && !bootstrapApplied,
                databaseCreated && !structureReady,
                bootstrapApplied && demoDataRequested && !demoDataLoaded,
                bootstrapApplied && demoDataReady && !active,
                bootstrapApplied && demoDataReady && active,
                warningFor(client, ready),
                databaseCreated,
                structureReady,
                bootstrapApplied,
                demoDataRequested,
                demoDataLoaded,
                active,
                ready,
                statusTitle(databaseCreated, structureReady, bootstrapApplied, demoDataRequested, demoDataLoaded, active, ready),
                statusDescription(databaseCreated, structureReady, bootstrapApplied, demoDataRequested, demoDataLoaded, active, ready, runtimeFilesGenerated),
                ready ? "text-bg-success" : demoDataRequested && bootstrapApplied && !demoDataLoaded ? "text-bg-warning" : bootstrapApplied ? "text-bg-info" : databaseCreated ? "text-bg-warning" : "text-bg-secondary",
                ready ? "alert-success" : demoDataRequested && bootstrapApplied && !demoDataLoaded ? "alert-warning" : bootstrapApplied ? "alert-info" : databaseCreated ? "alert-warning" : "alert-info",
                bootstrapFileName,
                demoDataFileName,
                createDatabaseFileName,
                String.join(System.lineSeparator(), commands),
                openBusinessUrl(client),
                runtimeFolderPath.toString(),
                applicationPath,
                runScriptPath,
                templateDemoDataSql(client, activeModuleKeys)
        );
    }

    public List<PlatformProvisioningLog> listLogs(Long clientId) {
        return logRepository.findByClient(clientId);
    }

    public void createDatabase(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        ensureProvisioningAllowed(client);
        String databaseName = normalizedDatabaseName(client.getDatabaseName());
        String sql = createDatabaseSql(databaseName);

        try {
            jdbcTemplate.execute(sql);
            client.setDatabaseName(databaseName);
            client.setDatabaseStatus("DATABASE_CREATED");
            if ("DRAFT".equalsIgnoreCase(safe(client.getStatus()))) {
                client.setStatus("CONFIGURED");
            }
            clientRepository.saveAndFlush(client);
            saveLog(client, "CREATE_DATABASE", "SUCCESS", "Base de datos creada o ya existente: " + databaseName, sql);
        } catch (Exception ex) {
            saveLog(client, "CREATE_DATABASE", "ERROR", cleanError(ex.getMessage()), sql);
            throw new IllegalArgumentException("No se pudo crear la base de datos. Revisa permisos MySQL o ejecuta el SQL manualmente.");
        }
    }

    @Transactional
    public void copyStructureAutomatically(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        ensureProvisioningAllowed(client);
        String targetDatabase = normalizedDatabaseName(client.getDatabaseName());
        String sourceDatabase = normalizedDatabaseName(sourceDatabaseName);
        if (sourceDatabase.equals(targetDatabase)) {
            throw new IllegalArgumentException("La base origen y destino no pueden ser la misma.");
        }

        List<String> tables = listBaseTables(sourceDatabase);
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron tablas base en " + sourceDatabase + ".");
        }

        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String table : tables) {
                String ddl = showCreateTable(sourceDatabase, table);
                jdbcTemplate.execute(toTargetCreateTableSql(ddl, targetDatabase));
            }
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
            client.setDatabaseStatus("STRUCTURE_READY");
            client.setRuntimeStatus("PENDING");
            client.setLastRuntimeGeneratedAt(null);
            if (!"ACTIVE".equalsIgnoreCase(safe(client.getStatus()))) {
                client.setStatus("PROVISIONING");
            }
            clientRepository.saveAndFlush(client);
            saveLog(client, "COPY_STRUCTURE_AUTO", "SUCCESS",
                    "Estructura copiada automáticamente desde " + sourceDatabase + " hacia " + targetDatabase + ". Tablas: " + tables.size(),
                    "SHOW CREATE TABLE + CREATE TABLE IF NOT EXISTS");
        } catch (Exception ex) {
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
            } catch (Exception ignored) {
                // Ignore cleanup errors; original error is logged below.
            }
            saveLog(client, "COPY_STRUCTURE_AUTO", "ERROR", cleanError(ex.getMessage()), null);
            throw new IllegalArgumentException("No se pudo copiar la estructura automáticamente. Usa los comandos manuales como respaldo.");
        }
    }

    @Transactional
    public void applyBootstrapAutomatically(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        ensureProvisioningAllowed(client);
        List<String> activeModuleKeys = activeModuleKeys(clientId);
        String bootstrapSql = bootstrapSql(client, activeModuleKeys);
        List<String> statements = splitSqlStatements(bootstrapSql);

        if (statements.isEmpty()) {
            throw new IllegalArgumentException("No se generó SQL bootstrap para aplicar.");
        }

        try {
            for (String statement : statements) {
                jdbcTemplate.execute(statement);
            }
            // The bootstrap starts with USE target_database. Switch the shared connection back
            // to the platform/source database before saving JPA entities and provisioning logs.
            useDatabase(sourceDatabaseName);
            client.setDatabaseStatus("BOOTSTRAP_APPLIED");
            client.setRuntimeStatus("PENDING");
            client.setLastRuntimeGeneratedAt(null);
            if (!"ACTIVE".equalsIgnoreCase(safe(client.getStatus()))) {
                client.setStatus("PROVISIONING");
            }
            clientRepository.saveAndFlush(client);
            saveLog(client, "APPLY_BOOTSTRAP_AUTO", "SUCCESS",
                    "Configuración inicial aplicada automáticamente en " + normalizedDatabaseName(client.getDatabaseName()) + ". Sentencias: " + statements.size(),
                    bootstrapSql);
        } catch (Exception ex) {
            // If the failing statement changed the current schema with USE target_database,
            // ensure the error log is written in the platform/source database.
            try {
                useDatabase(sourceDatabaseName);
            } catch (Exception ignored) {
                // Keep the original error as the relevant failure reason.
            }
            saveLog(client, "APPLY_BOOTSTRAP_AUTO", "ERROR", cleanError(ex.getMessage()), bootstrapSql);
            throw new IllegalArgumentException("No se pudo aplicar el bootstrap automáticamente. Revisa que la estructura exista en la base destino.");
        }
    }


    @Transactional
    public void loadTemplateDemoDataAutomatically(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        ensureProvisioningAllowed(client);
        if (!client.isDemoDataEnabled()) {
            throw new IllegalArgumentException("Este negocio fue creado sin datos demo habilitados.");
        }

        String databaseStatus = safe(client.getDatabaseStatus()).toUpperCase(Locale.ROOT);
        if (!BOOTSTRAP_READY_STATUSES.contains(databaseStatus) && !DEMO_DATA_READY_STATUSES.contains(databaseStatus)) {
            throw new IllegalArgumentException("Primero aplica la configuración inicial antes de cargar datos demo.");
        }

        List<String> activeModuleKeys = activeModuleKeys(clientId);
        String demoSql = templateDemoDataSql(client, activeModuleKeys);
        List<String> statements = splitSqlStatements(demoSql);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("No se generó SQL demo para esta plantilla.");
        }

        try {
            for (String statement : statements) {
                jdbcTemplate.execute(statement);
            }
            useDatabase(sourceDatabaseName);
            client.setDatabaseStatus("DEMO_DATA_LOADED");
            client.setRuntimeStatus("PENDING");
            client.setLastRuntimeGeneratedAt(null);
            if (!"ACTIVE".equalsIgnoreCase(safe(client.getStatus()))) {
                client.setStatus("PROVISIONING");
            }
            clientRepository.saveAndFlush(client);
            saveLog(client, "LOAD_TEMPLATE_DEMO_DATA", "SUCCESS",
                    "Datos demo cargados para plantilla " + templateCode(client) + " en " + normalizedDatabaseName(client.getDatabaseName()) + ". Sentencias: " + statements.size(),
                    demoSql);
        } catch (Exception ex) {
            try {
                useDatabase(sourceDatabaseName);
            } catch (Exception ignored) {
                // Keep original error as the relevant failure reason.
            }
            saveLog(client, "LOAD_TEMPLATE_DEMO_DATA", "ERROR", cleanError(ex.getMessage()), demoSql);
            throw new IllegalArgumentException("No se pudieron cargar los datos demo. Revisa que la estructura base y el bootstrap existan en la base destino.");
        }
    }

    @Transactional
    public void generateRuntimeFiles(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        ensureProvisioningAllowed(client);
        String profile = normalizeRuntimeProfile(defaultValue(client.getRuntimeProfile(), client.getCode()));
        int port = client.getRuntimePort() == null || client.getRuntimePort() <= 0 ? suggestedPort(client) : client.getRuntimePort();
        String publicUrl = defaultValue(client.getPublicUrl(), "http://localhost:" + port);

        runtimeService.saveRuntimeSettings(clientId, profile, port, publicUrl);
        PlatformRuntimePlan runtime = runtimeService.buildPlan(clientId);
        PlatformProvisioningPlan provisioningPlan = buildPlan(clientId);
        Path folder = runtimeFolder(runtime.runtimeProfile());

        try {
            Files.createDirectories(folder);
            writeFile(folder.resolve("application.properties"), runtime.applicationProperties());
            writeFile(folder.resolve("run.sh"), runtime.runScript());
            writeFile(folder.resolve(provisioningPlan.createDatabaseFileName()), provisioningPlan.createDatabaseSql());
            writeFile(folder.resolve(provisioningPlan.bootstrapFileName()), provisioningPlan.bootstrapSql());
            writeFile(folder.resolve(provisioningPlan.demoDataFileName()), provisioningPlan.demoDataSql());
            writeFile(folder.resolve("README.txt"), runtimeReadme(runtime, provisioningPlan));
            markExecutable(folder.resolve("run.sh"));

            PlatformBusinessClient updatedClient = getClient(clientId);
            updatedClient.setRuntimeStatus("FILES_GENERATED");
            updatedClient.setLastRuntimeGeneratedAt(java.time.LocalDateTime.now());
            clientRepository.saveAndFlush(updatedClient);
            saveLog(updatedClient, "GENERATE_RUNTIME_FILES", "SUCCESS",
                    "Archivos runtime generados en " + folder.toAbsolutePath().normalize(), null);
        } catch (IOException ex) {
            saveLog(client, "GENERATE_RUNTIME_FILES", "ERROR", cleanError(ex.getMessage()), null);
            throw new IllegalArgumentException("No se pudieron generar los archivos runtime. Revisa permisos de escritura en la carpeta del proyecto.");
        }
    }

    @Transactional
    public void markStructureReady(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        ensureProvisioningAllowed(client);
        client.setDatabaseStatus("STRUCTURE_READY");
        if (!"ACTIVE".equalsIgnoreCase(safe(client.getStatus()))) {
            client.setStatus("PROVISIONING");
        }
        clientRepository.saveAndFlush(client);
        saveLog(client, "MARK_STRUCTURE_READY", "SUCCESS", "La estructura fue marcada como copiada en la base del cliente.", null);
    }

    @Transactional
    public void markActive(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        ensureProvisioningAllowed(client);
        client.setDatabaseStatus("READY");
        client.setStatus("ACTIVE");
        clientRepository.saveAndFlush(client);
        saveLog(client, "MARK_ACTIVE", "SUCCESS", "Negocio activado para demo o pruebas internas.", null);
    }

    @Transactional
    public void resetProvisioning(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
        ensureProvisioningAllowed(client);
        client.setDatabaseStatus("PENDING_STRUCTURE");
        client.setStatus("CONFIGURED");
        client.setRuntimeStatus("PENDING");
        client.setLastRuntimeGeneratedAt(null);
        clientRepository.saveAndFlush(client);
        saveLog(client, "RESET_PROVISIONING", "SUCCESS", "Estado de aprovisionamiento reiniciado sin eliminar base de datos.", null);
    }

    private void ensureProvisioningAllowed(PlatformBusinessClient client) {
        if (client.isProtectedInstance() || "PROTECTED".equalsIgnoreCase(safe(client.getManagementMode()))) {
            throw new IllegalArgumentException("Esta instancia está protegida. No se permite reiniciar, copiar estructura ni cargar datos demo desde el aprovisionamiento.");
        }
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



    private Set<String> normalizedActiveKeysForClient(PlatformBusinessClient client, List<String> activeModuleKeys) {
        Set<String> activeKeys = new LinkedHashSet<>(activeModuleKeys);
        if (isRestaurantClient(client)) {
            activeKeys.add("restaurant");
            activeKeys.add("restaurant_tables");
            activeKeys.add("restaurant_kitchen");
            activeKeys.add("restaurant_menu_qr");
            activeKeys.add("products");
            activeKeys.add("public_catalog");
            activeKeys.add("delivery");
            activeKeys.add("income");
            activeKeys.add("sales");
            activeKeys.add("supplies");
            activeKeys.add("marketing");
        }
        return activeKeys;
    }

    private boolean isRestaurantClient(PlatformBusinessClient client) {
        String template = templateCode(client);
        return template.contains("restaurant") || template.contains("restaurante");
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
                "mysql -u root -p " + db + " < bootstrap-" + db + ".sql",
                "mysql -u root -p " + db + " < demo-data-" + db + ".sql",
                "bash runtime-clients/" + normalizeRuntimeProfile(defaultValue(client.getRuntimeProfile(), client.getCode())) + "/run.sh"
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
        Set<String> activeKeys = normalizedActiveKeysForClient(client, activeModuleKeys);
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

        if (isRestaurantClient(client)) {
            appendRestaurantSchema(sql);
        }

        appendInitialAdminUser(sql);

        if (client.isDemoDataEnabled()) {
            sql.append("-- Datos demo: habilitados.\n");
            sql.append("-- Ejecutar luego el script demo específico de la plantilla cuando exista.\n");
        } else {
            sql.append("-- Datos demo: no solicitados para este negocio.\n");
        }

        return sql.toString();
    }


    private String templateDemoDataSql(PlatformBusinessClient client, List<String> activeModuleKeys) {
        String db = normalizedDatabaseName(client.getDatabaseName());
        String template = templateCode(client);
        StringBuilder sql = new StringBuilder();
        sql.append("-- Datos demo generados desde Super Admin para: ").append(sqlComment(client.getBusinessName())).append("\n");
        sql.append("-- Plantilla detectada: ").append(sqlComment(template)).append("\n");
        sql.append("-- Ejecutar después del bootstrap inicial.\n\n");
        sql.append("USE `").append(db).append("`;\n\n");
        sql.append("SET @demo_now = NOW();\n\n");

        appendCommonDemoSettings(sql, client);

        if (template.contains("restaurant") || template.contains("restaurante")) {
            appendRestaurantDemoData(sql);
        } else if (template.contains("temu") || template.contains("ecommerce") || template.contains("tienda") || template.contains("e_commerce")) {
            appendTemuDemoData(sql);
        } else if (template.contains("academy") || template.contains("academia") || template.contains("curso")) {
            appendAcademyDemoData(sql);
        } else if (template.contains("courier") || template.contains("rutapack") || template.contains("ruta")) {
            appendCourierDemoData(sql);
        } else if (template.contains("selva") || template.contains("catalog")) {
            appendSelvaDemoData(sql);
        } else {
            appendAguaEcoDemoData(sql);
        }

        sql.append("\n-- Marca interna para auditoría del aprovisionamiento demo.\n");
        appendSetting(sql, "platform.demo.loaded", "true", "boolean", "platform", "Indica que los datos demo de plantilla fueron cargados");
        appendSetting(sql, "platform.demo.template", template, "string", "platform", "Plantilla usada para cargar datos demo");
        return sql.toString();
    }

    private void appendCommonDemoSettings(StringBuilder sql, PlatformBusinessClient client) {
        String type = templateCode(client);
        if (type.contains("restaurant") || type.contains("restaurante")) {
            appendSetting(sql, "public.hero.badge1", "Carta digital", "string", "public_site", "Beneficio público demo");
            appendSetting(sql, "public.hero.badge2", "Pedidos para llevar", "string", "public_site", "Beneficio público demo");
            appendSetting(sql, "public.hero.badge3", "Delivery coordinado", "string", "public_site", "Beneficio público demo");
        } else if (type.contains("temu") || type.contains("ecommerce") || type.contains("tienda")) {
            appendSetting(sql, "public.hero.badge1", "Catálogo con filtros", "string", "public_site", "Beneficio público demo");
            appendSetting(sql, "public.hero.badge2", "Promos por WhatsApp", "string", "public_site", "Beneficio público demo");
            appendSetting(sql, "public.hero.badge3", "Entrega coordinada", "string", "public_site", "Beneficio público demo");
        } else if (type.contains("academy") || type.contains("academia")) {
            appendSetting(sql, "public.hero.badge1", "Cursos online", "string", "public_site", "Beneficio público demo");
            appendSetting(sql, "public.hero.badge2", "Certificados", "string", "public_site", "Beneficio público demo");
            appendSetting(sql, "public.hero.badge3", "Lecciones guiadas", "string", "public_site", "Beneficio público demo");
        }
    }

    private void appendTemuDemoData(StringBuilder sql) {
        appendProducts(sql, List.of(
                productRow("Mini licuadora portátil USB", "Producto compacto para jugos, batidos y uso diario. Ideal para catálogo tipo Temu.", "/img/catalog/temu-mini-licuadora.jpg", "49.90", true, "35", "5"),
                productRow("Audífonos Bluetooth económicos", "Audífonos inalámbricos para llamadas, música y ventas por WhatsApp.", "/img/catalog/temu-audifonos.jpg", "39.90", true, "60", "10"),
                productRow("Organizador plegable multiuso", "Organizador liviano para cocina, dormitorio o escritorio.", "/img/catalog/temu-organizador.jpg", "24.90", false, "80", "15"),
                productRow("Luz LED recargable para escritorio", "Lámpara LED portátil con carga USB para estudio y trabajo.", "/img/catalog/temu-luz-led.jpg", "29.90", false, "45", "8")
        ));
        appendClients(sql, List.of(
                clientRow("Cliente WhatsApp Norte", "70000001", "Av. La Marina 1200", "Pregunta por combos y delivery", "+51911111111", "-3.7438000", "-73.2516000"),
                clientRow("Cliente Compras por Mayor", "70000002", "Jr. Próspero 455", "Busca productos económicos por docena", "+51922222222", "-3.7499000", "-73.2442000"),
                clientRow("Cliente Catálogo Online", "70000003", "Belén zona comercial", "Consulta disponibilidad antes de pagar", "+51933333333", "-3.7608000", "-73.2474000")
        ));
        appendDeliveryZones(sql);
        appendMarketing(sql,
                "Semana de productos virales",
                "Mostrar 5 productos económicos con video corto y CTA WhatsApp.",
                "¿Buscas algo útil, bonito y barato? Consulta disponibilidad por WhatsApp.",
                "Consulta disponibilidad");
    }

    private void appendRestaurantDemoData(StringBuilder sql) {
        appendProducts(sql, List.of(
                productRow("Juane amazónico especial", "Plato típico con arroz, presa y sazón regional. Ideal para carta digital.", "/img/catalog/restaurant-juane.jpg", "18.00", true, "30", "5"),
                productRow("Tacacho con cecina", "Plato fuerte regional para salón, delivery y promociones.", "/img/catalog/restaurant-tacacho.jpg", "25.00", true, "25", "5"),
                productRow("Refresco de camu camu", "Bebida natural amazónica para acompañar el menú.", "/img/catalog/restaurant-camu-camu.jpg", "7.00", false, "50", "10"),
                productRow("Combo familiar amazónico", "Combo de platos regionales para compartir y vender por WhatsApp.", "/img/catalog/restaurant-combo.jpg", "69.00", true, "12", "3")
        ));
        appendClients(sql, List.of(
                clientRow("Mesa demo 01", "71000001", "Salón principal", "Cliente para prueba de comanda", "+51944444444", "-3.7487000", "-73.2479000"),
                clientRow("Delivery Oficina Centro", "71000002", "Calle Putumayo 640", "Pedido para almuerzo", "+51955555555", "-3.7468000", "-73.2439000"),
                clientRow("Cliente frecuente familiar", "71000003", "Av. Quiñones 1800", "Compra combos fines de semana", "+51966666666", "-3.7670000", "-73.2825000")
        ));
        appendDeliveryZones(sql);
        appendRestaurantTablesAndSampleOrder(sql);
        appendMarketing(sql,
                "Menú del día y combos familiares",
                "Publicar plato del día a las 10:30 a. m. y reforzar combos por WhatsApp.",
                "Hoy cocina regional lista para llevar. Reserva tu plato por WhatsApp.",
                "Pedir menú de hoy");
    }

    private void appendAcademyDemoData(StringBuilder sql) {
        sql.append("INSERT INTO academy_course (`title`, `slug`, `category`, `instructor`, `short_description`, `long_description`, `cover_image_url`, `duration_label`, `price`, `featured`, `active`, `status`, `level`, `whatsapp_message`, `created_at`, `updated_at`, `published_at`)\n")
                .append("SELECT 'Curso demo: ventas por WhatsApp', 'curso-demo-ventas-whatsapp', 'Ventas', 'Equipo comercial', 'Aprende a convertir consultas en pedidos.', 'Curso demo generado para presentar la Academia del negocio.', '/img/academy/course-whatsapp.jpg', '2 horas', 49.00, true, true, 'PUBLISHED', 'BEGINNER', 'Hola, deseo información del curso demo de ventas por WhatsApp', @demo_now, @demo_now, @demo_now\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM academy_course WHERE slug = 'curso-demo-ventas-whatsapp');\n")
                .append("SET @academy_course_id = (SELECT id FROM academy_course WHERE slug = 'curso-demo-ventas-whatsapp' LIMIT 1);\n")
                .append("INSERT INTO academy_course_module (`course_id`, `title`, `description`, `display_order`, `active`, `created_at`, `updated_at`)\n")
                .append("SELECT @academy_course_id, 'Módulo 1: atención y cierre', 'Flujo de atención, objeciones y cierre por WhatsApp.', 1, true, @demo_now, @demo_now\n")
                .append("WHERE @academy_course_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM academy_course_module WHERE course_id = @academy_course_id AND title = 'Módulo 1: atención y cierre');\n")
                .append("SET @academy_module_id = (SELECT id FROM academy_course_module WHERE course_id = @academy_course_id AND title = 'Módulo 1: atención y cierre' LIMIT 1);\n")
                .append("INSERT INTO academy_lesson (`course_id`, `module_id`, `title`, `description`, `lesson_type`, `content_text`, `duration_label`, `display_order`, `preview`, `active`, `status`, `created_at`, `updated_at`)\n")
                .append("SELECT @academy_course_id, @academy_module_id, 'Lección demo: primer mensaje efectivo', 'Guía para responder consultas sin perder ventas.', 'TEXT', 'Plantilla: saludo + disponibilidad + beneficio + cierre por WhatsApp.', '15 min', 1, true, true, 'PUBLISHED', @demo_now, @demo_now\n")
                .append("WHERE @academy_course_id IS NOT NULL AND @academy_module_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM academy_lesson WHERE course_id = @academy_course_id AND title = 'Lección demo: primer mensaje efectivo');\n\n");
        appendClients(sql, List.of(
                clientRow("Alumno demo WhatsApp", "72000001", "Online", "Interesado en cursos cortos", "+51977777777", "-3.7481000", "-73.2448000")
        ));
        appendMarketing(sql,
                "Lanzamiento de curso demo",
                "Promocionar curso corto para emprendedores con certificado.",
                "Aprende en poco tiempo y aplica hoy mismo en tu negocio.",
                "Solicitar inscripción");
    }

    private void appendCourierDemoData(StringBuilder sql) {
        appendClients(sql, List.of(
                clientRow("Cliente Ruta Centro", "73000001", "Plaza 28 de Julio", "Entrega con evidencia", "+51988811111", "-3.7462000", "-73.2456000"),
                clientRow("Cliente Ruta Belén", "73000002", "Mercado de Belén", "Cobrar contra entrega", "+51988822222", "-3.7604000", "-73.2476000"),
                clientRow("Cliente Ruta San Juan", "73000003", "Av. Participación", "Reprogramar si no responde", "+51988833333", "-3.7800000", "-73.3010000")
        ));
        appendDeliveryZones(sql);
        appendDeliveryImportDemo(sql, "Ruta demo courier", "Repartidor demo", List.of(
                stopRow(1, "Cliente Ruta Centro", "+51988811111", "Plaza 28 de Julio", "Punto de referencia: esquina principal", "12.00", "-3.7462000", "-73.2456000"),
                stopRow(2, "Cliente Ruta Belén", "+51988822222", "Mercado de Belén", "Ingreso por zona comercial", "18.00", "-3.7604000", "-73.2476000"),
                stopRow(3, "Cliente Ruta San Juan", "+51988833333", "Av. Participación", "Confirmar antes de salir", "20.00", "-3.7800000", "-73.3010000")
        ));
    }

    private void appendSelvaDemoData(StringBuilder sql) {
        appendProducts(sql, List.of(
                productRow("Paiche seco seleccionado", "Producto amazónico para platos regionales y venta por encargo.", "/img/catalog/selva-paiche.jpg", "35.00", true, "20", "4"),
                productRow("Hoja de bijao para juanes", "Hojas listas para preparación tradicional según disponibilidad.", "/img/catalog/selva-bijao.jpg", "8.00", true, "80", "20"),
                productRow("Camu camu congelado", "Fruta amazónica para refrescos, jugos y negocio gastronómico.", "/img/catalog/selva-camu-camu.jpg", "15.00", false, "45", "10")
        ));
        appendClients(sql, List.of(
                clientRow("Restaurante amazónico demo", "74000001", "Centro de Iquitos", "Compra productos regionales por semana", "+51999911111", "-3.7486000", "-73.2455000"),
                clientRow("Cliente familiar demo", "74000002", "San Juan", "Consulta disponibilidad de bijao", "+51999922222", "-3.7770000", "-73.2850000")
        ));
        appendMarketing(sql,
                "Campaña sabores de la selva",
                "Contenido con origen, preparación y usos de productos amazónicos.",
                "Productos amazónicos seleccionados para tu mesa o negocio.",
                "Consultar disponibilidad");
    }

    private void appendAguaEcoDemoData(StringBuilder sql) {
        appendProducts(sql, List.of(
                productRow("Bidón de agua 20 L", "Agua purificada para hogar, empresa y restaurante.", "/img/productos/bidon-20l.png", "3.00", true, "120", "20"),
                productRow("Pack oficina semanal", "Plan de agua para oficinas con reparto coordinado.", "/img/productos/pack-oficina.png", "25.00", true, "30", "5")
        ));
        appendClients(sql, List.of(
                clientRow("Familia demo Los Delfines", "75000001", "Los Delfines, Iquitos", "Compra semanal", "+51900011111", "-3.7457000", "-73.2608000"),
                clientRow("Restaurante demo centro", "75000002", "Centro de Iquitos", "Precio restaurante", "+51900022222", "-3.7480000", "-73.2440000")
        ));
        appendDeliveryZones(sql);
        appendMarketing(sql,
                "Recompra semanal de bidones",
                "Recordar a clientes frecuentes sus pedidos de agua purificada.",
                "¿Necesitas agua para hoy? Coordinamos tu entrega por WhatsApp.",
                "Pedir agua");
    }


    private void appendRestaurantSchema(StringBuilder sql) {
        sql.append("\n-- Estructura operativa del módulo Restaurante.\n")
                .append("CREATE TABLE IF NOT EXISTS restaurant_table (\n")
                .append("    id BIGINT NOT NULL AUTO_INCREMENT,\n")
                .append("    code VARCHAR(50) NOT NULL,\n")
                .append("    name VARCHAR(120) NOT NULL,\n")
                .append("    area VARCHAR(120) NULL,\n")
                .append("    seats INT NOT NULL DEFAULT 0,\n")
                .append("    status VARCHAR(30) NOT NULL DEFAULT 'FREE',\n")
                .append("    active TINYINT(1) NOT NULL DEFAULT 1,\n")
                .append("    notes TEXT NULL,\n")
                .append("    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,\n")
                .append("    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n")
                .append("    PRIMARY KEY (id),\n")
                .append("    UNIQUE KEY uk_restaurant_table_code (code),\n")
                .append("    KEY idx_restaurant_table_status (status),\n")
                .append("    KEY idx_restaurant_table_area (area)\n")
                .append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n")
                .append("CREATE TABLE IF NOT EXISTS restaurant_order (\n")
                .append("    id BIGINT NOT NULL AUTO_INCREMENT,\n")
                .append("    order_code VARCHAR(80) NOT NULL,\n")
                .append("    service_type VARCHAR(30) NOT NULL DEFAULT 'DINE_IN',\n")
                .append("    table_id BIGINT NULL,\n")
                .append("    customer_name VARCHAR(180) NULL,\n")
                .append("    customer_phone VARCHAR(40) NULL,\n")
                .append("    status VARCHAR(30) NOT NULL DEFAULT 'NEW',\n")
                .append("    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,\n")
                .append("    notes TEXT NULL,\n")
                .append("    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,\n")
                .append("    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n")
                .append("    PRIMARY KEY (id),\n")
                .append("    UNIQUE KEY uk_restaurant_order_code (order_code),\n")
                .append("    KEY idx_restaurant_order_status (status),\n")
                .append("    KEY idx_restaurant_order_created (created_at),\n")
                .append("    KEY idx_restaurant_order_table (table_id),\n")
                .append("    CONSTRAINT fk_restaurant_order_table FOREIGN KEY (table_id) REFERENCES restaurant_table(id) ON DELETE SET NULL\n")
                .append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n")
                .append("CREATE TABLE IF NOT EXISTS restaurant_order_item (\n")
                .append("    id BIGINT NOT NULL AUTO_INCREMENT,\n")
                .append("    order_id BIGINT NOT NULL,\n")
                .append("    product_id BIGINT NULL,\n")
                .append("    product_name VARCHAR(200) NOT NULL,\n")
                .append("    quantity INT NOT NULL DEFAULT 1,\n")
                .append("    unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,\n")
                .append("    line_total DECIMAL(10,2) NOT NULL DEFAULT 0.00,\n")
                .append("    kitchen_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',\n")
                .append("    PRIMARY KEY (id),\n")
                .append("    KEY idx_restaurant_order_item_order (order_id),\n")
                .append("    KEY idx_restaurant_order_item_product (product_id),\n")
                .append("    CONSTRAINT fk_restaurant_order_item_order FOREIGN KEY (order_id) REFERENCES restaurant_order(id) ON DELETE CASCADE\n")
                .append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n\n");
    }

    private void appendRestaurantTablesAndSampleOrder(StringBuilder sql) {
        sql.append("-- Mesas demo para restaurante.\n")
                .append("INSERT INTO restaurant_table (`code`, `name`, `area`, `seats`, `status`, `active`, `notes`)\n")
                .append("SELECT 'MESA-01', 'Mesa 01', 'Salón principal', 4, 'FREE', true, 'Mesa demo cerca a caja'\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM restaurant_table WHERE code = 'MESA-01');\n")
                .append("INSERT INTO restaurant_table (`code`, `name`, `area`, `seats`, `status`, `active`, `notes`)\n")
                .append("SELECT 'MESA-02', 'Mesa 02', 'Salón principal', 4, 'OCCUPIED', true, 'Mesa demo con comanda activa'\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM restaurant_table WHERE code = 'MESA-02');\n")
                .append("INSERT INTO restaurant_table (`code`, `name`, `area`, `seats`, `status`, `active`, `notes`)\n")
                .append("SELECT 'MESA-03', 'Mesa 03', 'Terraza', 6, 'RESERVED', true, 'Reserva familiar de prueba'\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM restaurant_table WHERE code = 'MESA-03');\n")
                .append("INSERT INTO restaurant_table (`code`, `name`, `area`, `seats`, `status`, `active`, `notes`)\n")
                .append("SELECT 'MESA-04', 'Mesa 04', 'Salón principal', 2, 'FREE', true, 'Mesa demo para pareja'\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM restaurant_table WHERE code = 'MESA-04');\n")
                .append("SET @restaurant_demo_table_id = (SELECT id FROM restaurant_table WHERE code = 'MESA-02' LIMIT 1);\n")
                .append("SET @restaurant_demo_product_id = (SELECT id FROM product WHERE active = true ORDER BY featured DESC, id ASC LIMIT 1);\n")
                .append("SET @restaurant_demo_product_name = (SELECT name FROM product WHERE id = @restaurant_demo_product_id LIMIT 1);\n")
                .append("SET @restaurant_demo_product_price = (SELECT price FROM product WHERE id = @restaurant_demo_product_id LIMIT 1);\n")
                .append("INSERT INTO restaurant_order (`order_code`, `service_type`, `table_id`, `customer_name`, `customer_phone`, `status`, `subtotal`, `notes`)\n")
                .append("SELECT 'CMD-DEMO-001', 'DINE_IN', @restaurant_demo_table_id, 'Cliente demo salón', '+51966666666', 'IN_KITCHEN', COALESCE(@restaurant_demo_product_price, 18.00), 'Comanda demo para cocina'\n")
                .append("WHERE @restaurant_demo_table_id IS NOT NULL AND @restaurant_demo_product_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM restaurant_order WHERE order_code = 'CMD-DEMO-001');\n")
                .append("SET @restaurant_demo_order_id = (SELECT id FROM restaurant_order WHERE order_code = 'CMD-DEMO-001' LIMIT 1);\n")
                .append("INSERT INTO restaurant_order_item (`order_id`, `product_id`, `product_name`, `quantity`, `unit_price`, `line_total`, `kitchen_status`)\n")
                .append("SELECT @restaurant_demo_order_id, @restaurant_demo_product_id, COALESCE(@restaurant_demo_product_name, 'Plato demo'), 1, COALESCE(@restaurant_demo_product_price, 18.00), COALESCE(@restaurant_demo_product_price, 18.00), 'PENDING'\n")
                .append("WHERE @restaurant_demo_order_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM restaurant_order_item WHERE order_id = @restaurant_demo_order_id);\n\n");
    }

    private record ProductSeed(String name, String description, String imagePath, String price, boolean featured, String stock, String minimumStock) {}
    private record ClientSeed(String name, String docNumber, String address, String reference, String phone, String latitude, String longitude) {}
    private record StopSeed(int order, String clientName, String phone, String address, String reference, String amount, String latitude, String longitude) {}

    private ProductSeed productRow(String name, String description, String imagePath, String price, boolean featured, String stock, String minimumStock) {
        return new ProductSeed(name, description, imagePath, price, featured, stock, minimumStock);
    }

    private ClientSeed clientRow(String name, String docNumber, String address, String reference, String phone, String latitude, String longitude) {
        return new ClientSeed(name, docNumber, address, reference, phone, latitude, longitude);
    }

    private StopSeed stopRow(int order, String clientName, String phone, String address, String reference, String amount, String latitude, String longitude) {
        return new StopSeed(order, clientName, phone, address, reference, amount, latitude, longitude);
    }

    private void appendProducts(StringBuilder sql, List<ProductSeed> products) {
        sql.append("-- Productos demo.\n");
        for (ProductSeed product : products) {
            sql.append("INSERT INTO product (`name`, `description`, `image_path`, `price`, `active`, `featured`, `stock`, `minimum_stock`)\n")
                    .append("SELECT '").append(sql(product.name())).append("', '").append(sql(product.description())).append("', '").append(sql(product.imagePath())).append("', ")
                    .append(product.price()).append(", true, ").append(product.featured()).append(", ").append(product.stock()).append(", ").append(product.minimumStock()).append("\n")
                    .append("WHERE NOT EXISTS (SELECT 1 FROM product WHERE `name` = '").append(sql(product.name())).append("');\n");
        }
        sql.append("\n");
    }

    private void appendClients(StringBuilder sql, List<ClientSeed> clients) {
        sql.append("-- Clientes demo.\n");
        for (ClientSeed client : clients) {
            sql.append("INSERT INTO client (`name`, `doc_type`, `doc_number`, `address`, `reference`, `phone`, `active`, `registration_date`, `latitude`, `longitude`)\n")
                    .append("SELECT '").append(sql(client.name())).append("', 'DNI', '").append(sql(client.docNumber())).append("', '")
                    .append(sql(client.address())).append("', '").append(sql(client.reference())).append("', '").append(sql(client.phone())).append("', true, @demo_now, ")
                    .append(client.latitude()).append(", ").append(client.longitude()).append("\n")
                    .append("WHERE NOT EXISTS (SELECT 1 FROM client WHERE `doc_number` = '").append(sql(client.docNumber())).append("');\n");
        }
        sql.append("\n");
    }

    private void appendDeliveryZones(StringBuilder sql) {
        sql.append("-- Zonas demo de entrega.\n")
                .append("INSERT INTO delivery_zone (`name`, `latitude`, `longitude`, `radius_meters`, `note`, `created_at`, `updated_at`)\n")
                .append("SELECT 'Centro Iquitos', -3.7481000, -73.2448000, 2500, 'Zona demo para entregas céntricas', @demo_now, @demo_now\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM delivery_zone WHERE `name` = 'Centro Iquitos');\n")
                .append("INSERT INTO delivery_zone (`name`, `latitude`, `longitude`, `radius_meters`, `note`, `created_at`, `updated_at`)\n")
                .append("SELECT 'San Juan / Aeropuerto', -3.7840000, -73.3080000, 3500, 'Zona demo para entregas extendidas', @demo_now, @demo_now\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM delivery_zone WHERE `name` = 'San Juan / Aeropuerto');\n\n");
    }

    private void appendMarketing(StringBuilder sql, String campaign, String objective, String hook, String callToAction) {
        sql.append("-- Marketing demo.\n")
                .append("INSERT INTO marketing_campaign_calendar (`name`, `type`, `status`, `start_date`, `end_date`, `channel`, `target_segment`, `objective`, `main_message`, `next_action`, `observations`, `created_at`, `updated_at`)\n")
                .append("SELECT '").append(sql(campaign)).append("', 'WHATSAPP', 'PLANNED', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'WhatsApp / Redes sociales', 'Clientes potenciales', '")
                .append(sql(objective)).append("', '").append(sql(hook)).append("', '").append(sql(callToAction)).append("', 'Dato demo generado por plantilla.', @demo_now, @demo_now\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM marketing_campaign_calendar WHERE `name` = '").append(sql(campaign)).append("');\n")
                .append("INSERT INTO marketing_content_idea (`title`, `channel`, `content_type`, `status`, `priority`, `suggested_date`, `target_segment`, `hook`, `main_message`, `call_to_action`, `next_action`, `observations`, `created_at`, `updated_at`)\n")
                .append("SELECT 'Idea demo: ").append(sql(campaign)).append("', 'WHATSAPP', 'SHORT_VIDEO', 'NEW', 'HIGH', CURDATE(), 'Clientes potenciales', '")
                .append(sql(hook)).append("', '").append(sql(objective)).append("', '").append(sql(callToAction)).append("', 'Crear pieza visual y publicar.', 'Dato demo generado por plantilla.', @demo_now, @demo_now\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM marketing_content_idea WHERE `title` = 'Idea demo: ").append(sql(campaign)).append("');\n\n");
    }

    private void appendDeliveryImportDemo(StringBuilder sql, String title, String person, List<StopSeed> stops) {
        sql.append("-- Ruta importada demo.\n")
                .append("INSERT INTO delivery_import_batch (`route_date`, `title`, `source_filename`, `delivery_person`, `total_stops`, `located_stops`, `missing_location_stops`, `created_at`)\n")
                .append("SELECT CURDATE(), '").append(sql(title)).append("', 'demo-template.csv', '").append(sql(person)).append("', ").append(stops.size()).append(", ").append(stops.size()).append(", 0, @demo_now\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM delivery_import_batch WHERE `title` = '").append(sql(title)).append("' AND `route_date` = CURDATE());\n")
                .append("SET @demo_batch_id = (SELECT id FROM delivery_import_batch WHERE `title` = '").append(sql(title)).append("' AND `route_date` = CURDATE() LIMIT 1);\n");
        for (StopSeed stop : stops) {
            sql.append("INSERT INTO delivery_import_stop (`batch_id`, `route_order_index`, `client_name`, `phone`, `address`, `reference`, `amount`, `observation`, `latitude`, `longitude`, `status`, `updated_at`)\n")
                    .append("SELECT @demo_batch_id, ").append(stop.order()).append(", '").append(sql(stop.clientName())).append("', '").append(sql(stop.phone())).append("', '")
                    .append(sql(stop.address())).append("', '").append(sql(stop.reference())).append("', ").append(stop.amount()).append(", 'Parada demo generada por plantilla', ")
                    .append(stop.latitude()).append(", ").append(stop.longitude()).append(", 'PENDING', @demo_now\n")
                    .append("WHERE @demo_batch_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM delivery_import_stop WHERE batch_id = @demo_batch_id AND route_order_index = ").append(stop.order()).append(");\n");
        }
        sql.append("\n");
    }

    private String templateCode(PlatformBusinessClient client) {
        StringBuilder key = new StringBuilder();
        if (client.getTemplate() != null) {
            key.append(valueOrEmpty(client.getTemplate().getCode())).append(' ')
                    .append(valueOrEmpty(client.getTemplate().getName())).append(' ')
                    .append(valueOrEmpty(client.getTemplate().getBusinessType())).append(' ');
        }
        key.append(valueOrEmpty(client.getBusinessType())).append(' ')
                .append(valueOrEmpty(client.getCode())).append(' ')
                .append(valueOrEmpty(client.getBusinessName()));
        return key.toString().toLowerCase(Locale.ROOT);
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


    private void appendInitialAdminUser(StringBuilder sql) {
        String passwordHash = passwordEncoder.encode("Demo12345");

        sql.append("\n-- Usuario administrador inicial de la instancia.\n")
                .append("INSERT INTO `roles` (`variable`, `title`)\n")
                .append("SELECT 'ROLE_OWNER', 'Propietario / Administrador'\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM `roles` WHERE `variable` = 'ROLE_OWNER');\n")
                .append("INSERT INTO `roles` (`variable`, `title`)\n")
                .append("SELECT 'ADMIN_PRINC', 'Administrador principal legacy'\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM `roles` WHERE `variable` = 'ADMIN_PRINC');\n")
                .append("SET @initial_admin_password = '").append(sql(passwordHash)).append("';\n")
                .append("INSERT INTO `user` (`username`, `password`, `active`, `rol`, `registration_date`)\n")
                .append("SELECT 'admin_demo', @initial_admin_password, 1, 1, NOW()\n")
                .append("WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'admin_demo');\n")
                .append("UPDATE `user` SET `password` = @initial_admin_password, `active` = 1, `rol` = 1 WHERE `username` = 'admin_demo';\n")
                .append("SET @initial_admin_user_id = (SELECT `id` FROM `user` WHERE `username` = 'admin_demo' LIMIT 1);\n")
                .append("SET @initial_owner_role_id = (SELECT `id` FROM `roles` WHERE `variable` = 'ROLE_OWNER' LIMIT 1);\n")
                .append("SET @initial_legacy_admin_role_id = (SELECT `id` FROM `roles` WHERE `variable` = 'ADMIN_PRINC' LIMIT 1);\n")
                .append("INSERT INTO `user_roles` (`user_id`, `rol_id`)\n")
                .append("SELECT @initial_admin_user_id, @initial_owner_role_id\n")
                .append("WHERE @initial_admin_user_id IS NOT NULL AND @initial_owner_role_id IS NOT NULL\n")
                .append("AND NOT EXISTS (SELECT 1 FROM `user_roles` WHERE `user_id` = @initial_admin_user_id AND `rol_id` = @initial_owner_role_id);\n")
                .append("INSERT INTO `user_roles` (`user_id`, `rol_id`)\n")
                .append("SELECT @initial_admin_user_id, @initial_legacy_admin_role_id\n")
                .append("WHERE @initial_admin_user_id IS NOT NULL AND @initial_legacy_admin_role_id IS NOT NULL\n")
                .append("AND NOT EXISTS (SELECT 1 FROM `user_roles` WHERE `user_id` = @initial_admin_user_id AND `rol_id` = @initial_legacy_admin_role_id);\n")
                .append("-- Credenciales demo iniciales: admin_demo / Demo12345. Cambiar la clave después de validar la instancia.\n\n");
    }

    private List<String> listBaseTables(String databaseName) {
        return jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME",
                String.class,
                databaseName
        );
    }

    private String showCreateTable(String databaseName, String tableName) {
        String sql = "SHOW CREATE TABLE `" + quoteIdentifier(databaseName) + "`.`" + quoteIdentifier(tableName) + "`";
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return rs.getString(2);
            }
            throw new IllegalArgumentException("No se pudo leer la estructura de " + tableName + ".");
        });
    }

    private String toTargetCreateTableSql(String ddl, String targetDatabase) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(ddl.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("DDL no compatible para copia automática.");
        }
        return matcher.replaceFirst("CREATE TABLE IF NOT EXISTS `" + quoteIdentifier(targetDatabase) + "`.`" + matcher.group(1) + "`");
    }

    private void useDatabase(String databaseName) {
        jdbcTemplate.execute("USE `" + quoteIdentifier(normalizedDatabaseName(databaseName)) + "`");
    }

    private List<String> splitSqlStatements(String sqlText) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        char previous = 0;

        for (int i = 0; i < sqlText.length(); i++) {
            char ch = sqlText.charAt(i);
            if (ch == '\'' && !inDoubleQuote && previous != '\\') {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote && previous != '\\') {
                inDoubleQuote = !inDoubleQuote;
            }

            if (ch == ';' && !inSingleQuote && !inDoubleQuote) {
                addStatement(statements, current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
            previous = ch;
        }
        addStatement(statements, current.toString());
        return statements;
    }

    private void addStatement(List<String> statements, String raw) {
        String clean = raw.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("--"))
                .reduce("", (left, right) -> left.isBlank() ? right : left + System.lineSeparator() + right)
                .trim();
        if (!clean.isBlank()) {
            statements.add(clean);
        }
    }

    private String runtimeReadme(PlatformRuntimePlan runtime, PlatformProvisioningPlan plan) {
        return "Runtime generado para " + runtime.client().getBusinessName() + System.lineSeparator()
                + "Perfil: " + runtime.runtimeProfile() + System.lineSeparator()
                + "Puerto: " + runtime.runtimePort() + System.lineSeparator()
                + "URL local: " + runtime.localUrl() + System.lineSeparator()
                + "Base de datos: " + runtime.client().getDatabaseName() + System.lineSeparator()
                + System.lineSeparator()
                + "Ejecutar:" + System.lineSeparator()
                + "bash runtime-clients/" + runtime.runtimeProfile() + "/run.sh" + System.lineSeparator()
                + System.lineSeparator()
                + "Archivos incluidos:" + System.lineSeparator()
                + "- application.properties" + System.lineSeparator()
                + "- run.sh" + System.lineSeparator()
                + "- " + plan.createDatabaseFileName() + System.lineSeparator()
                + "- " + plan.bootstrapFileName() + System.lineSeparator()
                + "- " + plan.demoDataFileName() + System.lineSeparator();
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void markExecutable(Path path) {
        try {
            Files.setPosixFilePermissions(path, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE
            ));
        } catch (Exception ignored) {
            // Windows filesystems do not support POSIX permissions; chmod can be used manually if needed.
        }
    }

    private Path runtimeFolder(String profile) {
        return Paths.get(runtimeClientsDirectory).resolve(profile).toAbsolutePath().normalize();
    }

    private int suggestedPort(PlatformBusinessClient client) {
        long id = client.getId() == null ? 1L : client.getId();
        int offset = (int) Math.min(Math.max(id, 1L), 500L);
        return 8081 + offset;
    }

    private void saveLog(PlatformBusinessClient client, String action, String status, String details, String sqlSnippet) {
        PlatformProvisioningLog log = new PlatformProvisioningLog();
        log.setClient(client);
        log.setAction(action);
        log.setStatus(status);
        log.setDetails(details);
        log.setSqlSnippet(sqlSnippet);
        logRepository.saveAndFlush(log);
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
        return "Ahora puedes ejecutar la instalación por pasos desde la plataforma. Los SQL manuales quedan como respaldo.";
    }

    private String statusTitle(boolean databaseCreated, boolean structureReady, boolean bootstrapApplied, boolean demoDataRequested, boolean demoDataLoaded, boolean active, boolean ready) {
        if (ready) {
            return "Negocio listo";
        }
        if (bootstrapApplied && demoDataRequested && !demoDataLoaded) {
            return "Configuración aplicada, faltan datos demo";
        }
        if (bootstrapApplied && !active) {
            return "Configuración aplicada, falta activar";
        }
        if (structureReady && !bootstrapApplied) {
            return "Estructura copiada, falta bootstrap";
        }
        if (databaseCreated) {
            return "Base creada, falta copiar estructura";
        }
        return "Pendiente de aprovisionamiento";
    }

    private String statusDescription(boolean databaseCreated, boolean structureReady, boolean bootstrapApplied, boolean demoDataRequested, boolean demoDataLoaded, boolean active, boolean ready, boolean runtimeFilesGenerated) {
        if (ready && runtimeFilesGenerated) {
            return "La base, estructura, configuración inicial, datos demo y archivos runtime están listos para ejecutar el negocio.";
        }
        if (ready) {
            return "La base, estructura, configuración inicial y datos demo fueron marcados como listos. Genera los archivos runtime para levantar la instancia.";
        }
        if (bootstrapApplied && demoDataRequested && !demoDataLoaded) {
            return "La configuración inicial ya fue aplicada. Ahora carga datos demo según la plantilla del negocio.";
        }
        if (bootstrapApplied && !active) {
            return "La configuración inicial ya fue aplicada. Ahora activa el negocio para demo o pruebas internas.";
        }
        if (structureReady && !bootstrapApplied) {
            return "La estructura ya existe en la base destino. Aplica el bootstrap automático o usa el SQL manual.";
        }
        if (databaseCreated) {
            return "La base vacía ya existe. Copia la estructura del sistema base hacia la base del cliente.";
        }
        return "Revisa los datos del negocio y ejecuta los pasos automáticos de izquierda a derecha.";
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

    private String normalizeRuntimeProfile(String value) {
        String normalized = valueOrEmpty(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized.isBlank() ? "cliente_demo" : normalized;
    }

    private String quoteIdentifier(String value) {
        String clean = valueOrEmpty(value).trim();
        if (!clean.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Identificador MySQL inválido: " + clean);
        }
        return clean;
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
