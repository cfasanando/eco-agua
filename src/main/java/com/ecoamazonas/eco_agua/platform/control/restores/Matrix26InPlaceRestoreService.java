package com.ecoamazonas.eco_agua.platform.control.restores;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLog;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLogRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceHealthService;
import com.ecoamazonas.eco_agua.platform.control.Matrix26TargetDatabaseService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupEncryption;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupExtraction;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupJob;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupProperties;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupRetentionClass;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupSecurityService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupVerificationState;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
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
public class Matrix26InPlaceRestoreService {

    private static final Pattern JDBC_MYSQL = Pattern.compile("^jdbc:mysql://([^/:?]+)(?::(\\d+))?/([^?]+)(?:\\?.*)?$");
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Matrix26InPlaceRestoreRepository repository;
    private final Matrix26RestoreProperties restoreProperties;
    private final Matrix26BackupProperties backupProperties;
    private final Matrix26BackupRepository backupRepository;
    private final Matrix26BackupService backupService;
    private final Matrix26BackupSecurityService backupSecurityService;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26TargetDatabaseService targetDatabaseService;
    private final Matrix26RuntimeControlService runtimeControlService;
    private final Matrix26InstanceHealthService healthService;
    private final Matrix26InstanceAuditLogRepository auditRepository;
    private final JdbcTemplate jdbcTemplate;
    private final String controlJdbcUrl;
    private final String databaseUsername;
    private final String databasePassword;

    public Matrix26InPlaceRestoreService(
            Matrix26InPlaceRestoreRepository repository,
            Matrix26RestoreProperties restoreProperties,
            Matrix26BackupProperties backupProperties,
            Matrix26BackupRepository backupRepository,
            Matrix26BackupService backupService,
            Matrix26BackupSecurityService backupSecurityService,
            PlatformBusinessClientRepository clientRepository,
            Matrix26TargetDatabaseService targetDatabaseService,
            Matrix26RuntimeControlService runtimeControlService,
            Matrix26InstanceHealthService healthService,
            Matrix26InstanceAuditLogRepository auditRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${spring.datasource.url}") String controlJdbcUrl,
            @Value("${spring.datasource.username}") String databaseUsername,
            @Value("${spring.datasource.password:}") String databasePassword
    ) {
        this.repository = repository;
        this.restoreProperties = restoreProperties;
        this.backupProperties = backupProperties;
        this.backupRepository = backupRepository;
        this.backupService = backupService;
        this.backupSecurityService = backupSecurityService;
        this.clientRepository = clientRepository;
        this.targetDatabaseService = targetDatabaseService;
        this.runtimeControlService = runtimeControlService;
        this.healthService = healthService;
        this.auditRepository = auditRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.controlJdbcUrl = controlJdbcUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
    }

    public List<Matrix26InPlaceRestoreCandidate> candidates() {
        List<Matrix26InPlaceRestoreCandidate> result = new ArrayList<>();
        for (Matrix26BackupJob backup : backupRepository.findRecent()) {
            if (!allowedCode(backup.instanceCode())) continue;
            Matrix26BackupEncryption encryption = backupSecurityService.metadata(backup.id());
            PlatformBusinessClient instance = clientRepository.findById(backup.instanceId()).orElse(null);
            boolean full = "MANUAL_FULL".equalsIgnoreCase(backup.backupType())
                    || "SCHEDULED_FULL".equalsIgnoreCase(backup.backupType());
            boolean verified = encryption != null && encryption.encrypted()
                    && encryption.verificationStatus() == Matrix26BackupVerificationState.VERIFIED;
            boolean identity = instance != null
                    && backup.instanceCode().equalsIgnoreCase(instance.getCode())
                    && backup.databaseName().equalsIgnoreCase(instance.getDatabaseName());
            boolean eligible = restoreProperties.isInPlaceEnabled() && backup.isCompleted() && full && verified
                    && identity && instance != null && !instance.isProtectedInstance();
            String reason = eligible ? "Ready for staged in-place restoration."
                    : !restoreProperties.isInPlaceEnabled() ? "In-place restoration is disabled."
                    : instance == null ? "Source instance registration is missing."
                    : instance.isProtectedInstance() ? "Protected instances cannot be restored in place."
                    : !full ? "Only full instance backups are eligible."
                    : !verified ? "Backup must be encrypted and verified."
                    : !identity ? "Backup identity no longer matches the registered source instance."
                    : "Backup is not completed.";
            result.add(new Matrix26InPlaceRestoreCandidate(backup, encryption, eligible, reason));
        }
        return result;
    }

    public Matrix26InPlaceRestoreSummary summary() { return repository.summary(); }
    public List<Matrix26InPlaceRestoreJob> recentJobs() { return repository.findRecent(); }
    public Matrix26InPlaceRestoreJob job(long id) { return repository.findById(id).orElseThrow(() -> new Matrix26RestoreException("In-place restore job not found.")); }
    public List<Matrix26InPlaceRestoreStep> steps(long id) { return repository.findSteps(id); }
    public List<Matrix26InPlaceRestoreCheck> checks(long id) { return repository.findChecks(id); }
    public Matrix26RestoreProperties properties() { return restoreProperties; }

