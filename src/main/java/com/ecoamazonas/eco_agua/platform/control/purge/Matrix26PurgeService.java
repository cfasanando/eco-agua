package com.ecoamazonas.eco_agua.platform.control.purge;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupEncryption;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupRetentionClass;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupSecurityService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupVerificationState;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.archive.Matrix26ArchiveRecord;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.archive.Matrix26ArchiveRepository;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.archive.Matrix26ArchiveService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26PurgeService {
    private final Matrix26PurgeRepository repository;
    private final Matrix26PurgeProperties properties;
    private final Matrix26ArchiveRepository archiveRepository;
    private final Matrix26ArchiveService archiveService;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26BackupSecurityService backupSecurityService;

    public Matrix26PurgeService(
            Matrix26PurgeRepository repository,
            Matrix26PurgeProperties properties,
            Matrix26ArchiveRepository archiveRepository,
            Matrix26ArchiveService archiveService,
            PlatformBusinessClientRepository clientRepository,
            Matrix26BackupSecurityService backupSecurityService
    ) {
        this.repository = repository;
        this.properties = properties;
        this.archiveRepository = archiveRepository;
        this.archiveService = archiveService;
        this.clientRepository = clientRepository;
        this.backupSecurityService = backupSecurityService;
    }

    public Matrix26PurgeSummary summary() {
        return repository.summary();
    }

    public List<Matrix26PurgeCandidate> candidates() {
        archiveService.refresh("matrix26-system");
        return archiveRepository.findAll().stream()
                .map(record -> new Matrix26PurgeCandidate(
                        record.id(),
                        record.publicId(),
                        record.instanceCode(),
                        record.instanceName(),
                        record.archiveStatus(),
                        record.instanceStatus(),
                        record.finalBackupPublicId(),
                        record.retentionStatus(),
                        record.retentionUntil(),
                        allowlisted(record.instanceCode())
                ))
                .toList();
    }

    public List<Matrix26PurgePlan> recentPlans() {
        return repository.recentPlans();
    }

    public Matrix26PurgePlan plan(long id) {
        return repository.findPlan(id)
                .orElseThrow(() -> new Matrix26PurgeException("The purge plan does not exist."));
    }

    public List<Matrix26PurgeItem> items(long planId) {
        return repository.items(planId);
    }

    public List<Matrix26PurgeCheck> checks(long planId) {
        return repository.checks(planId);
    }

    public List<Matrix26PurgeEvent> events(long planId) {
        return repository.events(planId);
    }

    public Matrix26PurgePlan prepare(long archiveRecordId, String reason, String confirmation, String actor) {
        validateEnabled();
        Matrix26ArchiveRecord archive = archiveRepository.findById(archiveRecordId)
                .orElseThrow(() -> new Matrix26PurgeException("The selected final archive does not exist."));
        String expected = "PREPARE PURGE DRY RUN " + archive.instanceCode();
        if (!expected.equals(confirmation == null ? "" : confirmation.trim())) {
            throw new Matrix26PurgeException("Type exactly: " + expected);
        }
        if (reason == null || reason.trim().length() < properties.getMinimumReasonLength()) {
            throw new Matrix26PurgeException("The dry run reason must contain at least "
                    + properties.getMinimumReasonLength() + " characters.");
        }
        PlatformBusinessClient instance = clientRepository.findById(archive.instanceId())
                .orElseThrow(() -> new Matrix26PurgeException("The archived instance registry entry is missing."));
        Matrix26PurgePlan draft = new Matrix26PurgePlan(
                null,
                publicId(),
                archive.id(),
                archive.publicId(),
                archive.decommissionJobId(),
                instance.getId(),
                instance.getCode(),
                instance.getBusinessName(),
                instance.getDatabaseName(),
                instance.getRuntimeProfile(),
                instance.getRuntimePort(),
                archive.finalBackupJobId(),
                archive.finalBackupPublicId(),
                archive.finalBackupSha256(),
                archive.retentionUntil(),
                archive.retentionStatus(),
                Matrix26PurgeStatus.DRAFT,
                1,
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                reason.trim(),
                safeActor(actor),
                LocalDateTime.now(),
                null,
                null
        );
        long id = repository.insertPlan(draft);
        repository.addEvent(id, "PURGE_DRY_RUN_REQUESTED", "COMPLETED", safeActor(actor),
                "Dry run created for archived instance " + archive.instanceCode() + ". No resource was deleted.");
        return evaluate(id, safeActor(actor));
    }

    public Matrix26PurgePlan refresh(long planId, String actor) {
        validateEnabled();
        Matrix26PurgePlan plan = plan(planId);
        if (plan.status() == Matrix26PurgeStatus.PURGING || plan.status() == Matrix26PurgeStatus.PURGED) {
            throw new Matrix26PurgeException("This purge plan is already executing or completed and cannot be refreshed.");
        }
        repository.addEvent(planId, "PURGE_DRY_RUN_REFRESH_REQUESTED", "COMPLETED", safeActor(actor),
                "Dry run refresh requested. Previous evidence remains stored; current run will be shown on the detail page.");
        return evaluate(planId, safeActor(actor));
    }

    public Matrix26PurgePlan prepareExecution(long planId, String confirmation, String actor) {
        validateExecutionEnabled();
        Matrix26PurgePlan plan = plan(planId);
        if (plan.status() != Matrix26PurgeStatus.DRY_RUN_READY) {
            throw new Matrix26PurgeException("Only a DRY_RUN_READY plan can be prepared for operational purge.");
        }
        String expected = "PREPARE PURGE EXECUTION " + plan.instanceCode();
        if (!expected.equals(confirmation == null ? "" : confirmation.trim())) {
            throw new Matrix26PurgeException("Type exactly: " + expected);
        }
        assertExecutionSafety(plan, true);
        repository.prepareExecutionItems(plan.id(), currentRun(plan));
        repository.updatePlanStatus(plan.id(), Matrix26PurgeStatus.READY_TO_PURGE, null);
        repository.addEvent(plan.id(), "PURGE_EXECUTION_PREPARED", Matrix26PurgeStatus.READY_TO_PURGE.name(), safeActor(actor),
                "The dry run was frozen for controlled operational purge. No resource was deleted yet.");
        return plan(plan.id());
    }

    public Matrix26PurgePlan execute(long planId, String purgeConfirmation, String databaseConfirmation, String actor) {
        validateExecutionEnabled();
        Matrix26PurgePlan plan = plan(planId);
        if (plan.status() != Matrix26PurgeStatus.READY_TO_PURGE && plan.status() != Matrix26PurgeStatus.PARTIALLY_PURGED) {
            throw new Matrix26PurgeException("Only a READY_TO_PURGE plan can execute operational purge.");
        }
        String expectedPurge = "PURGE INSTANCE " + plan.instanceCode();
        if (!expectedPurge.equals(purgeConfirmation == null ? "" : purgeConfirmation.trim())) {
            throw new Matrix26PurgeException("Type exactly: " + expectedPurge);
        }
        String expectedDatabase = "DROP ARCHIVED DATABASE " + plan.databaseName();
        if (!expectedDatabase.equals(databaseConfirmation == null ? "" : databaseConfirmation.trim())) {
            throw new Matrix26PurgeException("Type exactly: " + expectedDatabase);
        }

        assertExecutionSafety(plan, false);
        repository.updatePlanStatus(plan.id(), Matrix26PurgeStatus.PURGING, null);
        repository.markInstancePurging(plan.instanceId(), safeActor(actor));
        repository.addEvent(plan.id(), "PURGE_EXECUTION_STARTED", Matrix26PurgeStatus.PURGING.name(), safeActor(actor),
                "Operational purge started after explicit confirmations. Final archive and audit records will be preserved.");

        List<String> failures = new ArrayList<>();
        for (Matrix26PurgeItem item : repository.items(plan.id())) {
            if (item.disposition() != Matrix26PurgeDisposition.WOULD_DELETE) {
                continue;
            }
            try {
                executeItem(plan, item);
            } catch (RuntimeException ex) {
                failures.add(item.resourceType() + ": " + ex.getMessage());
                repository.updateItemExecutionStatus(item.id(), "FAILED", ex.getMessage());
                repository.addEvent(plan.id(), "PURGE_ITEM_FAILED", "FAILED", safeActor(actor),
                        item.resourceType() + " failed: " + ex.getMessage());
            }
        }

        if (failures.isEmpty()) {
            repository.markInstancePurged(plan.instanceId(), safeActor(actor));
            repository.updatePlanStatus(plan.id(), Matrix26PurgeStatus.PURGED, null);
            repository.addEvent(plan.id(), "PURGE_EXECUTION_COMPLETED", Matrix26PurgeStatus.PURGED.name(), safeActor(actor),
                    "Operational purge completed. Deleted items: " + repository.deletedItemCount(plan.id())
                            + ". Final archive, backups, archive records, decommission records, purge records, and clone restores were preserved.");
        } else {
            String error = String.join(" | ", failures);
            repository.markInstanceManualReview(plan.instanceId(), error);
            repository.updatePlanStatus(plan.id(), Matrix26PurgeStatus.PARTIALLY_PURGED, error);
            repository.addEvent(plan.id(), "PURGE_EXECUTION_PARTIAL", Matrix26PurgeStatus.PARTIALLY_PURGED.name(), safeActor(actor),
                    "Operational purge stopped with " + failures.size() + " failure(s): " + error);
        }
        return plan(plan.id());
    }

    public String report(long planId) {
        Matrix26PurgePlan plan = plan(planId);
        StringBuilder builder = new StringBuilder();
        builder.append("Matrix26 Purge Manager - Operational Report\n");
        builder.append("Plan: ").append(plan.publicId()).append('\n');
        builder.append("Instance: ").append(plan.instanceName()).append(" (").append(plan.instanceCode()).append(")\n");
        builder.append("Status: ").append(plan.status()).append('\n');
        builder.append("Eligible for purge: ").append(plan.eligibleForFuturePurge()).append('\n');
        builder.append("Reason: ").append(plan.reason()).append("\n\n");
        builder.append("Checks\n");
        for (Matrix26PurgeCheck check : checks(planId)) {
            builder.append("- [").append(check.status()).append("] ")
                    .append(check.label()).append(": ").append(nullToBlank(check.detail())).append('\n');
        }
        builder.append("\nItems\n");
        for (Matrix26PurgeItem item : items(planId)) {
            builder.append("- [").append(item.disposition()).append("] ")
                    .append(item.resourceType()).append(" | ").append(item.resourceName())
                    .append(" | ").append(nullToBlank(item.resourcePath()))
                    .append(" | execution=").append(item.executionStatusLabel())
                    .append(" | ").append(nullToBlank(item.executionDetail()))
                    .append('\n');
        }
        builder.append("\nDeleted resources in Phase 3H.2: ").append(repository.deletedItemCount(planId)).append('\n');
        builder.append("Preserved: final archive, protected backups, decommission records, archive records, purge records, and archive clone links.\n");
        return builder.toString();
    }

    private Matrix26PurgePlan evaluate(long planId, String actor) {
        Matrix26PurgePlan plan = plan(planId);
        int runNumber = plan.runNumber() == null ? 1 : plan.runNumber() + 1;
        repository.markRunning(planId, runNumber);
        List<Matrix26PurgeDisposition> dispositions = new ArrayList<>();
        try {
            Matrix26ArchiveRecord archive = archiveRepository.findById(plan.archiveRecordId())
                    .orElseThrow(() -> new Matrix26PurgeException("The archive record no longer exists."));
            PlatformBusinessClient instance = clientRepository.findById(plan.instanceId())
                    .orElseThrow(() -> new Matrix26PurgeException("The archived instance registry entry is missing."));

            check(planId, runNumber, "FEATURE_ENABLED", "Purge Manager dry run enabled",
                    properties.isEnabled(), "Non-destructive classification is always completed before operational purge.");
            check(planId, runNumber, "ALLOWLIST", "Instance is allowlisted for purge planning",
                    allowlisted(instance.getCode()), "Allowed instance codes: " + properties.getAllowedInstanceCodes());
            check(planId, runNumber, "PROTECTED_INSTANCE", "Instance is not protected",
                    !instance.isProtectedInstance() && !protectedCode(instance.getCode()),
                    "Protected runtime clients 8081, 8082, 8084, Matrix26 8091 and archive clones are blocked.");
            check(planId, runNumber, "DECOMMISSIONED", "Original instance is decommissioned",
                    "DECOMMISSIONED".equalsIgnoreCase(instance.getStatus())
                            || "PURGED".equalsIgnoreCase(instance.getStatus())
                            || "PURGING".equalsIgnoreCase(instance.getStatus()),
                    "Current instance status: " + instance.getStatus());
            check(planId, runNumber, "ARCHIVE_READY", "Final archive is ready",
                    "READY".equalsIgnoreCase(archive.archiveStatus()),
                    "Current archive status: " + archive.archiveStatus());

            Matrix26BackupEncryption encryption = finalBackupMetadata(plan);
            boolean finalBackupOk = finalBackupOk(encryption);
            check(planId, runNumber, "FINAL_BACKUP", "Final backup is encrypted, FINAL, protected, and VERIFIED",
                    finalBackupOk,
                    encryption == null ? "No encrypted backup metadata found." : "Package SHA-256: " + nullToBlank(encryption.packageSha256()));

            boolean retentionExpired = archive.retentionUntil() != null && !archive.retentionUntil().isAfter(LocalDateTime.now());
            boolean retentionAllowed = !properties.isRequireRetentionExpired() || retentionExpired;
            check(planId, runNumber, "RETENTION", "Retention policy allows the configured purge stage",
                    retentionAllowed,
                    archive.retentionUntil() == null
                            ? "No retention date found."
                            : "Retention until: " + archive.retentionUntil() + ". Expired: " + retentionExpired);

            boolean portAvailable = instance.getRuntimePort() == null || isPortAvailable(instance.getRuntimePort());
            check(planId, runNumber, "ORIGINAL_PORT", "Original runtime port is free",
                    portAvailable,
                    instance.getRuntimePort() == null ? "No runtime port configured." : "Port " + instance.getRuntimePort());
            check(planId, runNumber, "NO_ACTIVE_OPERATIONS", "No active backup or restore blocks the plan",
                    !repository.hasActiveBackupOrRestore(instance.getId(), instance.getCode()),
                    "Active backup, clone restore, and in-place restore states are checked in Matrix26 metadata.");
            check(planId, runNumber, "NO_ACTIVE_ARCHIVE_RESTORE", "No active archive clone restore is running",
                    !repository.hasActiveArchiveCloneRestore(archive.id()),
                    "Existing completed clones are preserved; only active clone restore jobs block execution.");
            check(planId, runNumber, "SCHEDULES_DISABLED", "Backup schedules are disabled",
                    !repository.hasEnabledSchedules(instance.getId()),
                    "Total schedules registered: " + repository.totalSchedules(instance.getId()));

            int cloneCount = repository.associatedCloneCount(archive.id());
            check(planId, runNumber, "ARCHIVE_CLONES", "Archive restore clones are preserved",
                    true,
                    cloneCount == 0 ? "No clone restore links were found." : cloneCount + " clone restore link(s) are associated with this archive and will not be purged.");

            classifyDatabase(planId, runNumber, instance, dispositions);
            classifyDirectory(planId, runNumber, "RUNTIME_DIRECTORY", instance.getRuntimeProfile(),
                    Path.of(properties.getRuntimeDirectory()).resolve(nullToBlank(instance.getRuntimeProfile())), dispositions);
            classifyDirectory(planId, runNumber, "RUNTIME_DATA_DIRECTORY", instance.getCode(),
                    Path.of(properties.getDataDirectory()).resolve(nullToBlank(instance.getCode())), dispositions);
            classifyBackupRoot(planId, runNumber, instance, archive, finalBackupOk, dispositions);
            classifyMetadata(planId, runNumber, instance, archive, cloneCount, dispositions);

            int blockers = countChecks(planId);
            Matrix26PurgeRepository.Counts counts = counts(dispositions, blockers);
            boolean eligible = blockers == 0;
            Matrix26PurgeStatus status = eligible ? Matrix26PurgeStatus.DRY_RUN_READY : Matrix26PurgeStatus.BLOCKED;
            repository.completePlan(planId, status, eligible, counts,
                    eligible ? null : "The dry run found blockers. No resource was deleted.");
            repository.addEvent(planId, "PURGE_DRY_RUN_EVALUATED", status.name(), actor,
                    "Dry run completed with " + blockers + " blocker(s). Deleted resources: 0.");
            return plan(planId);
        } catch (RuntimeException ex) {
            repository.failPlan(planId, ex.getMessage());
            repository.addEvent(planId, "PURGE_DRY_RUN_FAILED", "FAILED", actor, ex.getMessage());
            throw ex;
        }
    }

    private void assertExecutionSafety(Matrix26PurgePlan plan, boolean preparing) {
        if (!allowlisted(plan.instanceCode()) || protectedCode(plan.instanceCode())) {
            throw new Matrix26PurgeException("The selected instance is not allowed for operational purge.");
        }
        if (plan.blockers() > 0 || !plan.eligibleForFuturePurge()) {
            throw new Matrix26PurgeException("The purge plan still has blockers. Refresh the dry run and resolve blockers first.");
        }
        Matrix26ArchiveRecord archive = archiveRepository.findById(plan.archiveRecordId())
                .orElseThrow(() -> new Matrix26PurgeException("The archive record no longer exists."));
        PlatformBusinessClient instance = clientRepository.findById(plan.instanceId())
                .orElseThrow(() -> new Matrix26PurgeException("The archived instance registry entry is missing."));
        if (!"READY".equalsIgnoreCase(archive.archiveStatus())) {
            throw new Matrix26PurgeException("The final archive must remain READY before purge execution.");
        }
        if (!"DECOMMISSIONED".equalsIgnoreCase(instance.getStatus())
                && !"PURGING".equalsIgnoreCase(instance.getStatus())
                && !"MANUAL_REVIEW_REQUIRED".equalsIgnoreCase(instance.getStatus())) {
            throw new Matrix26PurgeException("The original instance must be DECOMMISSIONED before purge execution. Current status: " + instance.getStatus());
        }
        if (instance.getRuntimePort() != null && !isPortAvailable(instance.getRuntimePort())) {
            throw new Matrix26PurgeException("The original runtime port is still busy: " + instance.getRuntimePort());
        }
        if (repository.hasEnabledSchedules(instance.getId())) {
            throw new Matrix26PurgeException("Enabled backup schedules still exist for the archived instance.");
        }
        if (repository.hasActiveBackupOrRestore(instance.getId(), instance.getCode())) {
            throw new Matrix26PurgeException("An active backup or restore still references the archived instance.");
        }
        if (repository.hasActiveArchiveCloneRestore(archive.id())) {
            throw new Matrix26PurgeException("An archive clone restore is still running. Wait until it completes before purging the original resources.");
        }
        Matrix26BackupEncryption encryption = finalBackupMetadata(plan);
        if (!finalBackupOk(encryption)) {
            throw new Matrix26PurgeException("The final backup is not encrypted, FINAL, protected and VERIFIED anymore.");
        }
        if (preparing && plan.status() != Matrix26PurgeStatus.DRY_RUN_READY) {
            throw new Matrix26PurgeException("The plan must be DRY_RUN_READY before preparation.");
        }
    }

    private void executeItem(Matrix26PurgePlan plan, Matrix26PurgeItem item) {
        switch (item.resourceType()) {
            case "DATABASE" -> purgeDatabase(plan, item);
            case "RUNTIME_DIRECTORY" -> purgeDirectory(item, Path.of(properties.getRuntimeDirectory()), "runtime");
            case "RUNTIME_DATA_DIRECTORY" -> purgeDirectory(item, Path.of(properties.getDataDirectory()), "runtime-data");
            default -> repository.updateItemExecutionStatus(item.id(), "SKIPPED_KEEP",
                    "This WOULD_DELETE item type has no operational purge handler and was preserved.");
        }
    }

    private void purgeDatabase(Matrix26PurgePlan plan, Matrix26PurgeItem item) {
        String database = nullToBlank(plan.databaseName());
        if (database.isBlank()) {
            repository.updateItemExecutionStatus(item.id(), "NOT_FOUND", "No database name exists in the plan.");
            return;
        }
        if (!database.equals(item.resourceName())) {
            throw new Matrix26PurgeException("Database item does not match the frozen plan.");
        }
        if (!repository.schemaExists(database)) {
            repository.updateItemExecutionStatus(item.id(), "NOT_FOUND", "The database schema was already absent.");
            return;
        }
        repository.dropSchema(database);
        if (repository.schemaExists(database)) {
            throw new Matrix26PurgeException("The database still exists after DROP DATABASE.");
        }
        repository.updateItemExecutionStatus(item.id(), "DELETED", "Archived database schema dropped after explicit confirmation.");
    }

    private void purgeDirectory(Matrix26PurgeItem item, Path root, String label) {
        Path rootPath = root.toAbsolutePath().normalize();
        Path target = Path.of(nullToBlank(item.resourcePath())).toAbsolutePath().normalize();
        if (!isSafeChild(rootPath, target)) {
            throw new Matrix26PurgeException("Unsafe " + label + " path refused: " + target);
        }
        if (!Files.exists(target)) {
            repository.updateItemExecutionStatus(item.id(), "NOT_FOUND", "The " + label + " directory was already absent.");
            return;
        }
        deleteDirectory(target);
        if (Files.exists(target)) {
            throw new Matrix26PurgeException("The " + label + " directory still exists after deletion: " + target);
        }
        repository.updateItemExecutionStatus(item.id(), "DELETED", "Archived " + label + " directory deleted after explicit confirmation.");
    }

    private void deleteDirectory(Path directory) {
        try (Stream<Path> stream = Files.walk(directory)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            throw new Matrix26PurgeException("Could not delete directory " + directory + ": " + ex.getMessage());
        }
    }

    private boolean isSafeChild(Path root, Path target) {
        return target.startsWith(root) && !target.equals(root);
    }

    private void classifyDatabase(long planId, int runNumber, PlatformBusinessClient instance, List<Matrix26PurgeDisposition> dispositions) {
        String database = nullToBlank(instance.getDatabaseName());
        if (database.isBlank()) {
            item(planId, runNumber, "DATABASE", "No database configured", "", Matrix26PurgeDisposition.NOT_FOUND,
                    null, null, "No database name exists in the instance registry.", dispositions);
            return;
        }
        if (!repository.schemaExists(database)) {
            item(planId, runNumber, "DATABASE", database, database, Matrix26PurgeDisposition.NOT_FOUND,
                    0L, 0, "The schema does not currently exist.", dispositions);
            return;
        }
        item(planId, runNumber, "DATABASE", database, database, Matrix26PurgeDisposition.WOULD_DELETE,
                repository.databaseSizeBytes(database), repository.tableCount(database),
                "Operational purge will drop only this archived laboratory schema after separate confirmations.", dispositions);
    }

    private void classifyDirectory(long planId, int runNumber, String type, String name, Path path, List<Matrix26PurgeDisposition> dispositions) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            item(planId, runNumber, type, nullToBlank(name), normalized.toString(), Matrix26PurgeDisposition.NOT_FOUND,
                    0L, 0, "The directory is already absent.", dispositions);
            return;
        }
        DirectorySnapshot snapshot = snapshot(normalized);
        item(planId, runNumber, type, nullToBlank(name), normalized.toString(), Matrix26PurgeDisposition.WOULD_DELETE,
                snapshot.sizeBytes(), snapshot.fileCount(),
                "Operational purge will remove this archived laboratory directory after separate confirmations.", dispositions);
    }

    private void classifyBackupRoot(
            long planId,
            int runNumber,
            PlatformBusinessClient instance,
            Matrix26ArchiveRecord archive,
            boolean finalBackupOk,
            List<Matrix26PurgeDisposition> dispositions
    ) {
        Path root = Path.of(properties.getBackupRootDirectory()).resolve(instance.getCode()).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            item(planId, runNumber, "BACKUP_ROOT", instance.getCode(), root.toString(), Matrix26PurgeDisposition.NOT_FOUND,
                    0L, 0, "The backup root was not found in the configured location.", dispositions);
            return;
        }
        DirectorySnapshot snapshot = snapshot(root);
        item(planId, runNumber, "BACKUP_ROOT", instance.getCode(), root.toString(), Matrix26PurgeDisposition.WOULD_KEEP,
                snapshot.sizeBytes(), snapshot.fileCount(),
                "Backups stay preserved during operational purge. FINAL archives remain protected.", dispositions);
        item(planId, runNumber, "FINAL_BACKUP", archive.finalBackupPublicId(), archive.finalBackupSha256(),
                finalBackupOk ? Matrix26PurgeDisposition.PROTECTED : Matrix26PurgeDisposition.BLOCKED,
                null, null,
                finalBackupOk ? "Final backup is protected and must not be removed by operational purge." : "Final backup metadata is incomplete or not VERIFIED.", dispositions);
        item(planId, runNumber, "BACKUP_METADATA", "Backup jobs", "matrix26_backup_job", Matrix26PurgeDisposition.WOULD_KEEP,
                null, repository.backupCount(instance.getId()), "Backup metadata stays for audit and final archive traceability.", dispositions);
    }

    private void classifyMetadata(
            long planId,
            int runNumber,
            PlatformBusinessClient instance,
            Matrix26ArchiveRecord archive,
            int cloneCount,
            List<Matrix26PurgeDisposition> dispositions
    ) {
        item(planId, runNumber, "INSTANCE_REGISTRY", instance.getCode(), "platform_business_client", Matrix26PurgeDisposition.WOULD_KEEP,
                null, null, "The historical instance row remains as audit evidence and is marked PURGED after execution.", dispositions);
        item(planId, runNumber, "DECOMMISSION_RECORDS", archive.decommissionPublicId(), "matrix26_decommission_*", Matrix26PurgeDisposition.WOULD_KEEP,
                null, null, "Decommission records stay available for governance and future audit.", dispositions);
        item(planId, runNumber, "ARCHIVE_RECORDS", archive.publicId(), "matrix26_archive_*", Matrix26PurgeDisposition.WOULD_KEEP,
                null, cloneCount, "Archive records and restore links stay available; clone restores are not confused with the original instance.", dispositions);
        item(planId, runNumber, "SCHEDULES", instance.getCode(), "matrix26_backup_schedule", Matrix26PurgeDisposition.WOULD_KEEP,
                null, repository.totalSchedules(instance.getId()), "Disabled schedule metadata stays preserved.", dispositions);
        if (cloneCount > 0) {
            item(planId, runNumber, "ARCHIVE_CLONE_LINKS", archive.publicId(), "matrix26_archive_restore_link", Matrix26PurgeDisposition.REQUIRES_REVIEW,
                    null, cloneCount, "Associated clone restore links exist and will be preserved.", dispositions);
        }
    }

    private void check(long planId, int runNumber, String code, String label, boolean passed, String detail) {
        repository.addCheck(planId, runNumber, code, label, passed ? "PASSED" : "BLOCKED", detail);
    }

    private int countChecks(long planId) {
        int blockers = 0;
        for (Matrix26PurgeCheck check : repository.checks(planId)) {
            if ("BLOCKED".equalsIgnoreCase(check.status())) {
                blockers++;
            }
        }
        return blockers;
    }

    private void item(
            long planId,
            int runNumber,
            String type,
            String name,
            String path,
            Matrix26PurgeDisposition disposition,
            Long sizeBytes,
            Integer fileCount,
            String detail,
            List<Matrix26PurgeDisposition> dispositions
    ) {
        repository.addItem(planId, runNumber, type, name, path, disposition, sizeBytes, fileCount, detail);
        dispositions.add(disposition);
    }

    private Matrix26PurgeRepository.Counts counts(List<Matrix26PurgeDisposition> dispositions, int blockers) {
        return new Matrix26PurgeRepository.Counts(
                blockers,
                count(dispositions, Matrix26PurgeDisposition.WOULD_DELETE),
                count(dispositions, Matrix26PurgeDisposition.WOULD_KEEP),
                count(dispositions, Matrix26PurgeDisposition.PROTECTED),
                count(dispositions, Matrix26PurgeDisposition.REQUIRES_REVIEW),
                count(dispositions, Matrix26PurgeDisposition.NOT_FOUND)
        );
    }

    private int count(List<Matrix26PurgeDisposition> dispositions, Matrix26PurgeDisposition value) {
        int total = 0;
        for (Matrix26PurgeDisposition disposition : dispositions) {
            if (disposition == value) {
                total++;
            }
        }
        return total;
    }

    private DirectorySnapshot snapshot(Path path) {
        if (!Files.exists(path)) {
            return new DirectorySnapshot(0, 0);
        }
        long bytes = 0;
        int files = 0;
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path current : stream.filter(Files::isRegularFile).toList()) {
                files++;
                try {
                    bytes += Files.size(current);
                } catch (IOException ignored) {
                    // Keep the dry run non-blocking when a file disappears during inventory.
                }
            }
        } catch (IOException ignored) {
            return new DirectorySnapshot(bytes, files);
        }
        return new DirectorySnapshot(bytes, files);
    }

    private Matrix26BackupEncryption finalBackupMetadata(Matrix26PurgePlan plan) {
        return plan.finalBackupJobId() == null ? null : backupSecurityService.metadata(plan.finalBackupJobId());
    }

    private boolean finalBackupOk(Matrix26BackupEncryption encryption) {
        return encryption != null
                && encryption.encrypted()
                && encryption.retentionClass() == Matrix26BackupRetentionClass.FINAL
                && encryption.protectedFlag()
                && encryption.verificationStatus() == Matrix26BackupVerificationState.VERIFIED;
    }

    private int currentRun(Matrix26PurgePlan plan) {
        return plan.runNumber() == null ? 1 : plan.runNumber();
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress("127.0.0.1", port));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private void validateEnabled() {
        if (!properties.isEnabled()) {
            throw new Matrix26PurgeException("Purge Manager is disabled in Matrix26 configuration.");
        }
    }

    private void validateExecutionEnabled() {
        validateEnabled();
        if (!properties.isExecutionEnabled()) {
            throw new Matrix26PurgeException("Operational purge execution is disabled in Matrix26 configuration.");
        }
    }

    private boolean allowlisted(String code) {
        if (code == null) {
            return false;
        }
        return properties.getAllowedInstanceCodes().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(code));
    }

    private boolean protectedCode(String code) {
        if (code == null) {
            return false;
        }
        String normalized = code.toLowerCase(Locale.ROOT);
        return properties.getProtectedInstanceCodes().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals);
    }

    private String publicId() {
        return "PUR-" + LocalDateTime.now().toString().replaceAll("[-:T.]", "").substring(0, 14)
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String safeActor(String actor) {
        return actor == null || actor.isBlank() ? "matrix26-system" : actor;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private record DirectorySnapshot(long sizeBytes, int fileCount) {}
}
