package com.ecoamazonas.eco_agua.platform.control.restores;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLog;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLogRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceManagementService;
import com.ecoamazonas.eco_agua.platform.control.Matrix26TargetDatabaseService;
import com.ecoamazonas.eco_agua.platform.control.appearance.Matrix26JsonCodec;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupExtraction;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupSecurityService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26RestoreVerificationService {

    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern MODULE_ENTRY = Pattern.compile(
            "\\{[^{}]*\"module_key\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"enabled\"\\s*:\\s*(true|1)[^{}]*\\}",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_$]+$");

    private final Matrix26RestoreRepository restoreRepository;
    private final Matrix26BackupSecurityService backupSecurityService;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26InstanceManagementService instanceManagementService;
    private final Matrix26TargetDatabaseService targetDatabaseService;
    private final Matrix26RestoreProperties properties;
    private final Matrix26InstanceAuditLogRepository auditRepository;

    public Matrix26RestoreVerificationService(
            Matrix26RestoreRepository restoreRepository,
            Matrix26BackupSecurityService backupSecurityService,
            PlatformBusinessClientRepository clientRepository,
            Matrix26InstanceManagementService instanceManagementService,
            Matrix26TargetDatabaseService targetDatabaseService,
            Matrix26RestoreProperties properties,
            Matrix26InstanceAuditLogRepository auditRepository
    ) {
        this.restoreRepository = restoreRepository;
        this.backupSecurityService = backupSecurityService;
        this.clientRepository = clientRepository;
        this.instanceManagementService = instanceManagementService;
        this.targetDatabaseService = targetDatabaseService;
        this.properties = properties;
        this.auditRepository = auditRepository;
    }

    public Matrix26RestoreValidationRun verify(long restoreJobId, String confirmation, String actor) {
        if (!properties.isVerificationEnabled()) {
            throw new Matrix26RestoreException("Automated restore verification is disabled.");
        }
        Matrix26RestoreJob job = restoreRepository.findById(restoreJobId)
                .orElseThrow(() -> new Matrix26RestoreException("Restore job not found."));
        if (!job.completed()) {
            throw new Matrix26RestoreException("Only completed restore jobs can be verified.");
        }
        String expected = "VERIFY " + job.publicId();
        if (!expected.equals(confirmation == null ? "" : confirmation.trim())) {
            throw new Matrix26RestoreException("Type exactly: " + expected);
        }
        if (restoreRepository.hasActiveValidation(restoreJobId)) {
            throw new Matrix26RestoreException("A verification run is already active for this restore.");
        }

        String publicId = "VRF-" + LocalDateTime.now().format(ID_TIME) + "-"
                + Long.toHexString(System.nanoTime()).toUpperCase(Locale.ROOT);
        long runId = restoreRepository.insertValidationRun(restoreJobId, publicId, safeActor(actor));
        Path work = Path.of(System.getProperty("java.io.tmpdir"), "matrix26-restore-verification", publicId)
                .toAbsolutePath().normalize();
        List<Matrix26RestoreCheckStatus> statuses = new ArrayList<>();

        try {
            Files.createDirectories(work);
            Matrix26BackupExtraction extraction = backupSecurityService.extractVerifiedBackup(
                    job.backupJobId(), work.resolve("backup")
            );
            add(runId, statuses, "BACKUP_PACKAGE", "Backup", "Encrypted recovery package",
                    Matrix26RestoreCheckStatus.MATCH, job.backupPublicId(), extraction.verificationMessage(),
                    "AES-256-GCM authentication and all internal SHA-256 checks passed.");

            PlatformBusinessClient source = clientRepository.findById(job.sourceInstanceId())
                    .orElseThrow(() -> new Matrix26RestoreException("The source instance registration no longer exists."));
            PlatformBusinessClient target = targetClient(job);

            verifySourceRegistration(runId, statuses, job, source);
            verifyDatabase(runId, statuses, job, extraction.extractedDirectory().resolve("database.sql.gz"));
            verifyModules(runId, statuses, target, extraction.extractedDirectory().resolve("modules.json"));
            verifyAppearance(runId, statuses, job, extraction.extractedDirectory().resolve("appearance.json"));
            verifyResources(runId, statuses, job, extraction.extractedDirectory().resolve("instance-files.zip"));
            verifyRuntimeIsolation(runId, statuses, job, source, target);
            verifyHttp(runId, statuses, job);

            Matrix26RestoreValidationStatus overall = overallStatus(statuses);
            String summary = summary(statuses, overall);
            restoreRepository.completeValidationRun(runId, overall, summary);
            writeAudit(target, actor, "RESTORE_CLONE_VERIFIED",
                    "Validation " + publicId + " completed with status " + overall.name() + ".");
            return restoreRepository.findValidationRun(runId).orElseThrow();
        } catch (Exception ex) {
            String detail = safeMessage(ex);
            if (statuses.stream().noneMatch(status -> status == Matrix26RestoreCheckStatus.FAILED)) {
                try {
                    add(runId, statuses, "VERIFICATION_EXECUTION", "Verification", "Verification execution",
                            Matrix26RestoreCheckStatus.FAILED, "Expected complete verification", "Execution stopped", detail);
                } catch (RuntimeException ignored) {
                    // Preserve the original verification exception.
                }
            }
            restoreRepository.completeValidationRun(runId, Matrix26RestoreValidationStatus.FAILED,
                    "Verification failed: " + detail);
            throw ex instanceof Matrix26RestoreException restoreException
                    ? restoreException
                    : new Matrix26RestoreException("Restore verification failed: " + detail, ex);
        } finally {
            deleteTreeQuietly(work);
        }
    }

    public Matrix26RestoreValidationRun latest(long restoreJobId) {
        return restoreRepository.findLatestValidationRun(restoreJobId).orElse(null);
    }

    public List<Matrix26RestoreValidationItem> items(long runId) {
        return restoreRepository.findValidationItems(runId);
    }

    public Matrix26RestoreValidationRun run(long runId) {
        return restoreRepository.findValidationRun(runId)
                .orElseThrow(() -> new Matrix26RestoreException("Verification run not found."));
    }

    public String report(long runId) {
        Matrix26RestoreValidationRun run = restoreRepository.findValidationRun(runId)
                .orElseThrow(() -> new Matrix26RestoreException("Verification run not found."));
        Matrix26RestoreJob job = restoreRepository.findById(run.restoreJobId())
                .orElseThrow(() -> new Matrix26RestoreException("Restore job not found."));
        StringBuilder output = new StringBuilder();
        output.append("Matrix26 Restore Verification Report\n")
                .append("====================================\n\n")
                .append("Verification: ").append(run.publicId()).append('\n')
                .append("Restore job: ").append(job.publicId()).append('\n')
                .append("Backup: ").append(job.backupPublicId()).append('\n')
                .append("Source: ").append(job.sourceInstanceCode()).append(" / ").append(job.sourceDatabaseName()).append('\n')
                .append("Target: ").append(job.targetInstanceCode()).append(" / ").append(job.targetDatabaseName()).append('\n')
                .append("Status: ").append(run.status().name()).append('\n')
                .append("Requested by: ").append(run.requestedBy()).append('\n')
                .append("Requested at: ").append(run.requestedAt()).append('\n')
                .append("Completed at: ").append(run.completedAt()).append('\n')
                .append("Summary: ").append(nullSafe(run.summary())).append("\n\nChecks\n------\n");
        for (Matrix26RestoreValidationItem item : restoreRepository.findValidationItems(runId)) {
            output.append('[').append(item.status().name()).append("] ")
                    .append(item.category()).append(" / ").append(item.label()).append('\n')
                    .append("Code: ").append(item.checkCode()).append('\n')
                    .append("Expected: ").append(nullSafe(item.sourceValue())).append('\n')
                    .append("Actual: ").append(nullSafe(item.targetValue())).append('\n')
                    .append("Detail: ").append(nullSafe(item.detail())).append("\n\n");
        }
        output.append("Safety note: this report contains no database password or backup master key.\n");
        return output.toString();
    }

    private void verifySourceRegistration(
            long runId,
            List<Matrix26RestoreCheckStatus> statuses,
            Matrix26RestoreJob job,
            PlatformBusinessClient source
    ) {
        boolean match = Objects.equals(source.getCode(), job.sourceInstanceCode())
                && Objects.equals(source.getDatabaseName(), job.sourceDatabaseName())
                && !Objects.equals(source.getDatabaseName(), job.targetDatabaseName())
                && !Objects.equals(source.getRuntimeProfile(), job.targetRuntimeProfile())
                && !Objects.equals(source.getRuntimePort(), job.targetRuntimePort());
        add(runId, statuses, "SOURCE_UNCHANGED", "Isolation", "Original instance registration",
                match ? Matrix26RestoreCheckStatus.MATCH : Matrix26RestoreCheckStatus.MISMATCH,
                job.sourceInstanceCode() + " / " + job.sourceDatabaseName(),
                source.getCode() + " / " + source.getDatabaseName(),
                match ? "The original instance remains registered under its own database, runtime, and port."
                        : "The source registration no longer matches the backup restore record.");
    }

    private void verifyDatabase(
            long runId,
            List<Matrix26RestoreCheckStatus> statuses,
            Matrix26RestoreJob job,
            Path gzipDump
    ) throws IOException {
        if (!targetDatabaseService.databaseExists(job.targetDatabaseName())) {
            add(runId, statuses, "DATABASE_PRESENT", "Database", "Restored database",
                    Matrix26RestoreCheckStatus.MISMATCH, job.targetDatabaseName(), "Missing",
                    "The isolated target database does not exist.");
            return;
        }

        Matrix26RestoreDumpSnapshot.Snapshot expected = Matrix26RestoreDumpSnapshot.read(gzipDump);
        JdbcTemplate target = targetDatabaseService.targetJdbcTemplate(job.targetDatabaseName());
        List<String> actualTables = target.queryForList("""
                SELECT TABLE_NAME FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY TABLE_NAME
                """, String.class);
        Set<String> expectedTables = new TreeSet<>(expected.createStatements().keySet());
        Set<String> actualTableSet = new TreeSet<>(actualTables);
        Set<String> missing = difference(expectedTables, actualTableSet);
        Set<String> unexpected = difference(actualTableSet, expectedTables);
        Matrix26RestoreCheckStatus tableStatus = missing.isEmpty() && unexpected.isEmpty()
                ? Matrix26RestoreCheckStatus.MATCH : Matrix26RestoreCheckStatus.MISMATCH;
        add(runId, statuses, "DATABASE_TABLE_SET", "Database", "Database table set", tableStatus,
                expectedTables.size() + " tables in encrypted dump", actualTableSet.size() + " restored tables",
                differences("Missing", missing, "Unexpected", unexpected));

        List<String> schemaMismatches = new ArrayList<>();
        for (String table : expectedTables) {
            if (!actualTableSet.contains(table) || !SAFE_IDENTIFIER.matcher(table).matches()) {
                continue;
            }
            String actualCreate = target.queryForObject("SHOW CREATE TABLE `" + table.replace("`", "``") + "`",
                    (rs, rowNum) -> rs.getString(2));
            String expectedCreate = expected.createStatements().get(table);
            if (!schemaEquivalent(expectedCreate, actualCreate)) {
                schemaMismatches.add(table);
            }
        }
        add(runId, statuses, "DATABASE_SCHEMA", "Database", "Schema signatures",
                schemaMismatches.isEmpty() && missing.isEmpty() && unexpected.isEmpty()
                        ? Matrix26RestoreCheckStatus.MATCH : Matrix26RestoreCheckStatus.MISMATCH,
                expectedTables.size() + " CREATE TABLE definitions",
                (expectedTables.size() - schemaMismatches.size() - missing.size()) + " matching definitions",
                schemaMismatches.isEmpty() ? "All restored table definitions match the encrypted SQL dump."
                        : "Different table definitions: " + limitedJoin(schemaMismatches, 30));

        Map<String, Long> actualCounts = new TreeMap<>();
        List<String> countMismatches = new ArrayList<>();
        for (String table : expectedTables) {
            if (!actualTableSet.contains(table) || !SAFE_IDENTIFIER.matcher(table).matches()) {
                continue;
            }
            Long count = target.queryForObject("SELECT COUNT(*) FROM `" + table.replace("`", "``") + "`", Long.class);
            long actual = count == null ? 0L : count;
            actualCounts.put(table, actual);
            long expectedCount = expected.rowCounts().getOrDefault(table, 0L);
            if (actual != expectedCount) {
                countMismatches.add(table + " (backup=" + expectedCount + ", clone=" + actual + ")");
            }
        }
        add(runId, statuses, "DATABASE_ROW_COUNTS", "Database", "Table row counts",
                countMismatches.isEmpty() && missing.isEmpty()
                        ? Matrix26RestoreCheckStatus.MATCH : Matrix26RestoreCheckStatus.MISMATCH,
                expected.rowCounts().values().stream().mapToLong(Long::longValue).sum() + " rows in encrypted dump",
                actualCounts.values().stream().mapToLong(Long::longValue).sum() + " rows in clone",
                countMismatches.isEmpty() ? "Every restored table has the same row count as the encrypted SQL dump."
                        : "Different row counts: " + limitedJoin(countMismatches, 40));
    }

    private void verifyModules(
            long runId,
            List<Matrix26RestoreCheckStatus> statuses,
            PlatformBusinessClient target,
            Path modulesJson
    ) throws IOException {
        String json = Files.readString(modulesJson, StandardCharsets.UTF_8);
        Set<String> expected = new TreeSet<>();
        Matcher matcher = MODULE_ENTRY.matcher(json);
        while (matcher.find()) {
            expected.add(matcher.group(1));
        }
        Set<String> actual = new TreeSet<>(instanceManagementService.assignedModuleKeys(target.getId()));
        add(runId, statuses, "MODULE_ASSIGNMENTS", "Modules", "Assigned modules",
                expected.equals(actual) ? Matrix26RestoreCheckStatus.MATCH : Matrix26RestoreCheckStatus.MISMATCH,
                String.join(", ", expected), String.join(", ", actual),
                expected.equals(actual) ? "The clone has the module set captured in the backup."
                        : differences("Missing", difference(expected, actual), "Unexpected", difference(actual, expected)));
    }

    private void verifyAppearance(
            long runId,
            List<Matrix26RestoreCheckStatus> statuses,
            Matrix26RestoreJob job,
            Path appearanceJson
    ) throws IOException {
        Map<String, Object> root = Matrix26JsonCodec.readObject(Files.readString(appearanceJson, StandardCharsets.UTF_8));
        Object appearanceValue = root.get("appearance");
        if (!(appearanceValue instanceof Map<?, ?> expectedRaw)) {
            add(runId, statuses, "APPEARANCE_CONFIGURATION", "Appearance", "Published appearance",
                    Matrix26RestoreCheckStatus.NOT_APPLICABLE, "No appearance snapshot", "Not evaluated",
                    "The backup did not contain a central Appearance Studio publication record.");
            return;
        }
        JdbcTemplate target = targetDatabaseService.targetJdbcTemplate(job.targetDatabaseName());
        Integer tableCount = target.queryForObject("""
                SELECT COUNT(*) FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'matrix26_instance_appearance_config'
                """, Integer.class);
        if (tableCount == null || tableCount == 0) {
            add(runId, statuses, "APPEARANCE_CONFIGURATION", "Appearance", "Published appearance",
                    Matrix26RestoreCheckStatus.MISMATCH, "Appearance configuration from backup", "Table missing",
                    "The restored database does not contain matrix26_instance_appearance_config.");
            return;
        }
        Map<String, Object> actual = target.query("""
                SELECT instance_code, public_theme_code, public_layout_code, admin_theme_code,
                       admin_layout_code, login_layout_code, overrides_json, published_version
                FROM matrix26_instance_appearance_config WHERE id = 1
                """, rs -> {
            if (!rs.next()) return Map.of();
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("instance_code", rs.getString("instance_code"));
            values.put("public_theme_code", rs.getString("public_theme_code"));
            values.put("public_layout_code", rs.getString("public_layout_code"));
            values.put("admin_theme_code", rs.getString("admin_theme_code"));
            values.put("admin_layout_code", rs.getString("admin_layout_code"));
            values.put("login_layout_code", rs.getString("login_layout_code"));
            values.put("overrides_json", normalizeJsonText(rs.getString("overrides_json"), job.sourceInstanceCode(), job.targetInstanceCode()));
            values.put("published_version", rs.getObject("published_version"));
            return values;
        });

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("instance_code", job.targetInstanceCode());
        for (String key : List.of("public_theme_code", "public_layout_code", "admin_theme_code",
                "admin_layout_code", "login_layout_code", "published_version")) {
            expected.put(key, expectedRaw.get(key));
        }
        expected.put("overrides_json", normalizeJsonText(value(expectedRaw.get("overrides_json")),
                job.sourceInstanceCode(), job.targetInstanceCode()));
        boolean match = comparableMap(expected).equals(comparableMap(actual));
        add(runId, statuses, "APPEARANCE_CONFIGURATION", "Appearance", "Published appearance",
                match ? Matrix26RestoreCheckStatus.MATCH : Matrix26RestoreCheckStatus.MISMATCH,
                compactMap(expected), compactMap(actual),
                match ? "Theme, layouts, published version, and overrides match the backup snapshot."
                        : "The restored appearance differs from the backup snapshot.");
    }

    private void verifyResources(
            long runId,
            List<Matrix26RestoreCheckStatus> statuses,
            Matrix26RestoreJob job,
            Path archive
    ) throws IOException {
        Path dataRoot = projectRoot().resolve(properties.getRuntimeDataDirectory()).resolve(job.targetInstanceCode()).normalize();
        Map<String, String> expectedHashes = new TreeMap<>();
        Map<String, Long> expectedSizes = new TreeMap<>();
        String prefix = "runtime-data/" + job.sourceInstanceCode() + "/";
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !name.startsWith(prefix)) continue;
                String relative = name.substring(prefix.length());
                try (InputStream input = zip.getInputStream(entry)) {
                    expectedHashes.put(relative, sha256(input));
                    expectedSizes.put(relative, entry.getSize());
                }
            }
        }
        List<String> missing = new ArrayList<>();
        List<String> different = new ArrayList<>();
        for (Map.Entry<String, String> entry : expectedHashes.entrySet()) {
            Path target = dataRoot.resolve(entry.getKey()).normalize();
            if (!target.startsWith(dataRoot)) {
                different.add(entry.getKey() + " (unsafe path)");
            } else if (!Files.isRegularFile(target)) {
                missing.add(entry.getKey());
            } else if (!entry.getValue().equalsIgnoreCase(sha256(Files.newInputStream(target)))) {
                different.add(entry.getKey());
            }
        }
        Matrix26RestoreCheckStatus status = missing.isEmpty() && different.isEmpty()
                ? Matrix26RestoreCheckStatus.MATCH : Matrix26RestoreCheckStatus.MISMATCH;
        add(runId, statuses, "RESOURCE_HASHES", "Resources", "Restored resource hashes", status,
                expectedHashes.size() + " resource files in backup",
                (expectedHashes.size() - missing.size() - different.size()) + " matching clone files",
                status == Matrix26RestoreCheckStatus.MATCH
                        ? "Every instance-owned resource has the same SHA-256 as the encrypted backup."
                        : differences("Missing", new LinkedHashSet<>(missing), "Different", new LinkedHashSet<>(different)));
    }

    private void verifyRuntimeIsolation(
            long runId,
            List<Matrix26RestoreCheckStatus> statuses,
            Matrix26RestoreJob job,
            PlatformBusinessClient source,
            PlatformBusinessClient target
    ) throws IOException {
        boolean registrationMatch = Objects.equals(target.getCode(), job.targetInstanceCode())
                && Objects.equals(target.getDatabaseName(), job.targetDatabaseName())
                && Objects.equals(target.getRuntimeProfile(), job.targetRuntimeProfile())
                && Objects.equals(target.getRuntimePort(), job.targetRuntimePort())
                && !Objects.equals(source.getDatabaseName(), target.getDatabaseName())
                && !Objects.equals(source.getRuntimeProfile(), target.getRuntimeProfile())
                && !Objects.equals(source.getRuntimePort(), target.getRuntimePort());
        add(runId, statuses, "RUNTIME_REGISTRATION", "Runtime", "Clone registration isolation",
                registrationMatch ? Matrix26RestoreCheckStatus.MATCH : Matrix26RestoreCheckStatus.MISMATCH,
                job.targetInstanceCode() + " / " + job.targetDatabaseName() + " / " + job.targetRuntimePort(),
                target.getCode() + " / " + target.getDatabaseName() + " / " + target.getRuntimePort(),
                registrationMatch ? "The clone uses an independent instance code, database, runtime profile, and port."
                        : "The clone registration does not match the isolated recovery target.");

        Path runtime = projectRoot().resolve(properties.getRuntimeDirectory()).resolve(job.targetRuntimeProfile()).normalize();
        Path config = runtime.resolve("application.properties");
        Path marker = runtime.resolve(".matrix26-restore-reference");
        boolean filesMatch = Files.isRegularFile(config) && Files.isRegularFile(marker)
                && Files.readString(marker, StandardCharsets.UTF_8).trim().equals(job.publicId());
        String configText = Files.isRegularFile(config) ? Files.readString(config, StandardCharsets.UTF_8) : "";
        boolean configMatch = filesMatch
                && configText.contains("server.port=" + job.targetRuntimePort())
                && configText.contains("/" + job.targetDatabaseName())
                && configText.contains("ecoagua.platform.client-code=" + job.targetInstanceCode())
                && !configText.contains("/" + job.sourceDatabaseName());
        add(runId, statuses, "RUNTIME_CONFIGURATION", "Runtime", "Runtime configuration",
                configMatch ? Matrix26RestoreCheckStatus.MATCH : Matrix26RestoreCheckStatus.MISMATCH,
                "Clone marker, database, port, and client code", configMatch ? "Isolated configuration present" : "Configuration missing or inconsistent",
                configMatch ? "Runtime configuration points only to the restored clone resources."
                        : "The runtime directory or non-sensitive isolation properties are inconsistent.");
    }

    private void verifyHttp(
            long runId,
            List<Matrix26RestoreCheckStatus> statuses,
            Matrix26RestoreJob job
    ) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(2, properties.getVerificationHttpTimeoutSeconds())))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        List<String> failures = new ArrayList<>();
        List<String> results = new ArrayList<>();
        for (String configuredPath : properties.getVerificationHttpPaths()) {
            String path = configuredPath == null || configuredPath.isBlank() ? "/" : configuredPath.trim();
            if (!path.startsWith("/")) path = "/" + path;
            try {
                URI uri = URI.create(job.targetPublicUrl() + path);
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(Math.max(2, properties.getVerificationHttpTimeoutSeconds())))
                        .GET().build();
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                int status = response.statusCode();
                results.add(path + "=" + status);
                boolean protectedRoute = path.startsWith("/admin/");
                boolean healthy = (status >= 200 && status < 400)
                        || (protectedRoute && (status == 401 || status == 403));
                if (!healthy) failures.add(path + " returned " + status);
            } catch (Exception ex) {
                failures.add(path + " failed: " + safeMessage(ex));
            }
        }
        add(runId, statuses, "HTTP_ROUTES", "HTTP", "Clone portal routes",
                failures.isEmpty() ? Matrix26RestoreCheckStatus.MATCH : Matrix26RestoreCheckStatus.MISMATCH,
                properties.getVerificationHttpPaths().size() + " reachable routes",
                String.join(", ", results),
                failures.isEmpty() ? "Public, login, restaurant, and protected administrative routes responded with HTTP 2xx/3xx."
                        : limitedJoin(failures, 20));
    }

    private PlatformBusinessClient targetClient(Matrix26RestoreJob job) {
        if (job.targetInstanceId() != null) {
            return clientRepository.findById(job.targetInstanceId())
                    .orElseThrow(() -> new Matrix26RestoreException("The restored clone registration no longer exists."));
        }
        return clientRepository.findByCodeIgnoreCase(job.targetInstanceCode())
                .orElseThrow(() -> new Matrix26RestoreException("The restored clone registration does not exist."));
    }

    private void add(
            long runId,
            List<Matrix26RestoreCheckStatus> statuses,
            String code,
            String category,
            String label,
            Matrix26RestoreCheckStatus status,
            String source,
            String target,
            String detail
    ) {
        restoreRepository.insertValidationItem(runId, code, category, label, status, source, target, detail);
        statuses.add(status);
    }

    private Matrix26RestoreValidationStatus overallStatus(List<Matrix26RestoreCheckStatus> statuses) {
        if (statuses.stream().anyMatch(status -> status == Matrix26RestoreCheckStatus.FAILED)) {
            return Matrix26RestoreValidationStatus.FAILED;
        }
        if (statuses.stream().anyMatch(status -> status == Matrix26RestoreCheckStatus.MISMATCH)) {
            return Matrix26RestoreValidationStatus.MISMATCH;
        }
        if (statuses.stream().anyMatch(status -> status == Matrix26RestoreCheckStatus.WARNING)) {
            return Matrix26RestoreValidationStatus.VERIFIED_WITH_WARNINGS;
        }
        return Matrix26RestoreValidationStatus.VERIFIED;
    }

    private String summary(List<Matrix26RestoreCheckStatus> statuses, Matrix26RestoreValidationStatus overall) {
        long matches = statuses.stream().filter(status -> status == Matrix26RestoreCheckStatus.MATCH).count();
        long warnings = statuses.stream().filter(status -> status == Matrix26RestoreCheckStatus.WARNING).count();
        long mismatches = statuses.stream().filter(status -> status == Matrix26RestoreCheckStatus.MISMATCH).count();
        long failures = statuses.stream().filter(status -> status == Matrix26RestoreCheckStatus.FAILED).count();
        return overall.getLabel() + ": " + matches + " matches, " + warnings + " warnings, "
                + mismatches + " mismatches, " + failures + " failures.";
    }

    private boolean schemaEquivalent(String expected, String actual) {
        String normalizedExpected = Matrix26RestoreDumpSnapshot.normalizeCreateStatement(expected)
                .replaceAll("(?i)\\s+COLLATE=\\w+", "")
                .replaceAll("(?i)\\s+DEFAULT CHARSET=\\w+", "")
                .replaceAll("\\s+", " ").trim();
        String normalizedActual = Matrix26RestoreDumpSnapshot.normalizeCreateStatement(actual)
                .replaceAll("(?i)\\s+COLLATE=\\w+", "")
                .replaceAll("(?i)\\s+DEFAULT CHARSET=\\w+", "")
                .replaceAll("\\s+", " ").trim();
        return normalizedExpected.equals(normalizedActual);
    }

    private Map<String, String> comparableMap(Map<String, Object> values) {
        Map<String, String> result = new TreeMap<>();
        values.forEach((key, value) -> result.put(key, value(value)));
        return result;
    }

    private String compactMap(Map<String, Object> values) {
        return Matrix26JsonCodec.write(new TreeMap<>(values));
    }

    private String normalizeJsonText(String value, String sourceCode, String targetCode) {
        if (value == null || value.isBlank()) return "";
        String replaced = value.replace(sourceCode, targetCode);
        Map<String, Object> parsed = Matrix26JsonCodec.readObject(replaced);
        return parsed.isEmpty() ? replaced.replaceAll("\\s+", "") : Matrix26JsonCodec.write(new TreeMap<>(parsed));
    }

    private Set<String> difference(Set<String> first, Set<String> second) {
        Set<String> result = new TreeSet<>(first);
        result.removeAll(second);
        return result;
    }

    private String differences(String firstLabel, Set<String> first, String secondLabel, Set<String> second) {
        if (first.isEmpty() && second.isEmpty()) return "No differences.";
        return firstLabel + ": " + (first.isEmpty() ? "none" : limitedJoin(new ArrayList<>(first), 30))
                + "; " + secondLabel + ": " + (second.isEmpty() ? "none" : limitedJoin(new ArrayList<>(second), 30));
    }

    private String limitedJoin(List<String> values, int maximum) {
        if (values == null || values.isEmpty()) return "none";
        List<String> copy = values.stream().limit(maximum).toList();
        return String.join(", ", copy) + (values.size() > maximum ? " … and " + (values.size() - maximum) + " more" : "");
    }

    private String sha256(InputStream input) throws IOException {
        try (InputStream source = input) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = source.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Path projectRoot() { return Path.of("").toAbsolutePath().normalize(); }

    private void writeAudit(PlatformBusinessClient instance, String actor, String action, String summary) {
        Matrix26InstanceAuditLog log = new Matrix26InstanceAuditLog();
        log.setInstance(instance);
        log.setActorUsername(safeActor(actor));
        log.setAction(action);
        log.setSummary(limit(summary, 500));
        log.setAfterSnapshot("{\"instanceCode\":\"" + escapeJson(instance.getCode()) + "\"}");
        auditRepository.save(log);
    }

    private void deleteTreeQuietly(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private String safeActor(String actor) { return actor == null || actor.isBlank() ? "matrix26-admin" : limit(actor.trim(), 120); }
    private String safeMessage(Throwable ex) { return limit(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 4000); }
    private String limit(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }
    private String nullSafe(String value) { return value == null ? "" : value; }
    private String value(Object value) { return value == null ? "" : String.valueOf(value); }
    private String escapeJson(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
