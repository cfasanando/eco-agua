package com.ecoamazonas.eco_agua.platform.control.backups;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLog;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BackupService {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_]{1,120}");
    private static final Pattern JDBC_MYSQL = Pattern.compile(
            "(?i)^jdbc:mysql://([^/:?]+)(?::(\\d+))?/([^?;]+).*$"
    );
    private static final DateTimeFormatter PUBLIC_ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM");
    private static final Set<Matrix26BackupStatus> ACTIVE_STATUSES = Set.of(
            Matrix26BackupStatus.PENDING,
            Matrix26BackupStatus.VALIDATING,
            Matrix26BackupStatus.RUNNING,
            Matrix26BackupStatus.COMPRESSING,
            Matrix26BackupStatus.VERIFYING
    );

    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26BackupRepository backupRepository;
    private final Matrix26BackupProperties properties;
    private final Matrix26BackupToolLocator toolLocator;
    private final Matrix26InstanceAuditLogRepository auditLogRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Matrix26FullBackupAssembler fullBackupAssembler;
    private final Set<Long> localLocks = ConcurrentHashMap.newKeySet();

    public Matrix26BackupService(
            PlatformBusinessClientRepository clientRepository,
            Matrix26BackupRepository backupRepository,
            Matrix26BackupProperties properties,
            Matrix26BackupToolLocator toolLocator,
            Matrix26InstanceAuditLogRepository auditLogRepository,
            JdbcTemplate jdbcTemplate,
            Matrix26FullBackupAssembler fullBackupAssembler
    ) {
        this.clientRepository = clientRepository;
        this.backupRepository = backupRepository;
        this.properties = properties;
        this.toolLocator = toolLocator;
        this.auditLogRepository = auditLogRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.fullBackupAssembler = fullBackupAssembler;
    }

    public List<Matrix26BackupCandidate> candidates() {
        return clientRepository.findAllByOrderByBusinessNameAsc().stream()
                .map(this::candidate)
                .toList();
    }

    public Matrix26BackupToolStatus toolStatus() {
        return toolLocator.locate();
    }

    public Path backupRoot() {
        String configured = firstNonBlank(
                System.getenv("MATRIX26_BACKUP_ROOT"),
                properties.getRootDirectory()
        );
        Path root = configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "Matrix26", "backups")
                : Path.of(configured);
        return root.toAbsolutePath().normalize();
    }

    public List<Matrix26BackupJob> recentJobs() {
        return backupRepository.findRecent();
    }

    public List<Matrix26BackupJob> jobsForInstance(long instanceId) {
        return backupRepository.findByInstanceId(instanceId);
    }

    public Matrix26BackupSummary summary() {
        Matrix26BackupSummary summary = backupRepository.summary();
        return summary == null ? new Matrix26BackupSummary(0, 0, 0, 0, 0) : summary;
    }

    public Matrix26BackupDetailView detail(long jobId) {
        Matrix26BackupJob job = backupRepository.findById(jobId)
                .orElseThrow(() -> new Matrix26BackupException("The requested backup does not exist."));
        return new Matrix26BackupDetailView(
                job,
                backupRepository.findArtifacts(jobId),
                backupRepository.findVerifications(jobId),
                job.backupDirectory(),
                formatBytes(job.compressedSizeBytes()),
                formatBytes(job.databaseSizeBytes()),
                formatBytes(job.dumpSizeBytes())
        );
    }

    public synchronized Matrix26BackupJob createManualDatabaseBackup(
            long instanceId,
            String actor,
            boolean confirmation
    ) {
        return createManualBackup(instanceId, actor, confirmation, false);
    }

    public synchronized Matrix26BackupJob createManualFullBackup(
            long instanceId,
            String actor,
            boolean confirmation
    ) {
        return createManualBackup(instanceId, actor, confirmation, true);
    }

    private Matrix26BackupJob createManualBackup(
            long instanceId,
            String actor,
            boolean confirmation,
            boolean fullBackup
    ) {
        if (!properties.isEnabled()) {
            throw new Matrix26BackupException("Database backups are disabled in Matrix26 configuration.");
        }
        if (!confirmation) {
            throw new Matrix26BackupException("Confirm that this is a manual database backup before continuing.");
        }

        PlatformBusinessClient instance = clientRepository.findById(instanceId)
                .orElseThrow(() -> new Matrix26BackupException("The selected instance does not exist."));
        Matrix26BackupCandidate candidate = candidate(instance);
        if (!candidate.allowed()) {
            throw new Matrix26BackupException(candidate.restrictionReason());
        }
        if (!localLocks.add(instanceId) || backupRepository.hasActiveJob(instanceId)) {
            localLocks.remove(instanceId);
            throw new Matrix26BackupException("Another backup is already active for this instance.");
        }

        Matrix26BackupJob createdJob = null;
        Path backupDirectory = null;
        try {
            Matrix26BackupToolStatus tool = toolLocator.locate();
            if (!tool.available()) {
                throw new Matrix26BackupException(tool.message());
            }

            Matrix26DatabaseConnectionInfo connection = connectionInfo(instance);
            validateTarget(instance, connection);

            LocalDateTime now = LocalDateTime.now();
            String publicId = "BKP-" + PUBLIC_ID_FORMAT.format(now) + "-" + UUID.randomUUID()
                    .toString().substring(0, 6).toUpperCase(Locale.ROOT);
            Path root = prepareBackupRoot();
            backupDirectory = root
                    .resolve(instance.getCode())
                    .resolve(YEAR_FORMAT.format(now))
                    .resolve(MONTH_FORMAT.format(now))
                    .resolve("backup-" + PUBLIC_ID_FORMAT.format(now) + "-" + publicId.substring(publicId.length() - 6))
                    .normalize();
            ensureInside(root, backupDirectory);
            Files.createDirectories(backupDirectory);
            ensureFreeSpace(backupDirectory);

            Matrix26BackupJob newJob = new Matrix26BackupJob(
                    null,
                    publicId,
                    instance.getId(),
                    instance.getCode(),
                    instance.getBusinessName(),
                    instance.getDatabaseName(),
                    fullBackup ? "MANUAL_FULL" : "MANUAL_DATABASE",
                    Matrix26BackupStatus.PENDING,
                    safeActor(actor),
                    now,
                    null,
                    null,
                    root.toString(),
                    backupDirectory.toString(),
                    tool.executable(),
                    tool.version(),
                    connection.host(),
                    connection.port(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            long jobId = backupRepository.insertJob(newJob);
            createdJob = backupRepository.findById(jobId).orElseThrow();

            executeBackup(createdJob, instance, connection, tool, root, backupDirectory, fullBackup);
            if (fullBackup) {
                Matrix26BackupJob databaseJob = backupRepository.findById(jobId).orElseThrow();
                Matrix26FullBackupResult fullResult = fullBackupAssembler.assemble(
                        databaseJob, instance, root, backupDirectory
                );
                finalizeFullBackup(databaseJob, instance, connection, tool, root, backupDirectory, fullResult);
                writeAudit(instance, actor, "FULL_BACKUP_COMPLETED", "Manual full instance backup completed: " + publicId);
            } else {
                writeAudit(instance, actor, "DATABASE_BACKUP_COMPLETED", "Manual database backup completed: " + publicId);
            }
            return backupRepository.findById(jobId).orElseThrow();
        } catch (Matrix26BackupException ex) {
            if (createdJob != null) {
                backupRepository.fail(createdJob.id(), sanitize(ex.getMessage()));
                writeFailureReport(backupDirectory, createdJob, ex.getMessage());
                writeAudit(instance, actor, fullBackup ? "FULL_BACKUP_FAILED" : "DATABASE_BACKUP_FAILED", (fullBackup ? "Manual full instance backup failed: " : "Manual database backup failed: ") + createdJob.publicId());
            }
            throw ex;
        } catch (Exception ex) {
            String message = "The backup could not be completed: " + safeMessage(ex);
            if (createdJob != null) {
                backupRepository.fail(createdJob.id(), message);
                writeFailureReport(backupDirectory, createdJob, message);
                writeAudit(instance, actor, fullBackup ? "FULL_BACKUP_FAILED" : "DATABASE_BACKUP_FAILED", (fullBackup ? "Manual full instance backup failed: " : "Manual database backup failed: ") + createdJob.publicId());
            }
            throw new Matrix26BackupException(message, ex);
        } finally {
            localLocks.remove(instanceId);
        }
    }

    private void executeBackup(
            Matrix26BackupJob job,
            PlatformBusinessClient instance,
            Matrix26DatabaseConnectionInfo connection,
            Matrix26BackupToolStatus tool,
            Path root,
            Path directory,
            boolean fullBackup
    ) throws IOException, InterruptedException {
        backupRepository.markStarted(job.id(), Matrix26BackupStatus.VALIDATING, tool.executable(), tool.version());

        int tableCount = tableCount(connection.databaseName());
        long databaseSize = databaseSize(connection.databaseName());
        verify(job.id(), "TARGET_DATABASE", "Target database", tableCount > 0,
                tableCount > 0
                        ? connection.databaseName() + " contains " + tableCount + " tables."
                        : "The target database does not contain any base tables.");
        if (tableCount <= 0) {
            throw new Matrix26BackupException("The target database does not contain any tables.");
        }

        Path rawDump = directory.resolve("database.raw.sql");
        Path sqlDump = directory.resolve("database.sql");
        Path compressed = directory.resolve("database.sql.gz");
        Path stderr = directory.resolve("mysqldump.stderr.log");
        Path manifest = directory.resolve("manifest.json");
        Path checksums = directory.resolve("checksums.sha256");
        Path report = directory.resolve("backup-report.txt");

        backupRepository.updateStatus(job.id(), Matrix26BackupStatus.RUNNING);
        ProcessBuilder builder = new ProcessBuilder(dumpCommand(tool.executable(), connection, rawDump));
        builder.redirectError(stderr.toFile());
        if (connection.password() != null && !connection.password().isBlank()) {
            builder.environment().put("MYSQL_PWD", connection.password());
        } else {
            builder.environment().remove("MYSQL_PWD");
        }

        Process process = builder.start();
        boolean finished = process.waitFor(Math.max(30L, properties.getProcessTimeoutSeconds()), TimeUnit.SECONDS);
        if (!finished) {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
            throw new Matrix26BackupException("The database export exceeded the configured timeout.");
        }
        if (process.exitValue() != 0) {
            throw new Matrix26BackupException("mysqldump failed: " + sanitizedFile(stderr));
        }
        if (!Files.isRegularFile(rawDump) || Files.size(rawDump) == 0L) {
            throw new Matrix26BackupException("mysqldump finished without producing a database export.");
        }

        prependOwnershipHeader(rawDump, sqlDump, job, connection);
        Files.deleteIfExists(rawDump);
        long dumpSize = Files.size(sqlDump);
        verify(job.id(), "DUMP_NON_EMPTY", "Non-empty SQL export", dumpSize > 0,
                "SQL export size: " + formatBytes(dumpSize) + ".");

        backupRepository.updateStatus(job.id(), Matrix26BackupStatus.COMPRESSING);
        gzip(sqlDump, compressed);
        long compressedSize = Files.size(compressed);
        Files.deleteIfExists(sqlDump);

        backupRepository.updateStatus(job.id(), Matrix26BackupStatus.VERIFYING);
        GzipInspection inspection = inspectGzip(compressed, connection.databaseName());
        verify(job.id(), "GZIP_READABLE", "Readable GZIP archive", inspection.readable(), inspection.message());
        verify(job.id(), "DATABASE_OWNERSHIP", "Database ownership marker", inspection.databaseMarker(),
                inspection.databaseMarker()
                        ? "The export identifies database " + connection.databaseName() + "."
                        : "The database ownership marker was not found.");
        verify(job.id(), "CREATE_TABLE", "CREATE TABLE statements", inspection.createTableCount() > 0,
                inspection.createTableCount() + " CREATE TABLE statements detected.");
        if (!inspection.readable() || !inspection.databaseMarker() || inspection.createTableCount() <= 0) {
            throw new Matrix26BackupException("The compressed SQL export did not pass structural verification.");
        }

        String dumpSha = sha256(compressed);
        Files.writeString(
                checksums,
                dumpSha + "  database.sql.gz" + System.lineSeparator(),
                StandardCharsets.UTF_8
        );
        String recheckedSha = sha256(compressed);
        verify(job.id(), "SHA256", "SHA-256 checksum", dumpSha.equals(recheckedSha),
                "SHA-256: " + dumpSha);
        if (!dumpSha.equals(recheckedSha)) {
            throw new Matrix26BackupException("The backup checksum changed during verification.");
        }

        writeManifest(manifest, job, instance, connection, tool, tableCount, databaseSize, dumpSize, compressedSize, dumpSha);
        writeReport(report, job, instance, tool, tableCount, databaseSize, dumpSize, compressedSize, dumpSha, stderr);
        Files.deleteIfExists(stderr);

        backupRepository.insertArtifact(job.id(), "DATABASE_DUMP", compressed.getFileName().toString(),
                relative(root, compressed), compressedSize, dumpSha, "VERIFIED");
        backupRepository.insertArtifact(job.id(), "MANIFEST", manifest.getFileName().toString(),
                relative(root, manifest), Files.size(manifest), sha256(manifest), "VERIFIED");
        backupRepository.insertArtifact(job.id(), "CHECKSUMS", checksums.getFileName().toString(),
                relative(root, checksums), Files.size(checksums), sha256(checksums), "VERIFIED");
        backupRepository.insertArtifact(job.id(), "REPORT", report.getFileName().toString(),
                relative(root, report), Files.size(report), sha256(report), "VERIFIED");

        if (fullBackup) {
            backupRepository.updateStatus(job.id(), Matrix26BackupStatus.VERIFYING);
        } else {
            backupRepository.complete(
                    job.id(),
                    databaseSize,
                    dumpSize,
                    compressedSize,
                    tableCount,
                    dumpSha,
                    relative(root, manifest),
                    relative(root, report),
                    "All required database backup checks passed."
            );
        }
    }

    private void finalizeFullBackup(
            Matrix26BackupJob job,
            PlatformBusinessClient instance,
            Matrix26DatabaseConnectionInfo connection,
            Matrix26BackupToolStatus tool,
            Path root,
            Path directory,
            Matrix26FullBackupResult result
    ) throws IOException {
        Path compressed = directory.resolve("database.sql.gz");
        Path manifest = directory.resolve("manifest.json");
        Path checksums = directory.resolve("checksums.sha256");
        Path report = directory.resolve("backup-report.txt");

        LinkedHashMap<Path, String> hashes = new LinkedHashMap<>();
        hashes.put(compressed, sha256(compressed));
        hashes.putAll(result.hashes());

        writeFullManifest(manifest, job, instance, connection, tool, result, hashes);
        hashes.put(manifest, sha256(manifest));

        StringBuilder checksumContent = new StringBuilder();
        for (Map.Entry<Path, String> entry : hashes.entrySet()) {
            checksumContent.append(entry.getValue())
                    .append("  ")
                    .append(entry.getKey().getFileName())
                    .append(System.lineSeparator());
        }
        Files.writeString(checksums, checksumContent, StandardCharsets.UTF_8);

        boolean hashesMatch = true;
        for (Map.Entry<Path, String> entry : hashes.entrySet()) {
            if (!entry.getValue().equals(sha256(entry.getKey()))) {
                hashesMatch = false;
                break;
            }
        }
        verify(job.id(), "FULL_ARTIFACT_SHA256", "Full package SHA-256 checksums", hashesMatch,
                hashesMatch
                        ? hashes.size() + " recovery artifacts passed SHA-256 verification."
                        : "At least one full-backup artifact changed during verification.");
        if (!hashesMatch) {
            throw new Matrix26BackupException("The full-backup artifact checksums did not pass verification.");
        }

        writeFullReport(report, job, instance, tool, result, hashes);

        backupRepository.deleteMetadataArtifacts(job.id());
        backupRepository.insertArtifact(job.id(), "MANIFEST", manifest.getFileName().toString(),
                relative(root, manifest), Files.size(manifest), sha256(manifest), "VERIFIED");
        backupRepository.insertArtifact(job.id(), "CHECKSUMS", checksums.getFileName().toString(),
                relative(root, checksums), Files.size(checksums), sha256(checksums), "VERIFIED");
        backupRepository.insertArtifact(job.id(), "REPORT", report.getFileName().toString(),
                relative(root, report), Files.size(report), sha256(report), "VERIFIED");

        long totalStored = Files.size(compressed)
                + result.storedBytes()
                + Files.size(manifest)
                + Files.size(checksums)
                + Files.size(report);
        String packageSha = sha256(checksums);
        backupRepository.complete(
                job.id(),
                value(job.databaseSizeBytes()),
                value(job.dumpSizeBytes()),
                totalStored,
                job.tableCount() == null ? 0 : job.tableCount(),
                packageSha,
                relative(root, manifest),
                relative(root, report),
                result.stableInventory()
                        ? "Database, runtime configuration, modules, appearance, and instance resources passed verification."
                        : "The full package passed verification with a file-change warning. Restore testing is recommended."
        );
    }

    private void writeFullManifest(
            Path manifest,
            Matrix26BackupJob job,
            PlatformBusinessClient instance,
            Matrix26DatabaseConnectionInfo connection,
            Matrix26BackupToolStatus tool,
            Matrix26FullBackupResult result,
            Map<Path, String> hashes
    ) throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("formatVersion", 2);
        values.put("backupId", job.publicId());
        values.put("backupType", "MANUAL_FULL");
        values.put("consistencyMode", "ONLINE_CONSISTENT");
        values.put("instanceId", instance.getId());
        values.put("instanceCode", instance.getCode());
        values.put("instanceName", instance.getBusinessName());
        values.put("databaseName", connection.databaseName());
        values.put("databaseHost", connection.host());
        values.put("databasePort", connection.port());
        values.put("runtimeProfile", instance.getRuntimeProfile());
        values.put("runtimePort", instance.getRuntimePort());
        values.put("createdAt", LocalDateTime.now().toString());
        values.put("createdAtUtc", java.time.Instant.now().toString());
        values.put("tool", tool.version());
        values.put("tableCount", job.tableCount());
        values.put("databaseSizeBytes", job.databaseSizeBytes());
        values.put("databaseDumpBytes", job.compressedSizeBytes());
        values.put("instanceArchiveEntries", result.archiveEntries());
        values.put("instanceArchiveSourceBytes", result.sourceBytes());
        values.put("instanceArchiveBytes", result.archiveBytes());
        values.put("fileInventoryStable", result.stableInventory());
        values.put("skippedSymlinks", result.skippedSymlinks());
        values.put("artifactCount", hashes.size());
        values.put("containsDatabase", true);
        values.put("containsResources", true);
        values.put("containsRuntimeConfiguration", true);
        values.put("containsAppearance", true);
        values.put("containsModules", true);
        values.put("credentialsRedacted", true);
        Files.writeString(manifest, toJson(values), StandardCharsets.UTF_8);
    }

    private void writeFullReport(
            Path report,
            Matrix26BackupJob job,
            PlatformBusinessClient instance,
            Matrix26BackupToolStatus tool,
            Matrix26FullBackupResult result,
            Map<Path, String> hashes
    ) throws IOException {
        String content = "Matrix26 full instance backup report" + System.lineSeparator()
                + "Backup ID: " + job.publicId() + System.lineSeparator()
                + "Instance: " + instance.getBusinessName() + " (" + instance.getCode() + ")" + System.lineSeparator()
                + "Database: " + instance.getDatabaseName() + System.lineSeparator()
                + "Runtime: " + instance.getRuntimeProfile() + System.lineSeparator()
                + "Requested by: " + job.requestedBy() + System.lineSeparator()
                + "Created at: " + LocalDateTime.now() + System.lineSeparator()
                + "Tool: " + tool.version() + System.lineSeparator()
                + "Database tables: " + (job.tableCount() == null ? 0 : job.tableCount()) + System.lineSeparator()
                + "Database dump: " + formatBytes(job.compressedSizeBytes()) + System.lineSeparator()
                + "Instance archive entries: " + result.archiveEntries() + System.lineSeparator()
                + "Instance source size: " + formatBytes(result.sourceBytes()) + System.lineSeparator()
                + "Instance archive size: " + formatBytes(result.archiveBytes()) + System.lineSeparator()
                + "File inventory stable: " + result.stableInventory() + System.lineSeparator()
                + "Skipped symbolic links: " + result.skippedSymlinks() + System.lineSeparator()
                + "Verified recovery artifacts: " + hashes.size() + System.lineSeparator()
                + "Credentials: REDACTED" + System.lineSeparator()
                + "Verification: PASSED" + System.lineSeparator();
        Files.writeString(report, content, StandardCharsets.UTF_8);
    }

    private long value(Long number) {
        return number == null ? 0L : number;
    }

    private Matrix26BackupCandidate candidate(PlatformBusinessClient instance) {
        boolean allowed = instance.getCode() != null
                && properties.getAllowedInstanceCodes().stream()
                .anyMatch(code -> code.equalsIgnoreCase(instance.getCode()));
        String reason = allowed
                ? ""
                : "Backups are not enabled for this instance in Phase 3E.2.";
        if (instance.getDatabaseName() == null || !SAFE_IDENTIFIER.matcher(instance.getDatabaseName()).matches()) {
            allowed = false;
            reason = "The instance does not have a valid isolated database name.";
        }
        return new Matrix26BackupCandidate(
                instance.getId(),
                instance.getCode(),
                instance.getBusinessName(),
                instance.getDatabaseName(),
                instance.getRuntimeProfile(),
                instance.getRuntimePort(),
                allowed,
                reason
        );
    }

    private Matrix26DatabaseConnectionInfo connectionInfo(PlatformBusinessClient instance) {
        if (instance.getRuntimeProfile() == null || instance.getRuntimeProfile().isBlank()) {
            throw new Matrix26BackupException("The instance does not have a runtime profile.");
        }
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path runtimeRoot = projectRoot.resolve(properties.getRuntimeDirectory()).normalize();
        Path config = runtimeRoot.resolve(instance.getRuntimeProfile()).resolve("application.properties").normalize();
        ensureInside(runtimeRoot, config);
        if (!Files.isRegularFile(config)) {
            throw new Matrix26BackupException("Runtime application.properties was not found for the selected instance.");
        }

        Properties runtime = new Properties();
        try (InputStream input = Files.newInputStream(config)) {
            runtime.load(input);
        } catch (IOException ex) {
            throw new Matrix26BackupException("Runtime configuration could not be read.", ex);
        }

        String jdbcUrl = resolveExpression(runtime.getProperty("spring.datasource.url", ""));
        Matcher matcher = JDBC_MYSQL.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new Matrix26BackupException("The runtime does not use a supported MySQL JDBC URL.");
        }
        String host = matcher.group(1);
        int port = matcher.group(2) == null ? 3306 : Integer.parseInt(matcher.group(2));
        String database = matcher.group(3);
        String username = resolveExpression(runtime.getProperty("spring.datasource.username", "root"));
        String password = resolveExpression(runtime.getProperty("spring.datasource.password", ""));
        return new Matrix26DatabaseConnectionInfo(host, port, database, username, password, jdbcUrl);
    }

    private void validateTarget(PlatformBusinessClient instance, Matrix26DatabaseConnectionInfo connection) {
        if (!SAFE_IDENTIFIER.matcher(connection.databaseName()).matches()) {
            throw new Matrix26BackupException("The runtime database name is not a safe MySQL identifier.");
        }
        if (!connection.databaseName().equalsIgnoreCase(instance.getDatabaseName())) {
            throw new Matrix26BackupException("The runtime database does not match the database registered in Matrix26.");
        }
        if ("matrix26_platform_control".equalsIgnoreCase(connection.databaseName())) {
            throw new Matrix26BackupException("The Matrix26 control database cannot be selected as a client backup target.");
        }
    }

    private Path prepareBackupRoot() throws IOException {
        Path root = backupRoot();
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        if (root.startsWith(projectRoot)) {
            throw new Matrix26BackupException("The backup root must be outside the project directory.");
        }
        Files.createDirectories(root);
        if (!Files.isDirectory(root) || !Files.isWritable(root)) {
            throw new Matrix26BackupException("The configured backup root is not writable: " + root);
        }
        return root;
    }

    private void ensureFreeSpace(Path directory) throws IOException {
        FileStore store = Files.getFileStore(directory);
        long required = Math.max(64L * 1024L * 1024L, properties.getMinimumFreeBytes());
        if (store.getUsableSpace() < required) {
            throw new Matrix26BackupException(
                    "Insufficient free storage. Required: " + formatBytes(required)
                            + ", available: " + formatBytes(store.getUsableSpace()) + "."
            );
        }
    }

    private List<String> dumpCommand(
            String executable,
            Matrix26DatabaseConnectionInfo connection,
            Path rawDump
    ) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("--single-transaction");
        command.add("--quick");
        command.add("--routines");
        command.add("--triggers");
        command.add("--events");
        command.add("--hex-blob");
        command.add("--default-character-set=utf8mb4");
        command.add("--host=" + connection.host());
        command.add("--port=" + connection.port());
        command.add("--user=" + connection.username());
        command.add("--result-file=" + rawDump.toAbsolutePath().normalize());
        command.add(connection.databaseName());
        return command;
    }

    private void prependOwnershipHeader(
            Path rawDump,
            Path finalDump,
            Matrix26BackupJob job,
            Matrix26DatabaseConnectionInfo connection
    ) throws IOException {
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(finalDump));
             InputStream input = new BufferedInputStream(Files.newInputStream(rawDump))) {
            String header = "-- Matrix26 verified database backup" + System.lineSeparator()
                    + "-- Backup ID: " + job.publicId() + System.lineSeparator()
                    + "-- Instance code: " + job.instanceCode() + System.lineSeparator()
                    + "-- Database: " + connection.databaseName() + System.lineSeparator()
                    + "-- Created at: " + LocalDateTime.now() + System.lineSeparator()
                    + System.lineSeparator();
            output.write(header.getBytes(StandardCharsets.UTF_8));
            input.transferTo(output);
        }
    }

    private void gzip(Path source, Path target) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".part");
        try (InputStream input = new BufferedInputStream(Files.newInputStream(source));
             GZIPOutputStream output = new GZIPOutputStream(
                     new BufferedOutputStream(Files.newOutputStream(temporary))
             )) {
            input.transferTo(output);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private GzipInspection inspectGzip(Path compressed, String databaseName) {
        int createTables = 0;
        boolean databaseMarker = false;
        Pattern createTable = Pattern.compile("^CREATE TABLE(?: IF NOT EXISTS)? ", Pattern.CASE_INSENSITIVE);
        try (GZIPInputStream gzip = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(compressed)));
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("-- Database: " + databaseName)) {
                    databaseMarker = true;
                }
                if (createTable.matcher(line).find()) {
                    createTables++;
                }
            }
            return new GzipInspection(true, databaseMarker, createTables,
                    "The GZIP archive is readable and contains " + createTables + " CREATE TABLE statements.");
        } catch (IOException ex) {
            return new GzipInspection(false, false, 0, "The GZIP archive could not be read: " + safeMessage(ex));
        }
    }

    private int tableCount(String databaseName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ? AND table_type = 'BASE TABLE'
                """,
                Integer.class,
                databaseName
        );
        return count == null ? 0 : count;
    }

    private long databaseSize(String databaseName) {
        Long size = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(data_length + index_length), 0)
                FROM information_schema.tables
                WHERE table_schema = ?
                """,
                Long.class,
                databaseName
        );
        return size == null ? 0L : size;
    }

    private void verify(long jobId, String code, String label, boolean passed, String detail) {
        backupRepository.insertVerification(
                jobId,
                code,
                label,
                passed ? Matrix26BackupVerificationStatus.PASSED : Matrix26BackupVerificationStatus.FAILED,
                detail
        );
    }

    private void writeManifest(
            Path manifest,
            Matrix26BackupJob job,
            PlatformBusinessClient instance,
            Matrix26DatabaseConnectionInfo connection,
            Matrix26BackupToolStatus tool,
            int tableCount,
            long databaseSize,
            long dumpSize,
            long compressedSize,
            String sha256
    ) throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("formatVersion", 1);
        values.put("backupId", job.publicId());
        values.put("backupType", "MANUAL_DATABASE");
        values.put("instanceId", instance.getId());
        values.put("instanceCode", instance.getCode());
        values.put("instanceName", instance.getBusinessName());
        values.put("databaseName", connection.databaseName());
        values.put("databaseHost", connection.host());
        values.put("databasePort", connection.port());
        values.put("runtimeProfile", instance.getRuntimeProfile());
        values.put("createdAt", LocalDateTime.now().toString());
        values.put("createdAtUtc", java.time.Instant.now().toString());
        values.put("tool", tool.version());
        values.put("tableCount", tableCount);
        values.put("databaseSizeBytes", databaseSize);
        values.put("sqlSizeBytes", dumpSize);
        values.put("compressedSizeBytes", compressedSize);
        values.put("artifact", "database.sql.gz");
        values.put("sha256", sha256);
        values.put("containsResources", false);
        values.put("containsRuntimeConfiguration", false);
        Files.writeString(manifest, toJson(values), StandardCharsets.UTF_8);
    }

    private void writeReport(
            Path report,
            Matrix26BackupJob job,
            PlatformBusinessClient instance,
            Matrix26BackupToolStatus tool,
            int tableCount,
            long databaseSize,
            long dumpSize,
            long compressedSize,
            String sha256,
            Path stderr
    ) throws IOException {
        String warnings = sanitizedFile(stderr);
        String content = "Matrix26 database backup report" + System.lineSeparator()
                + "Backup ID: " + job.publicId() + System.lineSeparator()
                + "Instance: " + instance.getBusinessName() + " (" + instance.getCode() + ")" + System.lineSeparator()
                + "Database: " + instance.getDatabaseName() + System.lineSeparator()
                + "Requested by: " + job.requestedBy() + System.lineSeparator()
                + "Created at: " + LocalDateTime.now() + System.lineSeparator()
                + "Tool: " + tool.version() + System.lineSeparator()
                + "Tables: " + tableCount + System.lineSeparator()
                + "Database size: " + formatBytes(databaseSize) + System.lineSeparator()
                + "SQL size: " + formatBytes(dumpSize) + System.lineSeparator()
                + "Compressed size: " + formatBytes(compressedSize) + System.lineSeparator()
                + "SHA-256: " + sha256 + System.lineSeparator()
                + "Verification: PASSED" + System.lineSeparator()
                + (warnings.isBlank() ? "" : "Tool warnings: " + warnings + System.lineSeparator());
        Files.writeString(report, content, StandardCharsets.UTF_8);
    }

    private void writeFailureReport(Path directory, Matrix26BackupJob job, String error) {
        if (directory == null || job == null) {
            return;
        }
        try {
            Files.createDirectories(directory);
            Files.writeString(
                    directory.resolve("backup-report.txt"),
                    "Matrix26 database backup report" + System.lineSeparator()
                            + "Backup ID: " + job.publicId() + System.lineSeparator()
                            + "Status: FAILED" + System.lineSeparator()
                            + "Error: " + sanitize(error) + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException ignored) {
            // The original failure remains the primary diagnostic.
        }
    }

    @Transactional
    protected void writeAudit(PlatformBusinessClient instance, String actor, String action, String summary) {
        Matrix26InstanceAuditLog log = new Matrix26InstanceAuditLog();
        log.setInstance(instance);
        log.setActorUsername(safeActor(actor));
        log.setAction(action);
        log.setSummary(limit(summary, 500));
        log.setAfterSnapshot("{\"database\":\"" + json(instance.getDatabaseName()) + "\"}");
        auditLogRepository.save(log);
    }

    private String resolveExpression(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        Matcher matcher = Pattern.compile("^\\$\\{([A-Za-z0-9_.-]+)(?::(.*))?}$").matcher(value);
        if (!matcher.matches()) {
            return value;
        }
        String environmentValue = System.getenv(matcher.group(1));
        if (environmentValue != null) {
            return environmentValue;
        }
        String systemValue = System.getProperty(matcher.group(1));
        if (systemValue != null) {
            return systemValue;
        }
        return matcher.group(2) == null ? "" : matcher.group(2);
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private String sanitizedFile(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return "";
        }
        try {
            return sanitize(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            return "Diagnostic unavailable";
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = value
                .replaceAll("(?i)(password|passwd|secret|token|api[_\\-.]?key)(\\s*[:=]\\s*)([^\\s,;]+)", "$1$2***REDACTED***")
                .replace('\u0000', ' ')
                .replaceAll("[\\r\\n]+", " ")
                .trim();
        return limit(sanitized, 1200);
    }

    private String safeMessage(Exception ex) {
        return sanitize(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
    }

    private String safeActor(String actor) {
        return actor == null || actor.isBlank() ? "matrix26-system" : limit(actor.trim(), 120);
    }

    private void ensureInside(Path parent, Path child) {
        if (!child.toAbsolutePath().normalize().startsWith(parent.toAbsolutePath().normalize())) {
            throw new Matrix26BackupException("A generated path escaped the configured storage boundary.");
        }
    }

    private String relative(Path root, Path file) {
        return root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize())
                .toString().replace('\\', '/');
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }

    private String toJson(Map<String, Object> values) {
        StringBuilder json = new StringBuilder("{\n");
        int index = 0;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (index++ > 0) {
                json.append(",\n");
            }
            json.append("  \"").append(json(entry.getKey())).append("\": ");
            Object value = entry.getValue();
            if (value == null) {
                json.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(json(value.toString())).append("\"");
            }
        }
        return json.append("\n}\n").toString();
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String limit(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value;
        }
        return value.substring(0, Math.max(0, maximum - 1)) + "…";
    }

    public static String formatBytes(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "—";
        }
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1024.0 && unit < units.length - 1) {
            value /= 1024.0;
            unit++;
        }
        return unit == 0
                ? String.format(Locale.ROOT, "%.0f %s", value, units[unit])
                : String.format(Locale.ROOT, "%.2f %s", value, units[unit]);
    }

    private record GzipInspection(
            boolean readable,
            boolean databaseMarker,
            int createTableCount,
            String message
    ) {
    }
}