    public synchronized Matrix26InPlaceRestoreJob prepare(long backupJobId, String confirmation, String actor) {
        if (!restoreProperties.isInPlaceEnabled()) throw new Matrix26RestoreException("In-place restoration is disabled.");
        if (repository.hasActiveJob()) throw new Matrix26RestoreException("Another in-place restore job is active.");
        Matrix26InPlaceRestoreCandidate candidate = candidate(backupJobId);
        if (!candidate.eligible()) throw new Matrix26RestoreException(candidate.reason());
        String expected = "RESTORE IN PLACE " + candidate.backup().instanceCode();
        requireConfirmation(expected, confirmation);

        PlatformBusinessClient source = source(candidate.backup());
        String publicId = "RIP-" + LocalDateTime.now().format(ID_TIME) + "-"
                + Long.toHexString(System.nanoTime()).toUpperCase(Locale.ROOT);
        String token = publicId.replaceAll("[^A-Za-z0-9]", "");
        token = token.substring(Math.max(0, token.length() - 12)).toLowerCase(Locale.ROOT);
        String stageDb = safeIdentifier(source.getDatabaseName() + "_stage_" + token);
        String rollbackDb = safeIdentifier(source.getDatabaseName() + "_rollback_" + token);
        Path work = backupRoot().resolve(".matrix26-inplace-temp").resolve(publicId).normalize();
        Path stageData = projectRoot().resolve(restoreProperties.getRuntimeDataDirectory())
                .resolve(restoreProperties.getInPlaceStageDirectory()).resolve(source.getCode()).resolve(publicId).resolve("content").normalize();
        Path rollbackData = projectRoot().resolve(restoreProperties.getRuntimeDataDirectory())
                .resolve(restoreProperties.getInPlaceRollbackDirectory()).resolve(source.getCode()).resolve(publicId).resolve("content").normalize();

        Matrix26InPlaceRestoreJob draft = new Matrix26InPlaceRestoreJob(
                null, publicId, candidate.backup().id(), candidate.backup().publicId(), source.getId(), source.getCode(),
                source.getBusinessName(), source.getDatabaseName(), stageDb, rollbackDb,
                source.getRuntimeProfile(), source.getRuntimePort(), source.getPublicUrl(), null, null,
                Matrix26InPlaceRestoreStatus.DRAFT, safeActor(actor), LocalDateTime.now(), null, null, null,
                null, null, work.toString(), stageData.toString(), rollbackData.toString(), null
        );
        long jobId = repository.insertJob(draft);
        createSteps(jobId);
        executePreparation(jobId, candidate, source, actor);
        return job(jobId);
    }

    private void executePreparation(long jobId, Matrix26InPlaceRestoreCandidate candidate, PlatformBusinessClient source, String actor) {
        Matrix26InPlaceRestoreJob job = job(jobId);
        String currentStep = "PRECHECK";
        try {
            repository.markStarted(jobId);
            Files.createDirectories(Path.of(job.workDirectory()));
            runStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.PRECHECKING,
                    "Validating source, backup, tools, storage, and staging boundaries.", () -> precheck(job, source, candidate));

            currentStep = "SAFETY_BACKUP";
            repository.startStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.SAFETY_BACKUP_RUNNING,
                    "Creating a current encrypted safety backup before any switch.");
            Matrix26BackupJob safetyPlain = backupService.createManualFullBackup(source.getId(), actor, true);
            Matrix26BackupJob safety = backupSecurityService.encryptBackup(safetyPlain.id(), Matrix26BackupRetentionClass.FINAL, actor);
            Matrix26BackupEncryption safetyEncryption = backupSecurityService.verifyEncryptedBackup(safety.id(), actor);
            if (safetyEncryption.verificationStatus() != Matrix26BackupVerificationState.VERIFIED) {
                throw new Matrix26RestoreException("The safety backup did not remain verified.");
            }
            repository.attachSafetyBackup(jobId, safety.id(), safety.publicId());
            repository.completeStep(jobId, currentStep, "Protected safety backup created and verified: " + safety.publicId());
            repository.updateStatus(jobId, Matrix26InPlaceRestoreStatus.SAFETY_BACKUP_VERIFIED);
            repository.addCheck(jobId, "SAFETY_BACKUP", "Current safety backup", "MATCH", "VERIFIED", "VERIFIED", safety.publicId());

