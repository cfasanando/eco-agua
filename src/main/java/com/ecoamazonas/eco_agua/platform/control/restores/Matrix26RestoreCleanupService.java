package com.ecoamazonas.eco_agua.platform.control.restores;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26TargetDatabaseService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupJob;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupProperties;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupRepository;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsInventoryService;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlService;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeInventoryItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26RestoreCleanupService {

    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Set<String> PROTECTED_CODES = Set.of(
            "matrix26-control", "eco-agua", "productos-selva-belen", "restaurante-buen-sabor",
            "matrix26-appearance-lab", "matrix26-restaurant-lab"
    );

    private final Matrix26RestoreRepository restoreRepository;
    private final Matrix26RestoreCleanupRepository cleanupRepository;
    private final Matrix26BackupRepository backupRepository;
    private final Matrix26BackupProperties backupProperties;
    private final Matrix26TargetDatabaseService targetDatabaseService;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26RuntimeControlService runtimeControlService;
    private final Matrix26OperationsInventoryService inventoryService;
    private final Matrix26RestoreProperties properties;
    private final JdbcTemplate jdbcTemplate;

    public Matrix26RestoreCleanupService(
            Matrix26RestoreRepository restoreRepository,
            Matrix26RestoreCleanupRepository cleanupRepository,
            Matrix26BackupRepository backupRepository,
            Matrix26BackupProperties backupProperties,
            Matrix26TargetDatabaseService targetDatabaseService,
            PlatformBusinessClientRepository clientRepository,
            Matrix26RuntimeControlService runtimeControlService,
            Matrix26OperationsInventoryService inventoryService,
            Matrix26RestoreProperties properties,
            JdbcTemplate jdbcTemplate
    ) {
        this.restoreRepository = restoreRepository;
        this.cleanupRepository = cleanupRepository;
        this.backupRepository = backupRepository;
        this.backupProperties = backupProperties;
        this.targetDatabaseService = targetDatabaseService;
        this.clientRepository = clientRepository;
        this.runtimeControlService = runtimeControlService;
        this.inventoryService = inventoryService;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Matrix26RestoreCleanupPlan latest(long restoreJobId) {
        return cleanupRepository.findLatestForJob(restoreJobId).orElse(null);
    }

    public List<Matrix26RestoreCleanupPlanItem> items(Matrix26RestoreCleanupPlan plan) {
        return plan == null ? List.of() : cleanupRepository.findItems(plan.id());
    }

    public Matrix26RestoreCleanupPlan prepare(long restoreJobId, String confirmation, String actor) {
        ensureCleanupEnabled();
        Matrix26RestoreJob job = restoreJob(restoreJobId);
        ensureCleanupCandidate(job);
        String expected = "PREPARE CLEANUP " + job.publicId();
        requireConfirmation(expected, confirmation);
        if (cleanupRepository.hasRunningPlan()) {
            throw new Matrix26RestoreException("Another cleanup plan is currently running.");
        }

        Matrix26RestoreCleanupSnapshot snapshot = snapshot(job);
        String publicId = "CLN-" + LocalDateTime.now().format(ID_TIME) + "-"
                + Long.toHexString(System.nanoTime()).toUpperCase(Locale.ROOT);
        String signature = sign(publicId, job.id(), snapshot.fingerprint());
        Matrix26RestoreCleanupStatus status = snapshot.blocked()
                ? Matrix26RestoreCleanupStatus.BLOCKED
                : Matrix26RestoreCleanupStatus.PREVIEW_READY;
        String summary = snapshot.summary() + (snapshot.blocked()
                ? " Blockers: " + String.join(" ", snapshot.blockers()) : "");

        cleanupRepository.cancelOpenPlans(job.id(), "Superseded by a new cleanup preview.");
        long planId = cleanupRepository.insertPlan(
                publicId, job.id(), status, snapshot.fingerprint(), signature,
                safeActor(actor), summary
        );
        for (Matrix26RestoreCleanupPlanItem item : snapshot.items()) {
            cleanupRepository.insertItem(planId, item);
        }
        cleanupRepository.insertEvent(planId, null, "PLAN_PREPARED", status.name(), safeActor(actor), summary);
        return cleanupRepository.findById(planId)
                .orElseThrow(() -> new Matrix26RestoreException("The cleanup plan could not be reloaded."));
    }

    public Matrix26RestoreCleanupPlan approve(
            long restoreJobId,
            long planId,
            String stopConfirmation,
            String filesConfirmation,
            String databaseConfirmation,
            String registrationConfirmation,
            String actor
    ) {
        ensureCleanupEnabled();
        Matrix26RestoreJob job = restoreJob(restoreJobId);
        Matrix26RestoreCleanupPlan plan = planForJob(planId, job.id());
        if (plan.status() != Matrix26RestoreCleanupStatus.PREVIEW_READY) {
            throw new Matrix26RestoreException("Only a PREVIEW_READY cleanup plan can be approved.");
        }
        verifyPlanSignature(plan);
        Matrix26RestoreCleanupSnapshot current = snapshot(job);
        if (current.blocked()) {
            throw new Matrix26RestoreException("Cleanup approval is blocked: " + String.join(" ", current.blockers()));
        }
        if (!MessageDigest.isEqual(
                plan.snapshotFingerprint().getBytes(StandardCharsets.UTF_8),
                current.fingerprint().getBytes(StandardCharsets.UTF_8))) {
            throw new Matrix26RestoreException("The target changed after preview. Generate a new cleanup plan.");
        }

        requireConfirmation("STOP RUNTIME " + job.targetInstanceCode(), stopConfirmation);
        requireConfirmation("REMOVE FILES " + job.targetInstanceCode(), filesConfirmation);
        requireConfirmation("DROP DATABASE " + job.targetDatabaseName(), databaseConfirmation);
        requireConfirmation("REMOVE REGISTRATION " + job.targetInstanceCode(), registrationConfirmation);

        cleanupRepository.approve(plan.id(), safeActor(actor));
        cleanupRepository.insertEvent(plan.id(), null, "PLAN_APPROVED", "APPROVED", safeActor(actor),
                "Four independent destructive confirmations were validated.");
        return cleanupRepository.findById(plan.id())
                .orElseThrow(() -> new Matrix26RestoreException("The approved cleanup plan could not be reloaded."));
    }

    public synchronized Matrix26RestoreCleanupPlan execute(
            long restoreJobId,
            long planId,
            String confirmation,
            String actor
    ) {
        ensureCleanupEnabled();
        Matrix26RestoreJob job = restoreJob(restoreJobId);
        Matrix26RestoreCleanupPlan plan = planForJob(planId, job.id());
        if (!plan.status().executable()) {
            throw new Matrix26RestoreException("The cleanup plan is not approved for execution.");
        }
        requireConfirmation("EXECUTE CLEANUP " + job.publicId(), confirmation);
        verifyPlanSignature(plan);
        ensureCleanupCandidate(job);
        ensureSourceBackupAvailable(job);

        if (plan.status() == Matrix26RestoreCleanupStatus.APPROVED) {
            Matrix26RestoreCleanupSnapshot current = snapshot(job);
            if (current.blocked()) {
                throw new Matrix26RestoreException("Cleanup execution is blocked: " + String.join(" ", current.blockers()));
            }
            if (!plan.snapshotFingerprint().equals(current.fingerprint())) {
                throw new Matrix26RestoreException("The cleanup target changed after approval. Generate a new plan.");
            }
        }

        cleanupRepository.start(plan.id());
        restoreRepository.markCleanupStarted(job.id());
        cleanupRepository.insertEvent(plan.id(), null, "EXECUTION_STARTED", "RUNNING", safeActor(actor),
                "Cleanup execution started. Completed items will be skipped on retry.");

        List<Matrix26RestoreCleanupPlanItem> items = cleanupRepository.findItems(plan.id());
        for (Matrix26RestoreCleanupPlanItem item : items) {
            if (item.status().finished()) continue;
            if (!item.actionable()) {
                cleanupRepository.completeItem(item.id(), Matrix26RestoreCleanupItemStatus.SKIPPED,
                        "No destructive action was required.");
                continue;
            }
            try {
                cleanupRepository.startItem(item.id());
                String detail = executeItem(job, item, safeActor(actor));
                cleanupRepository.completeItem(item.id(), Matrix26RestoreCleanupItemStatus.COMPLETED, detail);
                cleanupRepository.insertEvent(plan.id(), item.id(), "ITEM_COMPLETED", "COMPLETED", safeActor(actor), detail);
            } catch (RuntimeException ex) {
                String error = safeMessage(ex);
                cleanupRepository.failItem(item.id(), error);
                cleanupRepository.updatePlanStatus(plan.id(), Matrix26RestoreCleanupStatus.PARTIALLY_CLEANED,
                        "Cleanup stopped after a failed step. Retry continues from the first unfinished item.", error);
                restoreRepository.fail(job.id(), Matrix26RestoreStatus.PARTIALLY_CLEANED, error);
                cleanupRepository.insertEvent(plan.id(), item.id(), "ITEM_FAILED", "FAILED", safeActor(actor), error);
                throw new Matrix26RestoreException("Cleanup stopped safely: " + error, ex);
            }
        }

        List<String> residues = residuals(job);
        if (!residues.isEmpty()) {
            String error = "Residual resources remain: " + String.join(", ", residues);
            cleanupRepository.updatePlanStatus(plan.id(), Matrix26RestoreCleanupStatus.PARTIALLY_CLEANED,
                    "Cleanup steps finished but residual verification failed.", error);
            restoreRepository.fail(job.id(), Matrix26RestoreStatus.PARTIALLY_CLEANED, error);
            cleanupRepository.insertEvent(plan.id(), null, "RESIDUAL_CHECK_FAILED", "FAILED", safeActor(actor), error);
            throw new Matrix26RestoreException(error);
        }

        cleanupRepository.updatePlanStatus(plan.id(), Matrix26RestoreCleanupStatus.CLEANED,
                "All restore-owned residual resources were removed. The encrypted source backup was preserved.", null);
        restoreRepository.completeCleanup(job.id());
        cleanupRepository.insertEvent(plan.id(), null, "EXECUTION_COMPLETED", "CLEANED", safeActor(actor),
                "Cleanup completed and residual verification passed. Source backup preserved.");
        inventoryService.invalidateCache();
        return cleanupRepository.findById(plan.id())
                .orElseThrow(() -> new Matrix26RestoreException("The completed cleanup plan could not be reloaded."));
    }

    private String executeItem(Matrix26RestoreJob job, Matrix26RestoreCleanupPlanItem item, String actor) {
        return switch (item.resourceType()) {
            case "RUNTIME_PROCESS" -> stopRuntime(job, actor);
            case "MODULE_ASSIGNMENTS" -> removeModules(job);
            case "INSTANCE_REGISTRATION" -> removeRegistration(job);
            case "RUNTIME_DIRECTORY" -> removeOwnedDirectory(targetRuntimeDirectory(job), job.publicId(), "runtime");
            case "RUNTIME_DATA" -> removeOwnedDirectory(targetDataDirectory(job), job.publicId(), "runtime-data");
            case "DATABASE" -> dropDatabase(job);
            case "TEMPORARY_EXTRACTION" -> removeTemporary(job);
            case "SOURCE_BACKUP" -> "Source backup preserved.";
            default -> throw new Matrix26RestoreException("Unknown cleanup resource type: " + item.resourceType());
        };
    }

    private String stopRuntime(Matrix26RestoreJob job, String actor) {
        PlatformBusinessClient target = matchingRegistration(job).orElse(null);
        if (target == null) {
            if (!portAvailable(job.targetRuntimePort())) {
                throw new Matrix26RestoreException("Port " + job.targetRuntimePort() + " is occupied without an owned target registration.");
            }
            return "No registered runtime process remained.";
        }
        Matrix26RuntimeInventoryItem runtime = runtimeForCode(job.targetInstanceCode()).orElse(null);
        if (runtime == null || (!runtime.portListening() && runtime.processId() == null)) {
            return "The owned runtime was already stopped.";
        }
        if (!runtime.expectedProcess()) {
            throw new Matrix26RestoreException("Port " + job.targetRuntimePort() + " is owned by an unexpected process.");
        }
        runtimeControlService.stop(runtime.target().key(), actor, "STOP " + job.targetInstanceCode());
        inventoryService.invalidateCache();
        Matrix26RuntimeInventoryItem after = runtimeForCode(job.targetInstanceCode()).orElse(null);
        if (after != null && (after.portListening() || after.processId() != null)) {
            throw new Matrix26RestoreException("The clone runtime did not stop cleanly.");
        }
        return "Owned clone runtime stopped and port released.";
    }

    private String removeModules(Matrix26RestoreJob job) {
        PlatformBusinessClient target = matchingRegistration(job).orElse(null);
        if (target == null) return "No target module assignments remained.";
        int removed = jdbcTemplate.update("DELETE FROM platform_client_module WHERE client_id = ?", target.getId());
        return removed + " target module assignments removed.";
    }

    private String removeRegistration(Matrix26RestoreJob job) {
        PlatformBusinessClient target = matchingRegistration(job).orElse(null);
        if (target == null) return "No target registration remained.";
        if (runtimeForCode(job.targetInstanceCode()).map(runtime -> runtime.portListening() || runtime.processId() != null).orElse(false)) {
            throw new Matrix26RestoreException("The runtime must be stopped before removing the registration.");
        }
        long targetId = target.getId();
        int dependentRows = 0;
        List<Map<String, Object>> references = jdbcTemplate.queryForList("""
                SELECT k.TABLE_NAME, k.COLUMN_NAME, c.IS_NULLABLE
                FROM information_schema.KEY_COLUMN_USAGE k
                JOIN information_schema.COLUMNS c
                  ON c.TABLE_SCHEMA = k.TABLE_SCHEMA
                 AND c.TABLE_NAME = k.TABLE_NAME
                 AND c.COLUMN_NAME = k.COLUMN_NAME
                WHERE k.CONSTRAINT_SCHEMA = DATABASE()
                  AND k.REFERENCED_TABLE_SCHEMA = DATABASE()
                  AND k.REFERENCED_TABLE_NAME = 'platform_business_client'
                  AND k.REFERENCED_COLUMN_NAME = 'id'
                ORDER BY k.TABLE_NAME, k.COLUMN_NAME
                """);
        for (Map<String, Object> reference : references) {
            String table = safeIdentifier(String.valueOf(reference.get("TABLE_NAME")));
            String column = safeIdentifier(String.valueOf(reference.get("COLUMN_NAME")));
            if ("platform_business_client".equals(table)) continue;
            boolean nullable = "YES".equalsIgnoreCase(String.valueOf(reference.get("IS_NULLABLE")));
            if (nullable && "matrix26_instance_audit_log".equals(table)) {
                dependentRows += jdbcTemplate.update("UPDATE `" + table + "` SET `" + column + "` = NULL WHERE `" + column + "` = ?", targetId);
            } else {
                dependentRows += jdbcTemplate.update("DELETE FROM `" + table + "` WHERE `" + column + "` = ?", targetId);
            }
        }
        int removed = jdbcTemplate.update("DELETE FROM platform_business_client WHERE id = ? AND code = ?", targetId, job.targetInstanceCode());
        if (removed != 1) throw new Matrix26RestoreException("The target registration changed before deletion.");
        inventoryService.invalidateCache();
        return "Target registration removed after clearing " + dependentRows + " dependent operational rows; audit rows were detached and preserved.";
    }

    private String removeOwnedDirectory(Path directory, String owner, String label) {
        if (!Files.exists(directory)) return "No " + label + " directory remained.";
        ensureInside(label.equals("runtime") ? runtimeRoot() : dataRoot(), directory);
        Path marker = directory.resolve(".matrix26-restore-reference");
        if (!Files.isRegularFile(marker)) {
            throw new Matrix26RestoreException("The " + label + " ownership marker is missing.");
        }
        try {
            if (!owner.equals(Files.readString(marker, StandardCharsets.UTF_8).trim())) {
                throw new Matrix26RestoreException("The " + label + " directory belongs to another restore job.");
            }
            if (containsSymbolicLink(directory)) {
                throw new Matrix26RestoreException("The " + label + " directory contains a symbolic link and requires manual review.");
            }
            deleteTree(directory);
        } catch (IOException ex) {
            throw new Matrix26RestoreException("Could not remove the owned " + label + " directory: " + safeMessage(ex), ex);
        }
        return "Owned " + label + " directory removed.";
    }

    private String dropDatabase(Matrix26RestoreJob job) {
        String database = safeIdentifier(job.targetDatabaseName());
        if (!database.equals(properties.getTargetDatabaseName())
                || database.equalsIgnoreCase(job.sourceDatabaseName())
                || database.equalsIgnoreCase("matrix26_platform_control")) {
            throw new Matrix26RestoreException("Database deletion is outside the isolated restore target boundary.");
        }
        if (!targetDatabaseService.databaseExists(database)) return "No target database remained.";
        jdbcTemplate.execute("DROP DATABASE `" + database + "`");
        if (targetDatabaseService.databaseExists(database)) {
            throw new Matrix26RestoreException("The target database still exists after DROP DATABASE.");
        }
        return "Isolated restore database dropped after all other owned resources were removed.";
    }

    private String removeTemporary(Matrix26RestoreJob job) {
        Path directory = temporaryDirectory(job);
        if (!Files.exists(directory)) return "No temporary extraction directory remained.";
        ensureInside(temporaryRoot(), directory);
        try {
            if (containsSymbolicLink(directory)) {
                throw new Matrix26RestoreException("The temporary directory contains a symbolic link.");
            }
            deleteTree(directory);
        } catch (IOException ex) {
            throw new Matrix26RestoreException("Could not remove the restore temporary directory: " + safeMessage(ex), ex);
        }
        return "Restore temporary extraction directory removed.";
    }

    private Matrix26RestoreCleanupSnapshot snapshot(Matrix26RestoreJob job) {
        List<String> blockers = new ArrayList<>();
        List<Matrix26RestoreCleanupPlanItem> items = new ArrayList<>();
        ensureFixedTarget(job, blockers);
        ensureSourceBackupAvailable(job, blockers);

        Matrix26RestoreValidationRun validation = restoreRepository.findLatestValidationRun(job.id()).orElse(null);
        if (validation != null && (validation.status() == Matrix26RestoreValidationStatus.VERIFIED
                || validation.status() == Matrix26RestoreValidationStatus.VERIFIED_WITH_WARNINGS)) {
            blockers.add("The clone has a successful restore verification and must use decommission instead of failed-restore cleanup.");
        }

        PlatformBusinessClient registered = clientRepository.findByCodeIgnoreCase(job.targetInstanceCode()).orElse(null);
        boolean registrationOwned = registered != null && matchingTarget(job, registered);
        if (registered != null && !registrationOwned) blockers.add("The target registration conflicts with this restore job.");
        if (registered != null && jdbcCount("SELECT COUNT(*) FROM matrix26_backup_job WHERE instance_id = ?", registered.getId()) > 0) {
            blockers.add("The target clone already owns backup records and must use decommission.");
        }
        if (registered != null && jdbcCount("SELECT COUNT(*) FROM matrix26_restore_job WHERE target_instance_id = ? AND id <> ?", registered.getId(), job.id()) > 0) {
            blockers.add("Another restore job references the target registration.");
        }

        Optional<Matrix26RuntimeInventoryItem> runtime = runtimeForCode(job.targetInstanceCode());
        boolean runtimeRunning = runtime.map(item -> item.portListening() || item.processId() != null).orElse(false);
        String runtimeOwnership = registrationOwned ? "OWNED" : registered == null ? "MISSING" : "CONFLICT";
        if (runtimeRunning && runtime.map(Matrix26RuntimeInventoryItem::expectedProcess).orElse(false) == false) {
            blockers.add("Port " + job.targetRuntimePort() + " is occupied by an unexpected process.");
            runtimeOwnership = "CONFLICT";
        } else if (registered == null && !portAvailable(job.targetRuntimePort())) {
            blockers.add("Port " + job.targetRuntimePort() + " is occupied without a matching registration.");
            runtimeOwnership = "CONFLICT";
        }
        items.add(item(10, "RUNTIME_PROCESS", "port:" + job.targetRuntimePort(), runtimeRunning,
                runtimeOwnership, runtimeRunning ? "STOP" : "SKIP", "STOP_RUNTIME",
                runtimeRunning ? "The owned clone runtime must stop before cleanup." : "No running clone process detected."));

        int moduleCount = registered == null ? 0 : jdbcCount("SELECT COUNT(*) FROM platform_client_module WHERE client_id = ?", registered.getId());
        items.add(item(20, "MODULE_ASSIGNMENTS", job.targetInstanceCode(), moduleCount > 0,
                registrationOwned ? "OWNED" : registered == null ? "MISSING" : "CONFLICT",
                moduleCount > 0 && registrationOwned ? "REMOVE" : moduleCount == 0 ? "SKIP" : "BLOCKED",
                "REGISTRATION", moduleCount + " module assignment(s) detected."));

        items.add(item(30, "INSTANCE_REGISTRATION", job.targetInstanceCode(), registered != null,
                registrationOwned ? "OWNED" : registered == null ? "MISSING" : "CONFLICT",
                registrationOwned ? "REMOVE" : registered == null ? "SKIP" : "BLOCKED",
                "REGISTRATION", registered == null ? "No central registration exists." : "Central target registration detected."));

        addDirectoryItem(items, blockers, 40, "RUNTIME_DIRECTORY", targetRuntimeDirectory(job), runtimeRoot(), job, "FILES");
        addDirectoryItem(items, blockers, 50, "RUNTIME_DATA", targetDataDirectory(job), dataRoot(), job, "FILES");

        boolean databaseExists = targetDatabaseService.databaseExists(job.targetDatabaseName());
        int tableCount = databaseExists ? targetDatabaseService.tableCount(job.targetDatabaseName()) : 0;
        items.add(item(60, "DATABASE", job.targetDatabaseName(), databaseExists,
                fixedDatabase(job) ? "OWNED_RESTORE_TARGET" : "CONFLICT",
                databaseExists && fixedDatabase(job) ? "DROP" : databaseExists ? "BLOCKED" : "SKIP",
                "DATABASE", databaseExists ? tableCount + " table(s) detected in the isolated target database." : "No target database exists."));
        if (databaseExists && !fixedDatabase(job)) blockers.add("The database name is outside the fixed restore cleanup boundary.");

        Path temporary = temporaryDirectory(job);
        items.add(item(70, "TEMPORARY_EXTRACTION", temporary.toString(), Files.exists(temporary), "EPHEMERAL",
                Files.exists(temporary) ? "REMOVE" : "SKIP", "FILES",
                Files.exists(temporary) ? "Restore temporary extraction remains." : "No temporary extraction remains."));

        boolean backupAvailable = sourceBackupAvailable(job);
        items.add(item(80, "SOURCE_BACKUP", job.backupPublicId(), backupAvailable, backupAvailable ? "PROTECTED" : "MISSING",
                "KEEP", "NONE", backupAvailable ? "Encrypted source backup remains protected and is never removed by cleanup."
                        : "The encrypted source backup is unavailable."));

        for (Matrix26RestoreCleanupPlanItem item : items) {
            if ("BLOCKED".equals(item.plannedAction())) blockers.add(item.resourceType() + " requires manual ownership review.");
        }
        String canonical = canonical(job, items, blockers);
        String fingerprint = sha256(canonical.getBytes(StandardCharsets.UTF_8));
        long existing = items.stream().filter(Matrix26RestoreCleanupPlanItem::existedAtPreview).count();
        String summary = existing + " resource groups detected. " + (blockers.isEmpty()
                ? "The signed plan is eligible for independent approval."
                : "No destructive action is allowed until all blockers are resolved.");
        return new Matrix26RestoreCleanupSnapshot(fingerprint, List.copyOf(items), List.copyOf(blockers), summary);
    }

    private void addDirectoryItem(
            List<Matrix26RestoreCleanupPlanItem> items,
            List<String> blockers,
            int sequence,
            String type,
            Path directory,
            Path root,
            Matrix26RestoreJob job,
            String confirmationGroup
    ) {
        boolean exists = Files.exists(directory);
        String ownership = directoryOwnership(directory, root, job.publicId());
        String action = !exists ? "SKIP" : "OWNED".equals(ownership) ? "REMOVE" : "BLOCKED";
        if (exists && !"OWNED".equals(ownership)) blockers.add(type + " ownership is " + ownership + ".");
        items.add(item(sequence, type, relativeOrAbsolute(directory), exists, ownership, action, confirmationGroup,
                exists ? "Directory detected and checked against the restore ownership marker." : "Directory is absent."));
    }

    private Matrix26RestoreCleanupPlanItem item(
            int sequence, String type, String location, boolean exists, String ownership,
            String action, String confirmationGroup, String detail
    ) {
        Matrix26RestoreCleanupItemStatus status = "BLOCKED".equals(action)
                ? Matrix26RestoreCleanupItemStatus.BLOCKED
                : Matrix26RestoreCleanupItemStatus.PENDING;
        return new Matrix26RestoreCleanupPlanItem(null, null, sequence, type, location, exists, ownership,
                action, confirmationGroup, status, detail, null, null, null);
    }

    private void ensureFixedTarget(Matrix26RestoreJob job, List<String> blockers) {
        if (!properties.getTargetInstanceCode().equals(job.targetInstanceCode())) blockers.add("Unexpected target instance code.");
        if (!properties.getTargetDatabaseName().equals(job.targetDatabaseName())) blockers.add("Unexpected target database name.");
        if (!properties.getTargetRuntimeProfile().equals(job.targetRuntimeProfile())) blockers.add("Unexpected target runtime profile.");
        if (properties.getTargetRuntimePort() != job.targetRuntimePort()) blockers.add("Unexpected target runtime port.");
        if (PROTECTED_CODES.contains(job.targetInstanceCode().toLowerCase(Locale.ROOT))) blockers.add("The target code is protected from cleanup.");
        if (job.sourceDatabaseName().equalsIgnoreCase(job.targetDatabaseName())) blockers.add("Source and target database names are identical.");
    }

    private boolean fixedDatabase(Matrix26RestoreJob job) {
        return properties.getTargetDatabaseName().equals(job.targetDatabaseName())
                && !job.sourceDatabaseName().equalsIgnoreCase(job.targetDatabaseName())
                && !"matrix26_platform_control".equalsIgnoreCase(job.targetDatabaseName());
    }

    private void ensureSourceBackupAvailable(Matrix26RestoreJob job) {
        if (!sourceBackupAvailable(job)) throw new Matrix26RestoreException("The encrypted source backup is unavailable; cleanup is blocked.");
    }

    private void ensureSourceBackupAvailable(Matrix26RestoreJob job, List<String> blockers) {
        if (!sourceBackupAvailable(job)) blockers.add("The encrypted source backup is unavailable.");
    }

    private boolean sourceBackupAvailable(Matrix26RestoreJob job) {
        Matrix26BackupJob backup = backupRepository.findById(job.backupJobId()).orElse(null);
        if (backup == null || !backup.isCompleted() || backup.backupDirectory() == null) return false;
        Path directory = Path.of(backup.backupDirectory()).toAbsolutePath().normalize();
        return Files.isRegularFile(directory.resolve("package.m26backup"))
                && Files.isRegularFile(directory.resolve("public-manifest.json"))
                && Files.isRegularFile(directory.resolve("checksums.sha256"));
    }

    private List<String> residuals(Matrix26RestoreJob job) {
        List<String> result = new ArrayList<>();
        if (clientRepository.findByCodeIgnoreCase(job.targetInstanceCode()).isPresent()) result.add("instance registration");
        if (Files.exists(targetRuntimeDirectory(job))) result.add("runtime directory");
        if (Files.exists(targetDataDirectory(job))) result.add("runtime-data directory");
        if (targetDatabaseService.databaseExists(job.targetDatabaseName())) result.add("database");
        if (Files.exists(temporaryDirectory(job))) result.add("temporary extraction");
        if (!portAvailable(job.targetRuntimePort())) result.add("port " + job.targetRuntimePort());
        return result;
    }

    private Matrix26RestoreJob restoreJob(long id) {
        return restoreRepository.findById(id)
                .orElseThrow(() -> new Matrix26RestoreException("Restore job not found."));
    }

    private void ensureCleanupCandidate(Matrix26RestoreJob job) {
        if (!(job.status() == Matrix26RestoreStatus.FAILED
                || job.status() == Matrix26RestoreStatus.CLEANUP_REQUIRED
                || job.status() == Matrix26RestoreStatus.PARTIALLY_CLEANED
                || job.status() == Matrix26RestoreStatus.CLEANING)) {
            throw new Matrix26RestoreException("Only incomplete or partially cleaned restore jobs are cleanup candidates.");
        }
    }

    private void ensureCleanupEnabled() {
        if (!properties.isCleanupEnabled()) throw new Matrix26RestoreException("Restore cleanup is disabled.");
    }

    private Matrix26RestoreCleanupPlan planForJob(long planId, long jobId) {
        Matrix26RestoreCleanupPlan plan = cleanupRepository.findById(planId)
                .orElseThrow(() -> new Matrix26RestoreException("Cleanup plan not found."));
        if (!plan.restoreJobId().equals(jobId)) throw new Matrix26RestoreException("Cleanup plan does not belong to this restore job.");
        return plan;
    }

    private void verifyPlanSignature(Matrix26RestoreCleanupPlan plan) {
        String expected = sign(plan.publicId(), plan.restoreJobId(), plan.snapshotFingerprint());
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), plan.planSignature().getBytes(StandardCharsets.UTF_8))) {
            throw new Matrix26RestoreException("Cleanup plan signature validation failed.");
        }
    }

    private String sign(String publicId, long jobId, String fingerprint) {
        try {
            byte[] key = masterKey();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((publicId + "|" + jobId + "|" + fingerprint)
                    .getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new Matrix26RestoreException("Cleanup plan signing failed: " + safeMessage(ex), ex);
        }
    }

    private byte[] masterKey() {
        String variable = firstNonBlank(backupProperties.getMasterKeyEnvironment(), "MATRIX26_BACKUP_MASTER_KEY");
        String raw = System.getenv(variable);
        if (raw == null || raw.isBlank()) raw = System.getProperty(variable);
        if (raw == null || raw.isBlank()) throw new Matrix26RestoreException("Cleanup plan signing key is unavailable. Set " + variable + ".");
        byte[] material;
        try {
            material = Base64.getDecoder().decode(raw.trim());
        } catch (IllegalArgumentException ex) {
            material = raw.getBytes(StandardCharsets.UTF_8);
        }
        if (material.length < 32) throw new Matrix26RestoreException("Cleanup plan signing key must provide at least 32 bytes.");
        try {
            return MessageDigest.getInstance("SHA-256").digest(material);
        } catch (Exception ex) {
            throw new Matrix26RestoreException("Cleanup signing key could not be derived.", ex);
        }
    }

    private String canonical(Matrix26RestoreJob job, List<Matrix26RestoreCleanupPlanItem> items, List<String> blockers) {
        StringBuilder value = new StringBuilder();
        value.append(job.publicId()).append('|').append(job.id()).append('|')
                .append(job.status()).append('|').append(job.targetInstanceCode()).append('|')
                .append(job.targetDatabaseName()).append('|').append(job.targetRuntimeProfile()).append('|')
                .append(job.targetRuntimePort()).append('\n');
        items.stream().sorted(Comparator.comparingInt(Matrix26RestoreCleanupPlanItem::sequenceNumber)).forEach(item -> value
                .append(item.sequenceNumber()).append('|').append(item.resourceType()).append('|')
                .append(item.location()).append('|').append(item.existedAtPreview()).append('|')
                .append(item.ownership()).append('|').append(item.plannedAction()).append('|')
                .append(item.detail()).append('\n'));
        blockers.stream().sorted().forEach(blocker -> value.append("BLOCKER|").append(blocker).append('\n'));
        return value.toString();
    }

    private Optional<PlatformBusinessClient> matchingRegistration(Matrix26RestoreJob job) {
        return clientRepository.findByCodeIgnoreCase(job.targetInstanceCode())
                .filter(target -> matchingTarget(job, target));
    }

    private boolean matchingTarget(Matrix26RestoreJob job, PlatformBusinessClient target) {
        return target != null
                && job.targetInstanceCode().equalsIgnoreCase(target.getCode())
                && job.targetDatabaseName().equalsIgnoreCase(target.getDatabaseName())
                && job.targetRuntimeProfile().equalsIgnoreCase(target.getRuntimeProfile())
                && target.getRuntimePort() != null
                && job.targetRuntimePort().equals(target.getRuntimePort())
                && "RESTORED_CLONE".equalsIgnoreCase(target.getManagementMode());
    }

    private Optional<Matrix26RuntimeInventoryItem> runtimeForCode(String code) {
        try {
            return inventoryService.snapshot(true).runtimes().stream()
                    .filter(item -> code.equalsIgnoreCase(item.target().code()))
                    .findFirst();
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String directoryOwnership(Path directory, Path allowedRoot, String expectedOwner) {
        if (!Files.exists(directory)) return "MISSING";
        try {
            ensureInside(allowedRoot, directory);
            if (Files.isSymbolicLink(directory) || containsSymbolicLink(directory)) return "SYMLINK_BLOCKED";
            Path marker = directory.resolve(".matrix26-restore-reference");
            if (!Files.isRegularFile(marker)) return "MARKER_MISSING";
            return expectedOwner.equals(Files.readString(marker, StandardCharsets.UTF_8).trim()) ? "OWNED" : "CONFLICT";
        } catch (IOException | RuntimeException ex) {
            return "UNREADABLE";
        }
    }

    private Path projectRoot() { return Path.of("").toAbsolutePath().normalize(); }
    private Path runtimeRoot() { return projectRoot().resolve(properties.getRuntimeDirectory()).normalize(); }
    private Path dataRoot() { return projectRoot().resolve(properties.getRuntimeDataDirectory()).normalize(); }
    private Path targetRuntimeDirectory(Matrix26RestoreJob job) {
        Path value = runtimeRoot().resolve(job.targetRuntimeProfile()).normalize();
        ensureInside(runtimeRoot(), value);
        return value;
    }
    private Path targetDataDirectory(Matrix26RestoreJob job) {
        Path value = dataRoot().resolve(job.targetInstanceCode()).normalize();
        ensureInside(dataRoot(), value);
        return value;
    }
    private Path backupRoot() {
        String configured = firstNonBlank(System.getenv("MATRIX26_BACKUP_ROOT"), backupProperties.getRootDirectory());
        return (configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "Matrix26", "backups") : Path.of(configured))
                .toAbsolutePath().normalize();
    }
    private Path temporaryRoot() { return backupRoot().resolve(".matrix26-restore-temp").normalize(); }
    private Path temporaryDirectory(Matrix26RestoreJob job) {
        Path value = temporaryRoot().resolve(job.publicId()).normalize();
        ensureInside(temporaryRoot(), value);
        return value;
    }

    private boolean containsSymbolicLink(Path root) throws IOException {
        if (Files.isSymbolicLink(root)) return true;
        try (var stream = Files.walk(root)) {
            return stream.anyMatch(Files::isSymbolicLink);
        }
    }

    private void deleteTree(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean portAvailable(Integer port) {
        if (port == null || port <= 0) return false;
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private int jdbcCount(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }

    private String relativeOrAbsolute(Path path) {
        try { return projectRoot().relativize(path).toString(); }
        catch (RuntimeException ex) { return path.toString(); }
    }

    private void ensureInside(Path root, Path value) {
        Path safeRoot = root.toAbsolutePath().normalize();
        Path safeValue = value.toAbsolutePath().normalize();
        if (!safeValue.startsWith(safeRoot) || safeValue.equals(safeRoot)) {
            throw new Matrix26RestoreException("Cleanup path is outside the allowed boundary.");
        }
    }

    private String safeIdentifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new Matrix26RestoreException("Unsafe SQL identifier rejected.");
        }
        return value;
    }

    private String sha256(byte[] value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (Exception ex) { throw new Matrix26RestoreException("Cleanup fingerprint could not be generated.", ex); }
    }

    private void requireConfirmation(String expected, String actual) {
        if (!expected.equals(actual == null ? "" : actual.trim())) {
            throw new Matrix26RestoreException("Type exactly: " + expected);
        }
    }

    private String safeActor(String actor) {
        return actor == null || actor.isBlank() ? "matrix26-admin" : limit(actor.trim(), 120);
    }
    private String safeMessage(Throwable ex) {
        return limit(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 4000);
    }
    private String limit(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }
    private String firstNonBlank(String first, String second) { return first != null && !first.isBlank() ? first : second; }
}
