package com.ecoamazonas.eco_agua.platform.control.restores;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLog;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLogRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceHealthService;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceManagementService;
import com.ecoamazonas.eco_agua.platform.control.Matrix26TargetDatabaseService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupEncryption;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupExtraction;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupJob;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupProperties;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupSecurityService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupVerificationState;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlResult;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26RestoreService {

    private static final Pattern JDBC_MYSQL = Pattern.compile("^jdbc:mysql://([^/:?]+)(?::(\\d+))?/([^?]+)(?:\\?.*)?$");
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Matrix26RestoreRepository restoreRepository;
    private final Matrix26BackupRepository backupRepository;
    private final Matrix26BackupSecurityService backupSecurityService;
    private final Matrix26BackupProperties backupProperties;
    private final Matrix26RestoreProperties properties;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26InstanceManagementService instanceManagementService;
    private final Matrix26TargetDatabaseService targetDatabaseService;
    private final Matrix26RuntimeControlService runtimeControlService;
    private final Matrix26InstanceHealthService healthService;
    private final Matrix26InstanceAuditLogRepository auditRepository;
    private final String controlJdbcUrl;
    private final String databaseUsername;
    private final String databasePassword;

    public Matrix26RestoreService(
            Matrix26RestoreRepository restoreRepository,
            Matrix26BackupRepository backupRepository,
            Matrix26BackupSecurityService backupSecurityService,
            Matrix26BackupProperties backupProperties,
            Matrix26RestoreProperties properties,
            PlatformBusinessClientRepository clientRepository,
            Matrix26InstanceManagementService instanceManagementService,
            Matrix26TargetDatabaseService targetDatabaseService,
            Matrix26RuntimeControlService runtimeControlService,
            Matrix26InstanceHealthService healthService,
            Matrix26InstanceAuditLogRepository auditRepository,
            @Value("${spring.datasource.url}") String controlJdbcUrl,
            @Value("${spring.datasource.username}") String databaseUsername,
            @Value("${spring.datasource.password:}") String databasePassword
    ) {
        this.restoreRepository = restoreRepository;
        this.backupRepository = backupRepository;
        this.backupSecurityService = backupSecurityService;
        this.backupProperties = backupProperties;
        this.properties = properties;
        this.clientRepository = clientRepository;
        this.instanceManagementService = instanceManagementService;
        this.targetDatabaseService = targetDatabaseService;
        this.runtimeControlService = runtimeControlService;
        this.healthService = healthService;
        this.auditRepository = auditRepository;
        this.controlJdbcUrl = controlJdbcUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
    }

    public List<Matrix26RestoreCandidate> candidates() {
        List<Matrix26RestoreCandidate> result = new ArrayList<>();
        for (Matrix26BackupJob backup : backupRepository.findRecent()) {
            Matrix26BackupEncryption encryption = backupSecurityService.metadata(backup.id());
            boolean allowedSource = properties.getAllowedSourceInstanceCodes().stream()
                    .anyMatch(code -> code.equalsIgnoreCase(backup.instanceCode()));
            boolean full = "MANUAL_FULL".equalsIgnoreCase(backup.backupType())
                    || "SCHEDULED_FULL".equalsIgnoreCase(backup.backupType());
            boolean verified = encryption != null
                    && encryption.encrypted()
                    && encryption.verificationStatus() == Matrix26BackupVerificationState.VERIFIED;
            boolean eligible = backup.isCompleted() && allowedSource && full && verified;
            String reason = eligible ? "Ready for isolated clone restoration."
                    : !allowedSource ? "Source instance is outside the restore allowlist."
                    : !full ? "Only full instance backups can be restored as clones."
                    : !verified ? "Backup must be encrypted and verified."
                    : "Backup is not completed.";
            if (allowedSource) {
                result.add(new Matrix26RestoreCandidate(backup, encryption, eligible, reason));
            }
        }
        return result;
    }

    public Matrix26RestoreSummary summary() { return restoreRepository.summary(); }
    public List<Matrix26RestoreJob> recentJobs() { return restoreRepository.findRecent(); }
    public Matrix26RestoreJob job(long id) { return restoreRepository.findById(id).orElseThrow(() -> new Matrix26RestoreException("Restore job not found.")); }
    public List<Matrix26RestoreStep> steps(long id) { return restoreRepository.findSteps(id); }
    public List<Matrix26RestoreArtifact> artifacts(long id) { return restoreRepository.findArtifacts(id); }
    public List<Matrix26RestoreVerification> verifications(long id) { return restoreRepository.findVerifications(id); }
    public Matrix26RestoreProperties properties() { return properties; }

    public Matrix26RestoreJob restoreClone(long backupJobId, boolean startAfterRestore, String confirmation, String actor) {
        if (!properties.isEnabled()) {
            throw new Matrix26RestoreException("Restore Manager is disabled.");
        }
        if (restoreRepository.hasActiveRestore()) {
            throw new Matrix26RestoreException("Another restore job is already active.");
        }
        Matrix26RestoreCandidate candidate = candidates().stream()
                .filter(item -> item.backup().id().equals(backupJobId))
                .findFirst()
                .orElseThrow(() -> new Matrix26RestoreException("The selected backup is not available for restoration."));
        if (!candidate.eligible()) {
            throw new Matrix26RestoreException(candidate.reason());
        }
        String expectedConfirmation = "RESTORE " + properties.getTargetInstanceCode();
        if (!expectedConfirmation.equals(confirmation == null ? "" : confirmation.trim())) {
            throw new Matrix26RestoreException("Type exactly: " + expectedConfirmation);
        }
        validateTargetAvailable();

        Matrix26BackupJob backup = candidate.backup();
        String publicId = "RST-" + LocalDateTime.now().format(ID_TIME) + "-" + Long.toHexString(System.nanoTime()).toUpperCase(Locale.ROOT);
        Matrix26RestoreJob draft = new Matrix26RestoreJob(
                null, publicId, backup.id(), backup.publicId(), backup.instanceId(), backup.instanceCode(),
                backup.instanceName(), backup.databaseName(), null, properties.getTargetInstanceCode(),
                properties.getTargetInstanceName(), properties.getTargetDatabaseName(), properties.getTargetRuntimeProfile(),
                properties.getTargetRuntimePort(), properties.getTargetPublicUrl(), Matrix26RestoreStatus.DRAFT,
                startAfterRestore, safeActor(actor), LocalDateTime.now(), null, null, null, null
        );
        long jobId = restoreRepository.insertJob(draft);
        createSteps(jobId);
        execute(jobId, candidate, actor);
        return job(jobId);
    }

    private void execute(long jobId, Matrix26RestoreCandidate candidate, String actor) {
        Matrix26RestoreJob job = job(jobId);
        Path tempRoot = backupRoot().resolve(".matrix26-restore-temp").normalize();
        Path work = tempRoot.resolve(job.publicId()).normalize();
        boolean databaseCreated = false;
        boolean filesCreated = false;
        boolean instanceRegistered = false;
        String currentStep = "VALIDATE_BACKUP";
        try {
            Files.createDirectories(work);
            restoreRepository.markStarted(jobId, work.toString());

            runStep(jobId, "VALIDATE_BACKUP", Matrix26RestoreStatus.VALIDATING,
                    "Validating encrypted backup and isolated destination.", () -> {
                        validateTargetAvailable();
                        restoreRepository.insertVerification(jobId, "TARGET_ISOLATION", "Target isolation", "PASSED",
                                "Database, port, runtime profile, instance code, and directories are available.");
                    });

            currentStep = "DECRYPT_PACKAGE";
            restoreRepository.startStep(jobId, currentStep, "Decrypting AES-256-GCM package and checking internal hashes.");
            restoreRepository.updateJobStatus(jobId, Matrix26RestoreStatus.DECRYPTING);
            Matrix26BackupExtraction extraction = backupSecurityService.extractVerifiedBackup(candidate.backup().id(), work.resolve("decrypted"));
            Path payload = extraction.extractedDirectory();
            restoreRepository.completeStep(jobId, currentStep, extraction.verificationMessage());
            restoreRepository.insertVerification(jobId, "ENCRYPTED_PACKAGE", "Encrypted package", "PASSED", extraction.verificationMessage());
            restoreRepository.insertArtifact(jobId, "SOURCE_PACKAGE", candidate.encryption().packagePath(),
                    candidate.encryption().packageSizeBytes(), candidate.encryption().packageSha256(), "VERIFIED");

            currentStep = "CREATE_DATABASE";
            restoreRepository.startStep(jobId, currentStep, "Creating isolated database " + job.targetDatabaseName() + ".");
            restoreRepository.updateJobStatus(jobId, Matrix26RestoreStatus.CREATING_DATABASE);
            targetDatabaseService.createDatabase(job.targetDatabaseName(), false);
            databaseCreated = true;
            restoreRepository.completeStep(jobId, currentStep, "Empty isolated database created.");

            currentStep = "IMPORT_DATABASE";
            restoreRepository.startStep(jobId, currentStep, "Importing verified SQL export into the isolated database.");
            restoreRepository.updateJobStatus(jobId, Matrix26RestoreStatus.IMPORTING_DATABASE);
            Path sql = work.resolve("database.sql");
            gunzip(payload.resolve("database.sql.gz"), sql);
            importDatabase(job.targetDatabaseName(), sql, work.resolve("database-import.stderr.log"));
            int tables = targetDatabaseService.tableCount(job.targetDatabaseName());
            if (tables <= 0) {
                throw new Matrix26RestoreException("Database import completed without restored tables.");
            }
            adaptRestoredDatabase(job, candidate.backup().instanceCode());
            restoreRepository.completeStep(jobId, currentStep, tables + " tables restored and clone identifiers adapted.");
            restoreRepository.insertVerification(jobId, "DATABASE_TABLES", "Restored database tables", "PASSED", tables + " base tables detected.");
            restoreRepository.insertArtifact(jobId, "RESTORED_DATABASE", "mysql://" + job.targetDatabaseName(), null, null, "READY");
            Files.deleteIfExists(sql);

            currentStep = "RESTORE_FILES";
            restoreRepository.startStep(jobId, currentStep, "Restoring instance-owned files into a new namespace.");
            restoreRepository.updateJobStatus(jobId, Matrix26RestoreStatus.RESTORING_FILES);
            filesCreated = true;
            int restoredFiles = restoreInstanceFiles(payload.resolve("instance-files.zip"), candidate.backup(), job);
            restoreRepository.completeStep(jobId, currentStep, restoredFiles + " files restored under the clone namespace.");
            restoreRepository.insertVerification(jobId, "RESTORED_FILESET", "Restored files", "PASSED", restoredFiles + " safe ZIP entries restored.");
            restoreRepository.insertArtifact(jobId, "RUNTIME_DATA", "runtime-data/" + job.targetInstanceCode(), null, null, "RESTORED");

            currentStep = "GENERATE_RUNTIME";
            restoreRepository.startStep(jobId, currentStep, "Generating clone runtime without exposing credentials.");
            restoreRepository.updateJobStatus(jobId, Matrix26RestoreStatus.GENERATING_RUNTIME);
            generateRuntime(candidate.backup(), job);
            restoreRepository.completeStep(jobId, currentStep, "Runtime generated at runtime-clients/" + job.targetRuntimeProfile() + ".");
            restoreRepository.insertArtifact(jobId, "RUNTIME_CONFIGURATION", "runtime-clients/" + job.targetRuntimeProfile(), null, null, "GENERATED");

            currentStep = "REGISTER_INSTANCE";
            restoreRepository.startStep(jobId, currentStep, "Registering the restored clone in Matrix26.");
            restoreRepository.updateJobStatus(jobId, Matrix26RestoreStatus.REGISTERING_INSTANCE);
            PlatformBusinessClient target = registerClone(candidate.backup(), job, actor);
            instanceRegistered = true;
            restoreRepository.setTargetInstance(jobId, target.getId());
            restoreRepository.completeStep(jobId, currentStep, "Clone registered with instance ID " + target.getId() + ".");

            if (job.startAfterRestore()) {
                currentStep = "START_RUNTIME";
                restoreRepository.startStep(jobId, currentStep, "Starting restored runtime through Runtime Control Center.");
                restoreRepository.updateJobStatus(jobId, Matrix26RestoreStatus.STARTING_RUNTIME);
                Matrix26RuntimeControlResult result = runtimeControlService.start(String.valueOf(target.getId()), actor);
                restoreRepository.completeStep(jobId, currentStep, result.message());

                currentStep = "HEALTH_CHECK";
                restoreRepository.startStep(jobId, currentStep, "Checking restored portal availability.");
                restoreRepository.updateJobStatus(jobId, Matrix26RestoreStatus.HEALTH_CHECKING);
                var status = healthService.refreshInstance(target.getId());
                if (!status.online()) {
                    throw new Matrix26RestoreException("The clone runtime started but the health check remained offline: " + status.message());
                }
                restoreRepository.completeStep(jobId, currentStep, "HTTP health check passed at " + job.targetPublicUrl() + ".");
                restoreRepository.insertVerification(jobId, "HTTP_HEALTH", "Clone HTTP health", "PASSED", status.message());
            } else {
                restoreRepository.skipStep(jobId, "START_RUNTIME", "Start after restore was not requested.");
                restoreRepository.skipStep(jobId, "HEALTH_CHECK", "Health check will run after the clone is started manually.");
            }

            restoreRepository.complete(jobId);
            writeAudit(clientRepository.findById(job(jobId).targetInstanceId()).orElseThrow(), actor,
                    "RESTORE_CLONE_COMPLETED", "Encrypted backup " + candidate.backup().publicId() + " restored as clone " + job.targetInstanceCode() + ".");
        } catch (Exception ex) {
            String error = safeMessage(ex);
            restoreRepository.failStep(jobId, currentStep, error);
            Matrix26RestoreStatus failure = (databaseCreated || filesCreated || instanceRegistered)
                    ? Matrix26RestoreStatus.CLEANUP_REQUIRED : Matrix26RestoreStatus.FAILED;
            restoreRepository.fail(jobId, failure, error);
            clientRepository.findById(candidate.backup().instanceId()).ifPresent(source ->
                    writeAudit(source, actor, "RESTORE_CLONE_FAILED", "Restore job " + job.publicId() + " failed: " + error));
            throw ex instanceof Matrix26RestoreException restoreException
                    ? restoreException
                    : new Matrix26RestoreException("Clone restoration failed: " + error, ex);
        } finally {
            deleteTreeQuietly(work);
        }
    }

    private void runStep(long jobId, String code, Matrix26RestoreStatus status, String detail, CheckedAction action) throws Exception {
        restoreRepository.startStep(jobId, code, detail);
        restoreRepository.updateJobStatus(jobId, status);
        action.run();
        restoreRepository.completeStep(jobId, code, detail.replace("Validating", "Validated"));
    }

    private void validateTargetAvailable() {
        String code = safeIdentifier(properties.getTargetInstanceCode());
        String database = safeIdentifier(properties.getTargetDatabaseName());
        String runtime = safeIdentifier(properties.getTargetRuntimeProfile());
        int port = properties.getTargetRuntimePort();
        if (clientRepository.existsByCodeIgnoreCase(code)) throw new Matrix26RestoreException("Target instance code already exists: " + code);
        if (clientRepository.existsByDatabaseNameIgnoreCase(database)) throw new Matrix26RestoreException("Target database is already registered: " + database);
        if (clientRepository.existsByRuntimeProfileIgnoreCase(runtime)) throw new Matrix26RestoreException("Target runtime profile already exists: " + runtime);
        if (clientRepository.existsByRuntimePort(port)) throw new Matrix26RestoreException("Target port is already registered: " + port);
        if (targetDatabaseService.databaseExists(database)) throw new Matrix26RestoreException("Target database already exists: " + database);
        Path runtimeDir = projectRoot().resolve(properties.getRuntimeDirectory()).resolve(runtime).normalize();
        Path dataDir = projectRoot().resolve(properties.getRuntimeDataDirectory()).resolve(code).normalize();
        if (Files.exists(runtimeDir)) throw new Matrix26RestoreException("Target runtime directory already exists: " + runtimeDir);
        if (Files.exists(dataDir)) throw new Matrix26RestoreException("Target runtime-data directory already exists: " + dataDir);
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
        } catch (IOException ex) {
            throw new Matrix26RestoreException("Target port " + port + " is already occupied.");
        }
    }

    private void importDatabase(String databaseName, Path sql, Path stderr) throws IOException, InterruptedException {
        DatabaseTarget target = databaseTarget();
        String executable = importExecutable();
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("--host=" + target.host());
        command.add("--port=" + target.port());
        command.add("--user=" + databaseUsername);
        command.add("--default-character-set=utf8mb4");
        command.add("--binary-mode=1");
        command.add(databaseName);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectInput(sql.toFile());
        builder.redirectError(stderr.toFile());
        if (databasePassword != null && !databasePassword.isBlank()) builder.environment().put("MYSQL_PWD", databasePassword);
        else builder.environment().remove("MYSQL_PWD");
        Process process = builder.start();
        boolean finished = process.waitFor(Math.max(60L, properties.getProcessTimeoutSeconds()), TimeUnit.SECONDS);
        if (!finished) {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
            throw new Matrix26RestoreException("Database import exceeded the configured timeout.");
        }
        if (process.exitValue() != 0) {
            throw new Matrix26RestoreException("Database import failed: " + sanitizedFile(stderr));
        }
        Files.deleteIfExists(stderr);
    }

    private String importExecutable() {
        if (properties.getImportExecutable() != null && !properties.getImportExecutable().isBlank()) {
            Path configured = Path.of(properties.getImportExecutable()).toAbsolutePath().normalize();
            if (Files.isRegularFile(configured)) return configured.toString();
        }
        String dump = firstNonBlank(System.getenv("MATRIX26_MYSQLDUMP_PATH"), backupProperties.getDumpExecutable());
        if (dump != null && !dump.isBlank()) {
            Path dumpPath = Path.of(dump).toAbsolutePath().normalize();
            Path parent = dumpPath.getParent();
            if (parent != null) {
                for (String name : List.of("mariadb.exe", "mysql.exe", "mariadb", "mysql")) {
                    Path candidate = parent.resolve(name);
                    if (Files.isRegularFile(candidate)) return candidate.toString();
                }
            }
        }
        throw new Matrix26RestoreException("MariaDB/MySQL import client was not found. Configure matrix26.control-center.restores.import-executable.");
    }

    private DatabaseTarget databaseTarget() {
        Matcher matcher = JDBC_MYSQL.matcher(controlJdbcUrl);
        if (!matcher.matches()) throw new Matrix26RestoreException("Matrix26 control datasource does not use a supported MySQL JDBC URL.");
        return new DatabaseTarget(matcher.group(1), matcher.group(2) == null ? 3306 : Integer.parseInt(matcher.group(2)));
    }

    private void adaptRestoredDatabase(Matrix26RestoreJob job, String sourceCode) {
        var target = targetDatabaseService.targetJdbcTemplate(job.targetDatabaseName());
        Integer table = target.queryForObject("""
                SELECT COUNT(*) FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'matrix26_instance_appearance_config'
                """, Integer.class);
        if (table != null && table > 0) {
            target.update("""
                    UPDATE matrix26_instance_appearance_config
                    SET instance_code = ?,
                        overrides_json = REPLACE(COALESCE(overrides_json, ''), ?, ?),
                        branding_json = REPLACE(COALESCE(branding_json, ''), ?, ?),
                        asset_manifest_json = REPLACE(COALESCE(asset_manifest_json, ''), ?, ?)
                    WHERE id = 1
                    """, job.targetInstanceCode(), sourceCode, job.targetInstanceCode(), sourceCode,
                    job.targetInstanceCode(), sourceCode, job.targetInstanceCode());
        }
    }

    private int restoreInstanceFiles(Path archive, Matrix26BackupJob backup, Matrix26RestoreJob job) throws IOException {
        if (!Files.isRegularFile(archive)) throw new Matrix26RestoreException("instance-files.zip is missing.");
        Path runtimeRoot = projectRoot().resolve(properties.getRuntimeDirectory()).normalize();
        Path dataRoot = projectRoot().resolve(properties.getRuntimeDataDirectory()).normalize();
        Path targetRuntime = runtimeRoot.resolve(job.targetRuntimeProfile()).normalize();
        Path targetData = dataRoot.resolve(job.targetInstanceCode()).normalize();
        int restored = 0;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName().replace('\\', '/');
                if (!safeZipEntry(name)) throw new Matrix26RestoreException("Unsafe instance archive entry: " + name);
                Path target = null;
                String dataPrefix = "runtime-data/" + backup.instanceCode() + "/";
                String runtimePrefix = "runtime-clients/" + sourceRuntimeProfile(backup) + "/";
                if (name.startsWith(dataPrefix)) {
                    target = targetData.resolve(name.substring(dataPrefix.length())).normalize();
                    ensureInside(targetData, target);
                } else if (name.startsWith(runtimePrefix) && !name.endsWith("application.properties")) {
                    target = targetRuntime.resolve(name.substring(runtimePrefix.length())).normalize();
                    ensureInside(targetRuntime, target);
                }
                if (target == null) continue;
                Files.createDirectories(target.getParent());
                if (Files.exists(target)) throw new Matrix26RestoreException("Restore target file already exists: " + target);
                try (InputStream input = zip.getInputStream(entry)) {
                    Files.copy(input, target);
                }
                restored++;
            }
        }
        return restored;
    }

    private String sourceRuntimeProfile(Matrix26BackupJob backup) {
        return clientRepository.findById(backup.instanceId()).map(PlatformBusinessClient::getRuntimeProfile)
                .orElseThrow(() -> new Matrix26RestoreException("Source instance runtime profile no longer exists."));
    }

    private void generateRuntime(Matrix26BackupJob backup, Matrix26RestoreJob job) throws IOException {
        PlatformBusinessClient source = clientRepository.findById(backup.instanceId())
                .orElseThrow(() -> new Matrix26RestoreException("Source instance no longer exists."));
        Path runtimeRoot = projectRoot().resolve(properties.getRuntimeDirectory()).normalize();
        Path sourceDir = runtimeRoot.resolve(source.getRuntimeProfile()).normalize();
        Path targetDir = runtimeRoot.resolve(job.targetRuntimeProfile()).normalize();
        ensureInside(runtimeRoot, sourceDir);
        ensureInside(runtimeRoot, targetDir);
        if (!Files.isRegularFile(sourceDir.resolve("application.properties"))) {
            throw new Matrix26RestoreException("Source runtime application.properties is missing.");
        }
        Files.createDirectories(targetDir);
        Properties runtime = new Properties();
        try (InputStream input = Files.newInputStream(sourceDir.resolve("application.properties"))) {
            runtime.load(input);
        }
        runtime.setProperty("server.port", String.valueOf(job.targetRuntimePort()));
        runtime.setProperty("spring.datasource.url", replaceDatabase(runtime.getProperty("spring.datasource.url", ""), job.targetDatabaseName()));
        runtime.setProperty("matrix26.control-center.enabled", "false");
        runtime.setProperty("ecoagua.platform.client-code", job.targetInstanceCode());
        runtime.setProperty("ecoagua.platform.runtime-profile", job.targetRuntimeProfile());
        runtime.setProperty("ecoagua.platform.public-url", job.targetPublicUrl());
        runtime.setProperty("ecoagua.business.profile-code", job.targetRuntimeProfile());
        runtime.setProperty("ecoagua.business.name", job.targetInstanceName());
        runtime.setProperty("ecoagua.business.short-name", "Restore Test");
        runtime.setProperty("ecoagua.business.footer-right", "Restored clone managed by Matrix26");
        try (OutputStream output = Files.newOutputStream(targetDir.resolve("application.properties"))) {
            runtime.store(output, "Runtime restored by Matrix26 Restore Manager");
        }
        Path runScript = targetDir.resolve("run.sh");
        Files.writeString(runScript, runtimeScript(job), StandardCharsets.UTF_8);
        runScript.toFile().setExecutable(true, false);
        Files.writeString(targetDir.resolve("README.txt"), "Matrix26 restored clone\nInstance: " + job.targetInstanceCode()
                + "\nDatabase: " + job.targetDatabaseName() + "\nPort: " + job.targetRuntimePort() + "\n", StandardCharsets.UTF_8);
        Files.writeString(targetDir.resolve(".matrix26-restore-reference"), job.publicId() + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private PlatformBusinessClient registerClone(Matrix26BackupJob backup, Matrix26RestoreJob job, String actor) {
        PlatformBusinessClient source = clientRepository.findById(backup.instanceId())
                .orElseThrow(() -> new Matrix26RestoreException("Source instance no longer exists."));
        PlatformBusinessClient clone = new PlatformBusinessClient();
        clone.setCode(job.targetInstanceCode());
        clone.setBusinessName(job.targetInstanceName());
        clone.setLegalName(job.targetInstanceName());
        clone.setBusinessType(source.getBusinessType());
        clone.setDatabaseName(job.targetDatabaseName());
        clone.setDatabaseStatus("READY");
        clone.setStatus("ACTIVE");
        clone.setCity(source.getCity());
        clone.setCurrency(source.getCurrency());
        clone.setPrimaryColor(source.getPrimaryColor());
        clone.setRuntimeProfile(job.targetRuntimeProfile());
        clone.setRuntimePort(job.targetRuntimePort());
        clone.setPublicUrl(job.targetPublicUrl());
        clone.setRuntimeStatus("STOPPED");
        clone.setManagementMode("RESTORED_CLONE");
        clone.setMonitorVisible(true);
        clone.setProtectedInstance(true);
        clone.setRuntimeCommand("bash runtime-clients/" + job.targetRuntimeProfile() + "/run.sh");
        clone.setNotes("Restored from encrypted backup " + backup.publicId() + ". Source instance: " + backup.instanceCode() + ".");
        PlatformBusinessClient saved = clientRepository.save(clone);
        Set<String> modules = instanceManagementService.assignedModuleKeys(source.getId());
        instanceManagementService.updateModules(saved.getId(), new ArrayList<>(modules), actor);
        return saved;
    }

    private void createSteps(long jobId) {
        int sequence = 10;
        for (String[] step : List.of(
                new String[]{"VALIDATE_BACKUP", "Validate encrypted backup and destination"},
                new String[]{"DECRYPT_PACKAGE", "Decrypt and verify recovery package"},
                new String[]{"CREATE_DATABASE", "Create isolated database"},
                new String[]{"IMPORT_DATABASE", "Import restored database"},
                new String[]{"RESTORE_FILES", "Restore instance files"},
                new String[]{"GENERATE_RUNTIME", "Generate clone runtime"},
                new String[]{"REGISTER_INSTANCE", "Register clone in Matrix26"},
                new String[]{"START_RUNTIME", "Start clone runtime"},
                new String[]{"HEALTH_CHECK", "Verify clone health"}
        )) {
            restoreRepository.insertStep(jobId, step[0], sequence, step[1]);
            sequence += 10;
        }
    }

    private Path backupRoot() {
        String configured = firstNonBlank(System.getenv("MATRIX26_BACKUP_ROOT"), backupProperties.getRootDirectory());
        return (configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "Matrix26", "backups") : Path.of(configured))
                .toAbsolutePath().normalize();
    }

    private Path projectRoot() { return Path.of("").toAbsolutePath().normalize(); }

    private void gunzip(Path source, Path target) throws IOException {
        try (InputStream input = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(source)));
             OutputStream output = new BufferedOutputStream(Files.newOutputStream(target))) {
            input.transferTo(output);
        }
    }

    private String replaceDatabase(String jdbcUrl, String database) {
        Matcher matcher = JDBC_MYSQL.matcher(jdbcUrl);
        if (!matcher.matches()) throw new Matrix26RestoreException("Source runtime JDBC URL is not supported.");
        int query = jdbcUrl.indexOf('?');
        String suffix = query >= 0 ? jdbcUrl.substring(query) : "";
        String port = matcher.group(2) == null ? "" : ":" + matcher.group(2);
        return "jdbc:mysql://" + matcher.group(1) + port + "/" + database + suffix;
    }

    private String runtimeScript(Matrix26RestoreJob job) {
        return "#!/usr/bin/env bash\nset -euo pipefail\n\n"
                + "ROOT_DIR=\"$(cd \"$(dirname \"${BASH_SOURCE[0]}\")/../..\" && pwd)\"\n"
                + "CONFIG_FILE=\"$ROOT_DIR/runtime-clients/" + job.targetRuntimeProfile() + "/application.properties\"\n\n"
                + "if [[ ! -f \"$CONFIG_FILE\" ]]; then echo \"Runtime configuration was not found: $CONFIG_FILE\" >&2; exit 1; fi\n"
                + "if command -v cygpath >/dev/null 2>&1; then CONFIG_PATH=\"$(cygpath -m \"$CONFIG_FILE\")\"; else CONFIG_PATH=\"$CONFIG_FILE\"; fi\n"
                + "cd \"$ROOT_DIR\"\n"
                + "JAR=\"$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*sources*' | head -n 1)\"\n"
                + "if [[ -z \"$JAR\" ]]; then echo \"No application JAR found. Run: mvn clean -DskipTests package\" >&2; exit 1; fi\n"
                + "exec java -jar \"$JAR\" --spring.config.additional-location=\"file:${CONFIG_PATH}\"\n";
    }

    private void writeAudit(PlatformBusinessClient instance, String actor, String action, String summary) {
        Matrix26InstanceAuditLog log = new Matrix26InstanceAuditLog();
        log.setInstance(instance);
        log.setActorUsername(safeActor(actor));
        log.setAction(action);
        log.setSummary(limit(summary, 500));
        log.setAfterSnapshot("{\"instanceCode\":\"" + escapeJson(instance.getCode()) + "\"}");
        auditRepository.save(log);
    }

    private String sanitizedFile(Path path) {
        try {
            if (!Files.isRegularFile(path)) return "No diagnostic output was produced.";
            String value = Files.readString(path, StandardCharsets.UTF_8)
                    .replaceAll("(?i)(password|secret|token|api[-_.]?key)[=:]\\s*\\S+", "$1=***REDACTED***");
            return limit(value.trim(), 2000);
        } catch (IOException ex) {
            return "Diagnostic output could not be read.";
        }
    }

    private String safeIdentifier(String value) {
        String clean = value == null ? "" : value.trim();
        if (!SAFE_IDENTIFIER.matcher(clean).matches()) throw new Matrix26RestoreException("Unsafe technical identifier: " + value);
        return clean;
    }

    private boolean safeZipEntry(String name) {
        return name != null && !name.isBlank() && !name.startsWith("/") && !name.startsWith("\\")
                && !name.contains("../") && !name.matches("^[A-Za-z]:.*");
    }

    private void ensureInside(Path root, Path value) {
        if (!value.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize())) {
            throw new Matrix26RestoreException("Path escaped the allowed restore directory.");
        }
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void deleteTreeQuietly(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException ignored) { }
    }

    private String safeActor(String actor) { return actor == null || actor.isBlank() ? "matrix26-admin" : limit(actor.trim(), 120); }
    private String safeMessage(Throwable ex) { return limit(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 4000); }
    private String limit(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }
    private String firstNonBlank(String first, String second) { return first != null && !first.isBlank() ? first : second; }
    private String escapeJson(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private record DatabaseTarget(String host, int port) { }
    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
}
