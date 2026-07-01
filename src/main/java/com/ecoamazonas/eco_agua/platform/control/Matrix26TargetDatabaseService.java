package com.ecoamazonas.eco_agua.platform.control;

import com.ecoamazonas.eco_agua.config.SystemModuleVisibilityMapper;
import com.ecoamazonas.eco_agua.platform.control.appearance.Matrix26JsonCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26TargetDatabaseService {

    private static final Pattern DATABASE_URL_PATTERN = Pattern.compile("^(jdbc:mysql://[^/]+/)([^?]*)(\\?.*)?$");
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "(?is)^CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`([^`]+)`"
    );
    private static final Pattern FOREIGN_KEY_CONSTRAINT_PATTERN = Pattern.compile(
            "(?is)^CONSTRAINT\\s+`([^`]+)`\\s+FOREIGN\\s+KEY\\b"
    );
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]+$");

    private final JdbcTemplate controlJdbcTemplate;
    private final Matrix26ControlCenterProperties properties;
    private final String controlJdbcUrl;
    private final String databaseUsername;
    private final String databasePassword;
    private final String databaseDriver;

    public Matrix26TargetDatabaseService(
            JdbcTemplate controlJdbcTemplate,
            Matrix26ControlCenterProperties properties,
            @Value("${spring.datasource.url}") String controlJdbcUrl,
            @Value("${spring.datasource.username}") String databaseUsername,
            @Value("${spring.datasource.password:}") String databasePassword,
            @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}") String databaseDriver
    ) {
        this.controlJdbcTemplate = controlJdbcTemplate;
        this.properties = properties;
        this.controlJdbcUrl = controlJdbcUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.databaseDriver = databaseDriver;
    }

    public boolean databaseExists(String databaseName) {
        String safeName = safeIdentifier(databaseName);
        Integer count = controlJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?",
                Integer.class,
                safeName
        );
        return count != null && count > 0;
    }

    public int tableCount(String databaseName) {
        String safeName = safeIdentifier(databaseName);
        Integer count = controlJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'",
                Integer.class,
                safeName
        );
        return count == null ? 0 : count;
    }

    public void createDatabase(String databaseName, boolean allowExisting) {
        String safeName = safeIdentifier(databaseName);
        boolean exists = databaseExists(safeName);
        if (exists && !allowExisting && tableCount(safeName) > 0) {
            throw new IllegalArgumentException(
                    "La base " + safeName + " ya existe y contiene tablas. Matrix26 no la reutilizará automáticamente."
            );
        }
        controlJdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS `" + quoteIdentifier(safeName)
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    }

    public JdbcTemplate targetJdbcTemplate(String databaseName) {
        String safeName = safeIdentifier(databaseName);
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(databaseDriver);
        dataSource.setUrl(targetJdbcUrl(safeName));
        dataSource.setUsername(databaseUsername);
        dataSource.setPassword(databasePassword);
        return new JdbcTemplate(dataSource);
    }

    public int installCompatibleCore(Matrix26ProvisioningJob job, Set<String> selectedModules) {
        String templateDatabase = safeIdentifier(properties.getProvisioningTemplateDatabase());
        String targetDatabase = safeIdentifier(job.getDatabaseName());

        if (!databaseExists(templateDatabase)) {
            throw new IllegalArgumentException(
                    "La base plantilla " + templateDatabase + " no existe. No se modificó la base destino."
            );
        }

        JdbcTemplate target = targetJdbcTemplate(targetDatabase);
        List<String> tables = compatibleCoreTables(templateDatabase);
        if (tables.isEmpty()) {
            throw new IllegalStateException("No se encontraron tablas compatibles en la base plantilla.");
        }

        List<TableCopyDefinition> definitions = new ArrayList<>();
        List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();
        for (String table : tables) {
            String ddl = toTargetCreateTableSql(showCreateTable(templateDatabase, table));
            TableCopyDefinition definition = separateForeignKeys(table, ddl);
            definitions.add(definition);
            foreignKeys.addAll(definition.foreignKeys());
        }

        try {
            target.execute("SET FOREIGN_KEY_CHECKS=0");
            for (TableCopyDefinition definition : definitions) {
                target.execute(definition.createTableSql());
            }
            for (ForeignKeyDefinition foreignKey : foreignKeys) {
                if (!foreignKeyExists(target, foreignKey.tableName(), foreignKey.constraintName())) {
                    target.execute(foreignKey.alterTableSql());
                }
            }
        } finally {
            target.execute("SET FOREIGN_KEY_CHECKS=1");
        }

        seedBaseCatalog(target, selectedModules);
        seedBaseSettings(target, job, selectedModules);
        return tables.size();
    }

    public void createAdministrator(Matrix26ProvisioningJob job, String encodedPassword) {
        JdbcTemplate target = targetJdbcTemplate(job.getDatabaseName());
        target.update("""
                INSERT INTO roles (`variable`, `title`)
                VALUES ('ROLE_SUPER_ADMIN', 'Administrador general')
                ON DUPLICATE KEY UPDATE `title` = VALUES(`title`)
                """);
        target.update("""
                INSERT INTO roles (`variable`, `title`)
                VALUES ('ROLE_OWNER', 'Propietario / Administrador')
                ON DUPLICATE KEY UPDATE `title` = VALUES(`title`)
                """);
        target.update("""
                INSERT INTO `user` (`username`, `password`, `active`, `rol`, `registration_date`)
                VALUES (?, ?, 1, 1, NOW())
                ON DUPLICATE KEY UPDATE
                    `password` = VALUES(`password`),
                    `active` = 1,
                    `rol` = 1
                """, job.getAdminUsername(), encodedPassword);
        target.update("""
                INSERT INTO user_roles (`user_id`, `rol_id`)
                SELECT u.id, r.id
                FROM `user` u
                JOIN roles r ON r.variable IN ('ROLE_SUPER_ADMIN', 'ROLE_OWNER')
                WHERE u.username = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.rol_id = r.id
                  )
                """, job.getAdminUsername());
    }

    public void applyFinalBusinessSettings(Matrix26ProvisioningJob job, Set<String> selectedModules) {
        JdbcTemplate target = targetJdbcTemplate(job.getDatabaseName());
        seedBaseSettings(target, job, selectedModules);
        if (selectedModules.contains("restaurant")) {
            upsertSetting(target, "restaurant.identity.trade_name", job.getBusinessName(), "string", "restaurant", "Nombre comercial del restaurante");
            upsertSetting(target, "restaurant.identity.legal_name", defaultValue(job.getLegalName(), job.getBusinessName()), "string", "restaurant", "Razón social impresa en cuentas y recibos");
            upsertSetting(target, "restaurant.identity.address", defaultValue(job.getCity(), "Iquitos"), "string", "restaurant", "Dirección comercial");
        }
    }

    public String generateRuntimeFiles(Matrix26ProvisioningJob job, Set<String> selectedModules) {
        Path base = Paths.get(properties.getProvisioningRuntimeDirectory()).toAbsolutePath().normalize();
        Path folder = base.resolve(job.getRuntimeProfile()).normalize();
        if (!folder.startsWith(base)) {
            throw new IllegalArgumentException("El perfil runtime genera una ruta no permitida.");
        }

        try {
            Files.createDirectories(base);
            Path ownershipMarker = folder.resolve(".matrix26-provisioning-reference");
            if (Files.exists(folder)) {
                boolean emptyFolder;
                try (var entries = Files.list(folder)) {
                    emptyFolder = entries.findAny().isEmpty();
                }
                if (!emptyFolder) {
                    if (!Files.exists(ownershipMarker)) {
                        throw new IllegalArgumentException(
                                "La carpeta runtime " + folder + " ya existe y no pertenece a este plan."
                        );
                    }
                    String ownerReference = Files.readString(ownershipMarker, StandardCharsets.UTF_8).trim();
                    if (!job.getReferenceCode().equals(ownerReference)) {
                        throw new IllegalArgumentException(
                                "La carpeta runtime " + folder + " pertenece a otro plan de aprovisionamiento."
                        );
                    }
                }
            }
            Files.createDirectories(folder);
            writeFile(ownershipMarker, job.getReferenceCode() + System.lineSeparator());
            writeFile(folder.resolve("application.properties"), runtimeProperties(job, selectedModules));
            Path runScript = folder.resolve("run.sh");
            writeFile(runScript, runtimeScript(job));
            markExecutable(runScript);
            writeFile(folder.resolve("README.txt"), runtimeReadme(job));
            return folder.toString();
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudieron generar los archivos del runtime: " + safeMessage(ex), ex);
        }
    }

    private List<String> compatibleCoreTables(String templateDatabase) {
        List<String> sourceTables = controlJdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME",
                String.class,
                templateDatabase
        );
        List<String> result = new ArrayList<>();
        for (String table : sourceTables) {
            String normalized = table.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("restaurant_") || normalized.startsWith("matrix26_")) {
                continue;
            }
            result.add(table);
        }
        return result;
    }

    private String showCreateTable(String databaseName, String tableName) {
        String sql = "SHOW CREATE TABLE `" + quoteIdentifier(databaseName) + "`.`" + quoteIdentifier(tableName) + "`";
        return controlJdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                throw new IllegalArgumentException("No se pudo leer la estructura de " + tableName + ".");
            }
            return rs.getString(2);
        });
    }

    private String toTargetCreateTableSql(String ddl) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(ddl.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("DDL no compatible para copia estructural.");
        }
        String createSql = matcher.replaceFirst("CREATE TABLE IF NOT EXISTS `" + matcher.group(1) + "`");
        return createSql.replaceAll("(?i)AUTO_INCREMENT=\\d+", "AUTO_INCREMENT=1");
    }

    private TableCopyDefinition separateForeignKeys(String tableName, String ddl) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(ddl.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("DDL no compatible para copia estructural de " + tableName + ".");
        }

        int openingParenthesis = ddl.indexOf('(', matcher.end());
        if (openingParenthesis < 0) {
            throw new IllegalArgumentException("No se encontró la definición de columnas para " + tableName + ".");
        }
        int closingParenthesis = findMatchingParenthesis(ddl, openingParenthesis);
        if (closingParenthesis < 0) {
            throw new IllegalArgumentException("La definición estructural de " + tableName + " está incompleta.");
        }

        String body = ddl.substring(openingParenthesis + 1, closingParenthesis);
        List<String> retainedClauses = new ArrayList<>();
        List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();

        for (String clause : splitTopLevelClauses(body)) {
            String cleanClause = clause.trim();
            Matcher foreignKeyMatcher = FOREIGN_KEY_CONSTRAINT_PATTERN.matcher(cleanClause);
            if (foreignKeyMatcher.find()) {
                String constraintName = foreignKeyMatcher.group(1);
                String alterSql = "ALTER TABLE `" + quoteIdentifier(tableName) + "` ADD " + cleanClause;
                foreignKeys.add(new ForeignKeyDefinition(tableName, constraintName, alterSql));
            } else if (!cleanClause.isBlank()) {
                retainedClauses.add(cleanClause);
            }
        }

        if (retainedClauses.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron columnas válidas para " + tableName + ".");
        }

        String createSql = ddl.substring(0, openingParenthesis + 1)
                + System.lineSeparator()
                + "  "
                + String.join("," + System.lineSeparator() + "  ", retainedClauses)
                + System.lineSeparator()
                + ddl.substring(closingParenthesis);

        return new TableCopyDefinition(createSql, foreignKeys);
    }

    private int findMatchingParenthesis(String sql, int openingParenthesis) {
        int depth = 0;
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean backtickQuoted = false;

        for (int index = openingParenthesis; index < sql.length(); index++) {
            char current = sql.charAt(index);

            if (singleQuoted) {
                if (current == '\\' && index + 1 < sql.length()) {
                    index++;
                    continue;
                }
                if (current == '\'') {
                    singleQuoted = false;
                }
                continue;
            }
            if (doubleQuoted) {
                if (current == '\\' && index + 1 < sql.length()) {
                    index++;
                    continue;
                }
                if (current == '"') {
                    doubleQuoted = false;
                }
                continue;
            }
            if (backtickQuoted) {
                if (current == '`') {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == '`') {
                        index++;
                    } else {
                        backtickQuoted = false;
                    }
                }
                continue;
            }

            if (current == '\'') {
                singleQuoted = true;
            } else if (current == '"') {
                doubleQuoted = true;
            } else if (current == '`') {
                backtickQuoted = true;
            } else if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }

        return -1;
    }

    private List<String> splitTopLevelClauses(String body) {
        List<String> clauses = new ArrayList<>();
        StringBuilder currentClause = new StringBuilder();
        int depth = 0;
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean backtickQuoted = false;

        for (int index = 0; index < body.length(); index++) {
            char current = body.charAt(index);

            if (singleQuoted) {
                currentClause.append(current);
                if (current == '\\' && index + 1 < body.length()) {
                    currentClause.append(body.charAt(++index));
                } else if (current == '\'') {
                    singleQuoted = false;
                }
                continue;
            }
            if (doubleQuoted) {
                currentClause.append(current);
                if (current == '\\' && index + 1 < body.length()) {
                    currentClause.append(body.charAt(++index));
                } else if (current == '"') {
                    doubleQuoted = false;
                }
                continue;
            }
            if (backtickQuoted) {
                currentClause.append(current);
                if (current == '`') {
                    if (index + 1 < body.length() && body.charAt(index + 1) == '`') {
                        currentClause.append(body.charAt(++index));
                    } else {
                        backtickQuoted = false;
                    }
                }
                continue;
            }

            if (current == '\'') {
                singleQuoted = true;
                currentClause.append(current);
            } else if (current == '"') {
                doubleQuoted = true;
                currentClause.append(current);
            } else if (current == '`') {
                backtickQuoted = true;
                currentClause.append(current);
            } else if (current == '(') {
                depth++;
                currentClause.append(current);
            } else if (current == ')') {
                depth--;
                currentClause.append(current);
            } else if (current == ',' && depth == 0) {
                clauses.add(currentClause.toString());
                currentClause.setLength(0);
            } else {
                currentClause.append(current);
            }
        }

        if (!currentClause.isEmpty()) {
            clauses.add(currentClause.toString());
        }
        return clauses;
    }

    private boolean foreignKeyExists(JdbcTemplate target, String tableName, String constraintName) {
        Integer count = target.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND CONSTRAINT_NAME = ?
                  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
                """,
                Integer.class,
                tableName,
                constraintName
        );
        return count != null && count > 0;
    }

    private void seedBaseCatalog(JdbcTemplate target, Set<String> selectedModules) {
        upsertModule(target, "core", "Núcleo empresarial", "Plataforma", "Seguridad, configuración y funciones base.", 10);
        for (String moduleKey : selectedModules) {
            if ("restaurant".equals(moduleKey)) {
                upsertModule(target, "restaurant", "Restaurante", "Verticales", "Mesas, comandas, cocina, caja, reservas y carta QR.", 100);
            }
        }
    }

    private void seedBaseSettings(JdbcTemplate target, Matrix26ProvisioningJob job, Set<String> selectedModules) {
        String displayName = brandingValue(job, "displayName", job.getBusinessName());
        String shortName = brandingValue(job, "shortName", displayName);
        String tagline = brandingValue(job, "tagline", "Sistema empresarial administrado por Matrix26");
        String primaryLogo = job.isBrandingDemoAssetsEnabled()
                ? "/runtime-assets/logo-primary-v1.png"
                : "/img/logo3-transparente.png";
        String compactLogo = job.isBrandingDemoAssetsEnabled()
                ? "/runtime-assets/logo-compact-v1.png"
                : "/img/logo-eco.png";

        upsertSetting(target, "platform.name", displayName, "string", "platform", "Nombre de la instancia");
        upsertSetting(target, "platform.short_name", shortName, "string", "platform", "Nombre corto de la instancia");
        upsertSetting(target, "platform.tagline", tagline, "string", "platform", "Descripción de la instancia");
        upsertSetting(target, "platform.logo", primaryLogo, "string", "platform", "Logo principal");
        upsertSetting(target, "admin.brand.title", shortName, "string", "admin", "Título administrativo");
        upsertSetting(target, "admin.brand.subtitle", tagline, "string", "admin", "Subtítulo administrativo");
        upsertSetting(target, "admin.brand.logo", compactLogo, "string", "admin", "Logo administrativo");
        upsertSetting(target, "login.title", displayName, "string", "login", "Título de acceso");
        upsertSetting(target, "login.subtitle", brandingValue(job, "welcomeMessage", "Acceso al sistema de gestión"), "string", "login", "Subtítulo de acceso");
        Map<String, Boolean> moduleVisibility = SystemModuleVisibilityMapper.systemModuleFlags(
                selectedModules,
                selectedModules.contains("restaurant") || defaultValue(job.getBusinessType(), "").toLowerCase(Locale.ROOT).contains("restaurant")
                        || defaultValue(job.getBusinessType(), "").toLowerCase(Locale.ROOT).contains("restaurante")
        );
        moduleVisibility.forEach((moduleKey, enabled) -> upsertSetting(
                target,
                "module." + moduleKey + ".enabled",
                Boolean.toString(enabled),
                "boolean",
                "system_modules",
                "Matrix26 projected visibility flag for " + moduleKey
        ));
        upsertSetting(target, "public.nav.restaurant_label", "Carta", "string", "public_site", "Etiqueta de carta pública");
    }

    private void upsertModule(JdbcTemplate target, String key, String name, String area, String description, int order) {
        target.update("""
                INSERT INTO platform_module_catalog
                    (`module_key`, `name`, `area`, `description`, `default_enabled`, `configurable`, `active`, `display_order`, `created_at`, `updated_at`)
                VALUES (?, ?, ?, ?, 0, 1, 1, ?, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    `name` = VALUES(`name`),
                    `area` = VALUES(`area`),
                    `description` = VALUES(`description`),
                    `active` = 1,
                    `display_order` = VALUES(`display_order`),
                    `updated_at` = NOW()
                """, key, name, area, description, order);
    }

    private void upsertSetting(JdbcTemplate target, String variable, String value, String type, String category, String description) {
        target.update("""
                INSERT INTO platform_setting (`variable`, `value`, `type`, `category`, `description`)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    `value` = VALUES(`value`),
                    `type` = VALUES(`type`),
                    `category` = VALUES(`category`),
                    `description` = VALUES(`description`)
                """, variable, value == null ? "" : value, type, category, description);
    }

    private String runtimeProperties(Matrix26ProvisioningJob job, Set<String> selectedModules) {
        boolean restaurant = Boolean.TRUE.equals(SystemModuleVisibilityMapper.systemModuleFlags(
                selectedModules,
                defaultValue(job.getBusinessType(), "").toLowerCase(Locale.ROOT).contains("restaurant")
                        || defaultValue(job.getBusinessType(), "").toLowerCase(Locale.ROOT).contains("restaurante")
        ).get("restaurant"));
        String displayName = brandingValue(job, "displayName", job.getBusinessName());
        String shortName = brandingValue(job, "shortName", displayName);
        String tagline = brandingValue(job, "tagline", "Sistema empresarial administrado por Matrix26");
        String compactLogo = job.isBrandingDemoAssetsEnabled()
                ? "/runtime-assets/logo-compact-v1.png"
                : "/img/logo-eco.png";
        String primaryLogo = job.isBrandingDemoAssetsEnabled()
                ? "/runtime-assets/logo-primary-v1.png"
                : "/img/logo3-transparente.png";
        return "# Runtime generated by Matrix26 Control Center\n"
                + "server.port=" + job.getRuntimePort() + "\n"
                + "spring.datasource.url=" + targetJdbcUrl(job.getDatabaseName()) + "\n"
                + "spring.datasource.username=${ECOAGUA_DB_USERNAME:root}\n"
                + "spring.datasource.password=${ECOAGUA_DB_PASSWORD:root}\n"
                + "spring.datasource.driver-class-name=" + databaseDriver + "\n\n"
                + "spring.jpa.hibernate.ddl-auto=none\n"
                + "spring.jpa.show-sql=false\n"
                + "spring.jpa.open-in-view=false\n"
                + "spring.thymeleaf.cache=false\n"
                + "server.error.include-message=always\n\n"
                + "matrix26.control-center.enabled=false\n"
                + "matrix26.appearance-data-directory=runtime-data\n"
                + "ecoagua.modules.installation-allowed=false\n"
                + "ecoagua.platform.client-code=" + escapeProperty(job.getInstanceCode()) + "\n"
                + "ecoagua.platform.runtime-profile=" + escapeProperty(job.getRuntimeProfile()) + "\n"
                + "ecoagua.platform.public-url=" + escapeProperty(job.getPublicUrl()) + "\n\n"
                + "ecoagua.business.profile-code=" + escapeProperty(job.getRuntimeProfile()) + "\n"
                + "ecoagua.business.name=" + escapeProperty(displayName) + "\n"
                + "ecoagua.business.short-name=" + escapeProperty(shortName) + "\n"
                + "ecoagua.business.tagline=" + escapeProperty(tagline) + "\n"
                + "ecoagua.business.type=" + escapeProperty(defaultValue(job.getBusinessType(), restaurant ? "restaurant" : "business")) + "\n"
                + "ecoagua.business.admin-title=" + escapeProperty(tagline) + "\n"
                + "ecoagua.business.logo=" + escapeProperty(primaryLogo) + "\n"
                + "ecoagua.business.admin-logo=" + escapeProperty(compactLogo) + "\n"
                + "ecoagua.business.location=" + escapeProperty(brandingValue(job, "location", defaultValue(job.getCity(), "Iquitos"))) + "\n"
                + "ecoagua.business.footer-right=Instancia administrada por Matrix26\n\n"
                + runtimeFeatureProperties(selectedModules, restaurant)
                + "\n"
                + "google.maps.api-key=\n";
    }


    private String runtimeFeatureProperties(Set<String> selectedModules, boolean restaurantProfile) {
        StringBuilder out = new StringBuilder();
        SystemModuleVisibilityMapper.featureProperties(selectedModules, restaurantProfile)
                .forEach((property, enabled) -> out.append(property).append("=").append(enabled).append("\n"));
        return out.toString();
    }

    private String runtimeScript(Matrix26ProvisioningJob job) {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n\n"
                + "ROOT_DIR=\"$(cd \"$(dirname \"${BASH_SOURCE[0]}\")/../..\" && pwd)\"\n"
                + "CONFIG_FILE=\"$ROOT_DIR/runtime-clients/" + job.getRuntimeProfile()
                + "/application.properties\"\n\n"
                + "if [[ ! -f \"$CONFIG_FILE\" ]]; then\n"
                + "  echo \"Runtime configuration was not found: $CONFIG_FILE\" >&2\n"
                + "  exit 1\n"
                + "fi\n\n"
                + "if command -v cygpath >/dev/null 2>&1; then\n"
                + "  CONFIG_PATH=\"$(cygpath -m \"$CONFIG_FILE\")\"\n"
                + "else\n"
                + "  CONFIG_PATH=\"$CONFIG_FILE\"\n"
                + "fi\n\n"
                + "cd \"$ROOT_DIR\"\n\n"
                + "JAR=\"$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*sources*' | head -n 1)\"\n"
                + "if [[ -z \"$JAR\" ]]; then\n"
                + "  echo \"No application JAR found. Run: mvn clean -DskipTests package\" >&2\n"
                + "  exit 1\n"
                + "fi\n\n"
                + "exec java -jar \"$JAR\" --spring.config.additional-location=\"file:${CONFIG_PATH}\"\n";
    }

    private String runtimeReadme(Matrix26ProvisioningJob job) {
        return "Runtime generated by Matrix26 Control Center\n\n"
                + "Business: " + job.getBusinessName() + "\n"
                + "Database: " + job.getDatabaseName() + "\n"
                + "Port: " + job.getRuntimePort() + "\n"
                + "URL: " + job.getPublicUrl() + "\n\n"
                + "Build once from the repository root:\n"
                + "  mvn clean -DskipTests package\n\n"
                + "Run:\n"
                + "  bash runtime-clients/" + job.getRuntimeProfile() + "/run.sh\n";
    }

    private String targetJdbcUrl(String databaseName) {
        Matcher matcher = DATABASE_URL_PATTERN.matcher(controlJdbcUrl.trim());
        if (!matcher.matches()) {
            throw new IllegalStateException("Matrix26 datasource URL is not a supported MySQL JDBC URL.");
        }
        String query = matcher.group(3) == null ? "" : matcher.group(3);
        return matcher.group(1) + safeIdentifier(databaseName) + query;
    }

    private String safeIdentifier(String value) {
        String clean = value == null ? "" : value.trim();
        if (!SAFE_IDENTIFIER.matcher(clean).matches()) {
            throw new IllegalArgumentException("Identificador de base de datos no válido: " + clean);
        }
        return clean;
    }

    private String quoteIdentifier(String value) {
        return value.replace("`", "``");
    }

    private String brandingValue(Matrix26ProvisioningJob job, String key, String fallback) {
        String value = Matrix26JsonCodec.readFlatObject(job.getBrandingJson()).get(key);
        return defaultValue(value, fallback);
    }

    private String defaultValue(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        return clean.isBlank() ? fallback : clean;
    }

    private String escapeProperty(String value) {
        return defaultValue(value, "").replace("\\", "\\\\").replace("\n", " ").replace("\r", " ");
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
            // Windows filesystems do not expose POSIX permissions.
        }
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 450 ? message.substring(0, 450) : message;
    }
    private record TableCopyDefinition(
            String createTableSql,
            List<ForeignKeyDefinition> foreignKeys
    ) {
    }

    private record ForeignKeyDefinition(
            String tableName,
            String constraintName,
            String alterTableSql
    ) {
    }


}