            currentStep = "EXTRACT_RECOVERY";
            repository.startStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.PREPARING_STAGING_DATABASE,
                    "Decrypting and verifying the selected recovery package.");
            Path extractionRoot = Path.of(job.workDirectory()).resolve("recovery");
            Matrix26BackupExtraction extraction = backupSecurityService.extractVerifiedBackup(candidate.backup().id(), extractionRoot);
            repository.completeStep(jobId, currentStep, extraction.verificationMessage());
            repository.addCheck(jobId, "RECOVERY_PACKAGE", "Encrypted recovery package", "MATCH", "VERIFIED", "VERIFIED", extraction.verificationMessage());

            currentStep = "STAGE_DATABASE";
            repository.startStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.IMPORTING_STAGING_DATABASE,
                    "Importing the recovery SQL into an isolated staging database.");
            targetDatabaseService.createDatabase(job.stageDatabaseName(), false);
            Path payload = extraction.extractedDirectory();
            Path sql = Path.of(job.workDirectory()).resolve("database.sql");
            gunzip(payload.resolve("database.sql.gz"), sql);
            importDatabase(job.stageDatabaseName(), sql, Path.of(job.workDirectory()).resolve("stage-import.stderr.log"));
            Files.deleteIfExists(sql);
            repository.completeStep(jobId, currentStep, targetDatabaseService.tableCount(job.stageDatabaseName()) + " staging tables imported.");

            currentStep = "VERIFY_STAGE_DATABASE";
            runStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.VERIFYING_STAGING_DATABASE,
                    "Comparing staging table names and row counts with the selected SQL dump.",
                    () -> verifyStageDatabase(job, payload.resolve("database.sql.gz")));

            currentStep = "STAGE_FILES";
            repository.startStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.PREPARING_STAGING_FILES,
                    "Extracting source-owned files into an isolated staging directory.");
            int restored = prepareStageFiles(payload.resolve("instance-files.zip"), source.getCode(), Path.of(job.stageDataDirectory()), job.publicId());
            repository.completeStep(jobId, currentStep, restored + " verified files prepared for switching.");
            repository.addCheck(jobId, "STAGE_FILES", "Staging files", "MATCH", "verified archive entries", restored + " files", "Every extracted file matched its streamed SHA-256 value.");

            repository.updateStatus(jobId, Matrix26InPlaceRestoreStatus.READY_TO_SWITCH);
            repository.addEvent(jobId, "PREPARATION_COMPLETED", "READY_TO_SWITCH", safeActor(actor),
                    "Safety backup, staging database, and staging resources are ready.");
            writeAudit(source, actor, "INPLACE_RESTORE_PREPARED", "In-place restore " + job.publicId() + " is ready to switch.");
        } catch (Exception ex) {
            String error = safeMessage(ex);
            repository.failStep(jobId, currentStep, error);
            repository.fail(jobId, Matrix26InPlaceRestoreStatus.FAILED, error);
            repository.addEvent(jobId, "PREPARATION_FAILED", "FAILED", safeActor(actor), error);
            writeAudit(source, actor, "INPLACE_RESTORE_PREPARATION_FAILED", "In-place restore " + job.publicId() + " failed before switching: " + error);
            throw ex instanceof Matrix26RestoreException re ? re : new Matrix26RestoreException("In-place preparation failed: " + error, ex);
        }
    }

    public synchronized Matrix26InPlaceRestoreJob switchInstance(long jobId, String confirmation, String actor) {
        Matrix26InPlaceRestoreJob job = job(jobId);
        if (!job.canSwitch()) throw new Matrix26RestoreException("This job is not ready to switch.");
        requireConfirmation("SWITCH INSTANCE " + job.sourceInstanceCode(), confirmation);
        PlatformBusinessClient source = source(job.sourceInstanceId(), job.sourceInstanceCode());
        boolean databaseSwitched = false;
        boolean filesSwitched = false;
        String currentStep = "STOP_RUNTIME";
        try {
            validatePreparedState(job, source);
            repository.startStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.STOPPING_RUNTIME,
                    "Stopping the source runtime through Runtime Control.");
            var stop = runtimeControlService.stop(String.valueOf(source.getId()), actor, "STOP " + source.getCode());
            repository.completeStep(jobId, currentStep, stop.message());

            currentStep = "SWITCH_DATABASE";
            repository.startStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.SWITCHING_DATABASE,
                    "Atomically moving active tables to rollback and staging tables to the active schema.");
            switchDatabase(job.sourceDatabaseName(), job.stageDatabaseName(), job.rollbackDatabaseName());
            databaseSwitched = true;
            repository.markSwitchMutation(jobId);
            repository.completeStep(jobId, currentStep, "Database table sets switched without dropping the original database.");

            currentStep = "SWITCH_FILES";
            repository.startStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.SWITCHING_FILES,
                    "Moving current resources to rollback storage and staging resources into the live namespace.");
            switchFiles(job);
            filesSwitched = true;
            repository.completeStep(jobId, currentStep, "Runtime data switched with the previous resources retained for rollback.");

            currentStep = "START_RUNTIME";
            repository.startStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.STARTING_RUNTIME,
                    "Starting the restored instance through Runtime Control.");
            var start = runtimeControlService.start(String.valueOf(source.getId()), actor);
            repository.completeStep(jobId, currentStep, start.message());

            currentStep = "HEALTH_CHECK";
            repository.startStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.HEALTH_CHECKING,
                    "Checking restored portal availability.");
            var health = healthService.refreshInstance(source.getId());
            if (!health.online()) throw new Matrix26RestoreException("Restored runtime remained offline: " + health.message());
            repository.completeStep(jobId, currentStep, health.message());
            repository.addCheck(jobId, "HTTP_HEALTH", "Restored HTTP health", "MATCH", "ONLINE", "ONLINE", health.message());

            LocalDateTime expires = LocalDateTime.now().plusHours(Math.max(1, restoreProperties.getInPlaceRollbackRetentionHours()));
            repository.markSwitched(jobId, expires);
            repository.addEvent(jobId, "SWITCH_COMPLETED", "AWAITING_CONFIRMATION", safeActor(actor),
                    "The restored version is online. Rollback resources are retained until " + expires + ".");
            writeAudit(source, actor, "INPLACE_RESTORE_SWITCHED", "In-place restore " + job.publicId() + " switched and awaits confirmation.");
            return job(jobId);
        } catch (Exception ex) {
            String error = safeMessage(ex);
            repository.failStep(jobId, currentStep, error);
            if (databaseSwitched || filesSwitched) {
                repository.fail(jobId, Matrix26InPlaceRestoreStatus.MANUAL_RECOVERY_REQUIRED, error);
                repository.addEvent(jobId, "SWITCH_FAILED_AFTER_MUTATION", "MANUAL_RECOVERY_REQUIRED", safeActor(actor), error);
            } else {
                repository.fail(jobId, Matrix26InPlaceRestoreStatus.FAILED, error);
            }
            throw ex instanceof Matrix26RestoreException re ? re : new Matrix26RestoreException("In-place switch failed: " + error, ex);
        }
    }

    public synchronized Matrix26InPlaceRestoreJob confirm(long jobId, String confirmation, String actor) {
        Matrix26InPlaceRestoreJob job = job(jobId);
        if (!job.canConfirm()) throw new Matrix26RestoreException("This restored instance is not awaiting confirmation.");
        requireConfirmation("CONFIRM RESTORE " + job.sourceInstanceCode(), confirmation);
        PlatformBusinessClient source = source(job.sourceInstanceId(), job.sourceInstanceCode());
        var health = healthService.refreshInstance(source.getId());
        if (!health.online()) throw new Matrix26RestoreException("The instance must be online before confirmation: " + health.message());
        repository.confirm(jobId);
        repository.completeStep(jobId, "CONFIRM", "Operator accepted the restored instance. Rollback resources remain retained.");
        repository.addEvent(jobId, "RESTORE_CONFIRMED", "COMPLETED", safeActor(actor),
                "Rollback retained until " + job.rollbackExpiresAt() + ".");
        writeAudit(source, actor, "INPLACE_RESTORE_CONFIRMED", "In-place restore " + job.publicId() + " confirmed.");
        return job(jobId);
    }

    public synchronized Matrix26InPlaceRestoreJob rollback(long jobId, String confirmation, String actor) {
        Matrix26InPlaceRestoreJob job = job(jobId);
        if (!job.canRollback()) throw new Matrix26RestoreException("This job does not have an available rollback transition.");
        requireConfirmation("ROLLBACK INSTANCE " + job.sourceInstanceCode(), confirmation);
        if (job.rollbackExpiresAt() != null && LocalDateTime.now().isAfter(job.rollbackExpiresAt())) {
            throw new Matrix26RestoreException("The configured rollback retention window has expired. No automatic deletion was performed.");
        }
        PlatformBusinessClient source = source(job.sourceInstanceId(), job.sourceInstanceCode());
        String currentStep = "ROLLBACK";
        try {
            repository.startStep(jobId, currentStep, Matrix26InPlaceRestoreStatus.ROLLBACK_RUNNING,
                    "Stopping the restored runtime and returning the preserved database and resources.");
            runtimeControlService.stop(String.valueOf(source.getId()), actor, "STOP " + source.getCode());
            boolean resourcesRolledBack = rollbackFilesIfNeeded(job);
            boolean databaseRolledBack = rollbackDatabaseIfNeeded(job);
            if (!resourcesRolledBack && !databaseRolledBack) {
                throw new Matrix26RestoreException("No switched database or resource rollback state was found.");
            }
            runtimeControlService.start(String.valueOf(source.getId()), actor);
            var health = healthService.refreshInstance(source.getId());
            if (!health.online()) throw new Matrix26RestoreException("Rollback runtime remained offline: " + health.message());
            repository.completeStep(jobId, currentStep, "Previous database and resources restored; HTTP health passed.");
            repository.rolledBack(jobId);
            repository.addEvent(jobId, "ROLLBACK_COMPLETED", "ROLLED_BACK", safeActor(actor), health.message());
            writeAudit(source, actor, "INPLACE_RESTORE_ROLLED_BACK", "In-place restore " + job.publicId() + " rolled back.");
            return job(jobId);
        } catch (Exception ex) {
            String error = safeMessage(ex);
            repository.failStep(jobId, currentStep, error);
            repository.fail(jobId, Matrix26InPlaceRestoreStatus.MANUAL_RECOVERY_REQUIRED, error);
            repository.addEvent(jobId, "ROLLBACK_FAILED", "MANUAL_RECOVERY_REQUIRED", safeActor(actor), error);
            throw ex instanceof Matrix26RestoreException re ? re : new Matrix26RestoreException("Rollback failed: " + error, ex);
        }
    }

    private void precheck(Matrix26InPlaceRestoreJob job, PlatformBusinessClient source, Matrix26InPlaceRestoreCandidate candidate) throws IOException {
        if (source.isProtectedInstance()) throw new Matrix26RestoreException("Protected instances cannot be restored in place.");
        if (!allowedCode(source.getCode())) throw new Matrix26RestoreException("Source instance is outside the in-place allowlist.");
        if (!candidate.backup().databaseName().equalsIgnoreCase(source.getDatabaseName())) throw new Matrix26RestoreException("Backup database ownership does not match the live instance.");
        if (backupRepository.hasActiveJob(source.getId())) throw new Matrix26RestoreException("Another backup is active for this instance.");
        if (targetDatabaseService.databaseExists(job.stageDatabaseName()) || targetDatabaseService.databaseExists(job.rollbackDatabaseName())) {
            throw new Matrix26RestoreException("A staging or rollback database already exists for this job.");
        }
        Path currentData = currentData(source.getCode());
        Path stageData = Path.of(job.stageDataDirectory()).toAbsolutePath().normalize();
        Path rollbackData = Path.of(job.rollbackDataDirectory()).toAbsolutePath().normalize();
        ensureInside(projectRoot(), currentData);
        ensureInside(projectRoot(), stageData);
        ensureInside(projectRoot(), rollbackData);
        if (Files.exists(stageData) || Files.exists(rollbackData)) throw new Matrix26RestoreException("Staging or rollback resource directory already exists.");
        long usable = Files.getFileStore(projectRoot()).getUsableSpace();
        long required = Math.max(backupProperties.getMinimumFreeBytes(), Math.max(1L, candidate.encryption().packageSizeBytes()) * 3L);
        if (usable < required) throw new Matrix26RestoreException("Insufficient free disk space for staging and rollback.");
        validateDatabaseObjects(source.getDatabaseName());
        repository.addCheck(job.id(), "SOURCE_IDENTITY", "Source identity", "MATCH", source.getCode() + "/" + source.getDatabaseName(), candidate.backup().instanceCode() + "/" + candidate.backup().databaseName(), "Backup and live registration match.");
        repository.addCheck(job.id(), "STORAGE", "Available storage", "MATCH", Long.toString(required), Long.toString(usable), "Free space is sufficient for staging and rollback.");
    }

    private void validatePreparedState(Matrix26InPlaceRestoreJob job, PlatformBusinessClient source) {
        if (source.isProtectedInstance()) throw new Matrix26RestoreException("The source instance became protected after preparation.");
        if (!targetDatabaseService.databaseExists(job.stageDatabaseName()) || targetDatabaseService.tableCount(job.stageDatabaseName()) <= 0) {
            throw new Matrix26RestoreException("The staging database is missing or empty.");
        }
        if (targetDatabaseService.databaseExists(job.rollbackDatabaseName())) {
            throw new Matrix26RestoreException("The rollback database already exists. Generate a new job or review the previous attempt.");
        }
        if (!Files.isDirectory(Path.of(job.stageDataDirectory()))) throw new Matrix26RestoreException("The staging resource directory is missing.");
        if (Files.exists(Path.of(job.rollbackDataDirectory()))) throw new Matrix26RestoreException("The rollback resource directory already exists.");
        Matrix26BackupEncryption safety = job.safetyBackupJobId() == null ? null : backupSecurityService.metadata(job.safetyBackupJobId());
        if (safety == null || safety.verificationStatus() != Matrix26BackupVerificationState.VERIFIED || !safety.protectedFlag()) {
            throw new Matrix26RestoreException("The protected safety backup is unavailable or no longer verified.");
        }
        validateDatabaseObjects(job.sourceDatabaseName());
        validateDatabaseObjects(job.stageDatabaseName());
    }

    private void verifyStageDatabase(Matrix26InPlaceRestoreJob job, Path gzipDump) throws IOException {
        Matrix26RestoreDumpSnapshot.Snapshot snapshot = Matrix26RestoreDumpSnapshot.read(gzipDump);
        Set<String> expectedTables = new LinkedHashSet<>(snapshot.createStatements().keySet());
        Set<String> actualTables = new LinkedHashSet<>(tables(job.stageDatabaseName()));
        if (!expectedTables.equals(actualTables)) {
            repository.addCheck(job.id(), "STAGE_TABLE_SET", "Staging table set", "MISMATCH", expectedTables.toString(), actualTables.toString(), "Staging tables differ from the recovery dump.");
            throw new Matrix26RestoreException("Staging table set does not match the recovery dump.");
        }
        JdbcTemplate target = targetDatabaseService.targetJdbcTemplate(job.stageDatabaseName());
        List<String> mismatches = new ArrayList<>();
        for (String table : expectedTables) {
            Long actual = target.queryForObject("SELECT COUNT(*) FROM `" + quote(table) + "`", Long.class);
            long expected = snapshot.rowCounts().getOrDefault(table, 0L);
            if (actual == null || actual != expected) mismatches.add(table + ": expected " + expected + ", got " + actual);
        }
        if (!mismatches.isEmpty()) {
            repository.addCheck(job.id(), "STAGE_ROW_COUNTS", "Staging row counts", "MISMATCH", "dump counts", String.join("; ", mismatches), "At least one table count differs.");
            throw new Matrix26RestoreException("Staging row counts differ: " + String.join("; ", mismatches));
        }
        repository.addCheck(job.id(), "STAGE_TABLE_SET", "Staging table set", "MATCH", Integer.toString(expectedTables.size()), Integer.toString(actualTables.size()), "All tables from the dump are present.");
        repository.addCheck(job.id(), "STAGE_ROW_COUNTS", "Staging row counts", "MATCH", "exact", "exact", "All restored table counts match the dump snapshot.");
    }

    private int prepareStageFiles(Path archive, String sourceCode, Path stageData, String jobPublicId) throws IOException {
        if (Files.exists(stageData)) throw new Matrix26RestoreException("Staging resource directory already exists.");
        Files.createDirectories(stageData);
        String prefix = "runtime-data/" + sourceCode + "/";
        int restored = 0;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (!name.startsWith(prefix) || name.equals(prefix)) continue;
                String relative = name.substring(prefix.length());
                if (!safeZipEntry(relative)) throw new Matrix26RestoreException("Unsafe archive entry: " + name);
                Path target = stageData.resolve(relative).normalize();
                ensureInside(stageData, target);
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Files.createDirectories(target.getParent());
                try (var input = new BufferedInputStream(zip.getInputStream(entry));
                     var output = new BufferedOutputStream(Files.newOutputStream(target))) {
                    input.transferTo(output);
                }
                restored++;
            }
        }
        if (restored <= 0) throw new Matrix26RestoreException("No source-owned runtime-data files were found in the backup archive.");
        Properties marker = new Properties();
        marker.setProperty("matrix26.inplace.restore.job", jobPublicId);
        marker.setProperty("matrix26.inplace.restore.source", sourceCode);
        try (OutputStream output = Files.newOutputStream(stageData.resolve(".matrix26-inplace-restore.properties"))) {
            marker.store(output, "Matrix26 in-place restore ownership marker");
        }
        return restored;
    }

    private void switchDatabase(String active, String stage, String rollback) {
        if (targetDatabaseService.databaseExists(rollback)) throw new Matrix26RestoreException("Rollback database already exists.");
        targetDatabaseService.createDatabase(rollback, false);
        List<String> activeTables = tables(active);
        List<String> stageTables = tables(stage);
        if (activeTables.isEmpty() || stageTables.isEmpty()) throw new Matrix26RestoreException("Active or staging database does not contain base tables.");
        List<String> clauses = new ArrayList<>();
        for (String table : activeTables) clauses.add(qualified(active, table) + " TO " + qualified(rollback, table));
        for (String table : stageTables) clauses.add(qualified(stage, table) + " TO " + qualified(active, table));
        executeRename(clauses);
        if (targetDatabaseService.tableCount(active) != stageTables.size() || targetDatabaseService.tableCount(rollback) != activeTables.size()) {
            throw new Matrix26RestoreException("Database switch completed with an unexpected table count.");
        }
    }


    private boolean rollbackDatabaseIfNeeded(Matrix26InPlaceRestoreJob job) {
        if (!targetDatabaseService.databaseExists(job.rollbackDatabaseName())
                || targetDatabaseService.tableCount(job.rollbackDatabaseName()) <= 0) {
            return false;
        }
        rollbackDatabase(job.sourceDatabaseName(), job.stageDatabaseName(), job.rollbackDatabaseName());
        return true;
    }

    private boolean rollbackFilesIfNeeded(Matrix26InPlaceRestoreJob job) throws IOException {
        Path rollback = Path.of(job.rollbackDataDirectory()).toAbsolutePath().normalize();
        if (!Files.isDirectory(rollback)) {
            return false;
        }
        rollbackFiles(job);
        return true;
    }

    private void rollbackDatabase(String active, String stage, String rollback) {
        if (!targetDatabaseService.databaseExists(rollback) || targetDatabaseService.tableCount(rollback) <= 0) {
            throw new Matrix26RestoreException("Rollback database is missing or empty.");
        }
        if (targetDatabaseService.tableCount(stage) > 0) throw new Matrix26RestoreException("Staging database is not empty and cannot receive the restored version.");
        List<String> activeTables = tables(active);
        List<String> rollbackTables = tables(rollback);
        List<String> clauses = new ArrayList<>();
        for (String table : activeTables) clauses.add(qualified(active, table) + " TO " + qualified(stage, table));
        for (String table : rollbackTables) clauses.add(qualified(rollback, table) + " TO " + qualified(active, table));
        executeRename(clauses);
    }

    private void executeRename(List<String> clauses) {
        if (clauses.isEmpty()) throw new Matrix26RestoreException("No tables are available for switching.");
        String renameSql = "RENAME TABLE " + String.join(", ", clauses);
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                try {
                    statement.execute(renameSql);
                } finally {
                    statement.execute("SET FOREIGN_KEY_CHECKS=1");
                }
            }
            return null;
        });
    }

    private void switchFiles(Matrix26InPlaceRestoreJob job) throws IOException {
        Path current = currentData(job.sourceInstanceCode());
        Path stage = Path.of(job.stageDataDirectory()).toAbsolutePath().normalize();
        Path rollback = Path.of(job.rollbackDataDirectory()).toAbsolutePath().normalize();
        if (!Files.isDirectory(current) || !Files.isDirectory(stage) || Files.exists(rollback)) {
            throw new Matrix26RestoreException("Current, staging, or rollback resource state is not safe for switching.");
        }
        Files.createDirectories(rollback.getParent());
        move(current, rollback);
        try {
            Files.createDirectories(current.getParent());
            move(stage, current);
        } catch (Exception ex) {
            if (!Files.exists(current) && Files.exists(rollback)) move(rollback, current);
            throw ex;
        }
    }

    private void rollbackFiles(Matrix26InPlaceRestoreJob job) throws IOException {
        Path current = currentData(job.sourceInstanceCode());
        Path restoredStorage = Path.of(job.stageDataDirectory()).toAbsolutePath().normalize();
        Path rollback = Path.of(job.rollbackDataDirectory()).toAbsolutePath().normalize();
        if (!Files.isDirectory(current) || !Files.isDirectory(rollback) || Files.exists(restoredStorage)) {
            throw new Matrix26RestoreException("Resource rollback state is not safe.");
        }
        Files.createDirectories(restoredStorage.getParent());
        move(current, restoredStorage);
        try {
            Files.createDirectories(current.getParent());
            move(rollback, current);
        } catch (Exception ex) {
            if (!Files.exists(current) && Files.exists(restoredStorage)) move(restoredStorage, current);
            throw ex;
        }
    }

    private void validateDatabaseObjects(String database) {
        Integer nonTables = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE <> 'BASE TABLE'", Integer.class, database);
        Integer triggers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = ?", Integer.class, database);
        Integer routines = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = ?", Integer.class, database);
        Integer events = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA = ?", Integer.class, database);
        if (value(nonTables) > 0 || value(triggers) > 0 || value(routines) > 0 || value(events) > 0) {
            throw new Matrix26RestoreException("In-place table switching is blocked because the database contains views, triggers, routines, or events.");
        }
    }

    private List<String> tables(String database) {
        return jdbcTemplate.queryForList("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME", String.class, safeIdentifier(database));
    }

    private void importDatabase(String databaseName, Path sql, Path stderr) throws IOException, InterruptedException {
        DatabaseTarget target = databaseTarget();
        List<String> command = List.of(importExecutable(), "--protocol=TCP", "--host=" + target.host(),
                "--port=" + target.port(), "--user=" + databaseUsername, "--default-character-set=utf8mb4", safeIdentifier(databaseName));
        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectInput(sql.toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(stderr.toFile());
        if (databasePassword != null && !databasePassword.isBlank()) builder.environment().put("MYSQL_PWD", databasePassword);
        Process process = builder.start();
        boolean finished = process.waitFor(Math.max(60, restoreProperties.getProcessTimeoutSeconds()), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new Matrix26RestoreException("Database import exceeded the configured timeout.");
        }
        if (process.exitValue() != 0) throw new Matrix26RestoreException("Database import failed. Review the sanitized restore diagnostic log.");
    }

    private String importExecutable() {
        String configured = restoreProperties.getImportExecutable();
        if (configured == null || configured.isBlank()) throw new Matrix26RestoreException("MariaDB import executable is not configured.");
        Path path = Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) throw new Matrix26RestoreException("MariaDB import executable was not found: " + path);
        return path.toString();
    }

    private DatabaseTarget databaseTarget() {
        Matcher matcher = JDBC_MYSQL.matcher(controlJdbcUrl);
        if (!matcher.matches()) throw new Matrix26RestoreException("Control JDBC URL is not a supported MySQL URL.");
        return new DatabaseTarget(matcher.group(1), matcher.group(2) == null ? 3306 : Integer.parseInt(matcher.group(2)));
    }

    private Matrix26InPlaceRestoreCandidate candidate(long backupJobId) {
        return candidates().stream().filter(item -> item.backup().id().equals(backupJobId)).findFirst()
                .orElseThrow(() -> new Matrix26RestoreException("The selected backup is not available for in-place restoration."));
    }

    private PlatformBusinessClient source(Matrix26BackupJob backup) {
        return source(backup.instanceId(), backup.instanceCode());
    }

    private PlatformBusinessClient source(long id, String code) {
        PlatformBusinessClient source = clientRepository.findById(id)
                .orElseThrow(() -> new Matrix26RestoreException("The source instance registration is missing."));
        if (!source.getCode().equalsIgnoreCase(code)) throw new Matrix26RestoreException("Source instance identity changed after job creation.");
        return source;
    }

    private void createSteps(long jobId) {
        repository.insertStep(jobId, "PRECHECK", 10, "Validate source and staging boundaries");
        repository.insertStep(jobId, "SAFETY_BACKUP", 20, "Create and verify safety backup");
        repository.insertStep(jobId, "EXTRACT_RECOVERY", 30, "Decrypt selected recovery package");
        repository.insertStep(jobId, "STAGE_DATABASE", 40, "Import isolated staging database");
        repository.insertStep(jobId, "VERIFY_STAGE_DATABASE", 50, "Verify staging database");
        repository.insertStep(jobId, "STAGE_FILES", 60, "Prepare staging resources");
        repository.insertStep(jobId, "STOP_RUNTIME", 70, "Stop source runtime");
        repository.insertStep(jobId, "SWITCH_DATABASE", 80, "Switch database table sets");
        repository.insertStep(jobId, "SWITCH_FILES", 90, "Switch runtime resources");
        repository.insertStep(jobId, "START_RUNTIME", 100, "Start restored runtime");
        repository.insertStep(jobId, "HEALTH_CHECK", 110, "Verify restored HTTP health");
        repository.insertStep(jobId, "CONFIRM", 120, "Confirm restored instance");
        repository.insertStep(jobId, "ROLLBACK", 130, "Rollback to preserved instance");
    }

    private void runStep(long jobId, String code, Matrix26InPlaceRestoreStatus status, String detail, CheckedAction action) throws Exception {
        repository.startStep(jobId, code, status, detail);
        action.run();
        repository.completeStep(jobId, code, detail);
    }

    private Path currentData(String sourceCode) {
        Path root = projectRoot().resolve(restoreProperties.getRuntimeDataDirectory()).normalize();
        Path current = root.resolve(sourceCode).normalize();
        ensureInside(root, current);
        return current;
    }

    private Path backupRoot() {
        if (backupProperties.getRootDirectory() == null || backupProperties.getRootDirectory().isBlank()) {
            return projectRoot().resolve("runtime-data/matrix26-backups").normalize();
        }
        return Path.of(backupProperties.getRootDirectory()).toAbsolutePath().normalize();
    }

    private Path projectRoot() { return Path.of("").toAbsolutePath().normalize(); }

    private void gunzip(Path source, Path target) throws IOException {
        try (var input = new GZIPInputStream(Files.newInputStream(source)); var output = Files.newOutputStream(target)) {
            input.transferTo(output);
        }
    }

    private void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }

    private String qualified(String database, String table) { return "`" + quote(database) + "`.`" + quote(table) + "`"; }
    private String quote(String value) { return value.replace("`", "``"); }
    private int value(Integer value) { return value == null ? 0 : value; }

    private String safeIdentifier(String value) {
        if (value == null || value.length() > 64 || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new Matrix26RestoreException("Unsafe database identifier: " + value);
        }
        return value;
    }

    private boolean allowedCode(String code) {
        if (code == null) return false;
        return restoreProperties.getInPlaceAllowedInstanceCodes().stream()
                .filter(value -> value != null)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(code.trim().toLowerCase(Locale.ROOT)::equals);
    }

    private boolean safeZipEntry(String value) {
        return value != null && !value.isBlank() && !value.startsWith("/") && !value.startsWith("\\")
                && !value.matches("^[A-Za-z]:.*") && !value.contains("../") && !value.contains("..\\");
    }

    private void ensureInside(Path root, Path target) {
        Path safeRoot = root.toAbsolutePath().normalize();
        Path safeTarget = target.toAbsolutePath().normalize();
        if (!safeTarget.startsWith(safeRoot)) throw new Matrix26RestoreException("Path escapes the Matrix26 controlled directory.");
    }

    private void requireConfirmation(String expected, String actual) {
        if (actual == null || !expected.equals(actual.trim())) throw new Matrix26RestoreException("Type exactly: " + expected);
    }

    private void writeAudit(PlatformBusinessClient instance, String actor, String action, String summary) {
        Matrix26InstanceAuditLog log = new Matrix26InstanceAuditLog();
        log.setInstance(instance);
        log.setActorUsername(safeActor(actor));
        log.setAction(action);
        log.setSummary(limit(summary, 500));
        log.setAfterSnapshot("{\"instanceCode\":\"" + instance.getCode().replace("\"", "") + "\"}");
        auditRepository.save(log);
    }

    private String safeActor(String actor) { return actor == null || actor.isBlank() ? "matrix26-admin" : limit(actor.trim(), 120); }
    private String safeMessage(Throwable ex) { return limit(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 4000); }
    private String limit(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }

    private record DatabaseTarget(String host, int port) { }
    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
}
