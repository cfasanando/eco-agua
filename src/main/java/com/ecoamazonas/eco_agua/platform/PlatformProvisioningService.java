package com.ecoamazonas.eco_agua.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
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
            "DATABASE_CREATED", "STRUCTURE_READY", "BOOTSTRAP_APPLIED", "READY", "CREATED"
    );
    private static final Set<String> STRUCTURE_READY_STATUSES = Set.of(
            "STRUCTURE_READY", "BOOTSTRAP_APPLIED", "READY", "CREATED"
    );
    private static final Set<String> BOOTSTRAP_READY_STATUSES = Set.of(
            "BOOTSTRAP_APPLIED", "READY", "CREATED"
    );
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?is)^CREATE\\s+TABLE\\s+`([^`]+)`");

    private final PlatformBusinessClientRepository clientRepository;
    private final PlatformClientModuleRepository clientModuleRepository;
    private final PlatformModuleCatalogRepository moduleRepository;
    private final PlatformProvisioningLogRepository logRepository;
    private final PlatformRuntimeService runtimeService;
    private final JdbcTemplate jdbcTemplate;
    private final String sourceDatabaseName;
    private final String runtimeClientsDirectory;

    public PlatformProvisioningService(PlatformBusinessClientRepository clientRepository,
                                       PlatformClientModuleRepository clientModuleRepository,
                                       PlatformModuleCatalogRepository moduleRepository,
                                       PlatformProvisioningLogRepository logRepository,
                                       PlatformRuntimeService runtimeService,
                                       JdbcTemplate jdbcTemplate,
                                       @Value("${ecoagua.platform.source-database:productos_selva_belen}") String sourceDatabaseName,
                                       @Value("${ecoagua.platform.runtime-clients-dir:runtime-clients}") String runtimeClientsDirectory) {
        this.clientRepository = clientRepository;
        this.clientModuleRepository = clientModuleRepository;
        this.moduleRepository = moduleRepository;
        this.logRepository = logRepository;
        this.runtimeService = runtimeService;
        this.jdbcTemplate = jdbcTemplate;
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
        boolean active = "ACTIVE".equalsIgnoreCase(safe(client.getStatus())) || "READY".equals(databaseStatus) || "CREATED".equals(databaseStatus);
        boolean ready = active && bootstrapApplied;
        String databaseName = normalizedDatabaseName(client.getDatabaseName());
        String bootstrapFileName = "bootstrap-" + databaseName + ".sql";
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
        steps.add(step(5, "Generar runtime y activar", ready,
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
                bootstrapApplied && !active,
                bootstrapApplied && active && !runtimeFilesGenerated,
                warningFor(client, ready),
                databaseCreated,
                structureReady,
                bootstrapApplied,
                active,
                ready,
                statusTitle(databaseCreated, structureReady, bootstrapApplied, active, ready),
                statusDescription(databaseCreated, structureReady, bootstrapApplied, active, ready, runtimeFilesGenerated),
                ready ? "text-bg-success" : bootstrapApplied ? "text-bg-info" : databaseCreated ? "text-bg-warning" : "text-bg-secondary",
                ready ? "alert-success" : bootstrapApplied ? "alert-info" : databaseCreated ? "alert-warning" : "alert-info",
                bootstrapFileName,
                createDatabaseFileName,
                String.join(System.lineSeparator(), commands),
                openBusinessUrl(client),
                runtimeFolderPath.toString(),
                applicationPath,
                runScriptPath
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
    public void copyStructureAutomatically(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
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
            if (!"ACTIVE".equalsIgnoreCase(safe(client.getStatus()))) {
                client.setStatus("PROVISIONING");
            }
            clientRepository.save(client);
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
            if (!"ACTIVE".equalsIgnoreCase(safe(client.getStatus()))) {
                client.setStatus("PROVISIONING");
            }
            clientRepository.save(client);
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
    public void generateRuntimeFiles(Long clientId) {
        PlatformBusinessClient client = getClient(clientId);
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
            writeFile(folder.resolve("README.txt"), runtimeReadme(runtime, provisioningPlan));
            markExecutable(folder.resolve("run.sh"));

            PlatformBusinessClient updatedClient = getClient(clientId);
            updatedClient.setRuntimeStatus("FILES_GENERATED");
            updatedClient.setLastRuntimeGeneratedAt(java.time.LocalDateTime.now());
            clientRepository.save(updatedClient);
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
                "mysql -u root -p " + db + " < bootstrap-" + db + ".sql",
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
        sql.append("-- Recomendado: crear desde la pantalla Usuarios del sistema destino o copiar usuarios demo desde la base modelo.\n");
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
                + "- " + plan.bootstrapFileName() + System.lineSeparator();
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
        return "Ahora puedes ejecutar la instalación por pasos desde la plataforma. Los SQL manuales quedan como respaldo.";
    }

    private String statusTitle(boolean databaseCreated, boolean structureReady, boolean bootstrapApplied, boolean active, boolean ready) {
        if (ready) {
            return "Negocio listo";
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

    private String statusDescription(boolean databaseCreated, boolean structureReady, boolean bootstrapApplied, boolean active, boolean ready, boolean runtimeFilesGenerated) {
        if (ready && runtimeFilesGenerated) {
            return "La base, estructura, configuración inicial y archivos runtime están listos para ejecutar el negocio.";
        }
        if (ready) {
            return "La base, estructura y configuración inicial fueron marcadas como listas. Genera los archivos runtime para levantar la instancia.";
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
