package com.ecoamazonas.eco_agua.platform.control.purge;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupEncryption;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupJob;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupRetentionClass;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupSecurityService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupVerificationState;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.archive.Matrix26ArchiveRecord;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.archive.Matrix26ArchiveRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ArchiveDestructionService {
    private final Matrix26ArchiveDestructionRepository repository;
    private final Matrix26PurgeProperties properties;
    private final Matrix26ArchiveRepository archiveRepository;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26BackupRepository backupRepository;
    private final Matrix26BackupSecurityService backupSecurityService;
    private final Matrix26HistoricalArchiveExecutor historicalArchiveExecutor;

    public Matrix26ArchiveDestructionService(
            Matrix26ArchiveDestructionRepository repository,
            Matrix26PurgeProperties properties,
            Matrix26ArchiveRepository archiveRepository,
            PlatformBusinessClientRepository clientRepository,
            Matrix26BackupRepository backupRepository,
            Matrix26BackupSecurityService backupSecurityService,
            Matrix26HistoricalArchiveExecutor historicalArchiveExecutor
    ) {
        this.repository = repository;
        this.properties = properties;
        this.archiveRepository = archiveRepository;
        this.clientRepository = clientRepository;
        this.backupRepository = backupRepository;
        this.backupSecurityService = backupSecurityService;
        this.historicalArchiveExecutor = historicalArchiveExecutor;
    }

    public Matrix26ArchiveDestructionSummary summary() {
        return repository.summary();
    }

    public List<Matrix26ArchiveDestructionCandidate> candidates() {
        return archiveRepository.findAll().stream()
                .map(record -> new Matrix26ArchiveDestructionCandidate(
                        record.id(),
                        record.publicId(),
                        record.instanceCode(),
                        record.instanceName(),
                        record.archiveStatus(),
                        record.instanceStatus(),
                        record.finalBackupPublicId(),
                        record.retentionUntil(),
                        record.retentionStatus(),
                        repository.associatedCloneCount(record.id()),
                        allowlisted(record.instanceCode()) && !protectedCode(record.instanceCode())
                ))
                .toList();
    }

    public List<Matrix26ArchiveDestructionPlan> recentPlans() {
        return repository.recentPlans();
    }

    public Matrix26ArchiveDestructionPlan plan(long id) {
        return repository.findPlan(id)
                .orElseThrow(() -> new Matrix26PurgeException("The archive destruction plan does not exist."));
    }

    public List<Matrix26ArchiveDestructionItem> items(long planId) {
        return repository.items(planId);
    }

    public List<Matrix26ArchiveDestructionCheck> checks(long planId) {
        return repository.checks(planId);
    }

    public List<Matrix26ArchiveDestructionEvent> events(long planId) {
        return repository.events(planId);
    }

    public Matrix26ArchiveDestructionPlan prepare(long archiveRecordId, String reason, String confirmation, String actor) {
        validateEnabled();
        Matrix26ArchiveRecord archive = archiveRepository.findById(archiveRecordId)
                .orElseThrow(() -> new Matrix26PurgeException("The selected final archive does not exist."));
        String expected = "PREPARE ARCHIVE DESTRUCTION " + archive.instanceCode();
        if (!expected.equals(confirmation == null ? "" : confirmation.trim())) {
            throw new Matrix26PurgeException("Type exactly: " + expected);
        }
        if (reason == null || reason.trim().length() < properties.getMinimumReasonLength()) {
            throw new Matrix26PurgeException("The archive destruction reason must contain at least "
                    + properties.getMinimumReasonLength() + " characters.");
        }
        PlatformBusinessClient instance = clientRepository.findById(archive.instanceId())
                .orElseThrow(() -> new Matrix26PurgeException("The archived instance registry entry is missing."));
        Matrix26BackupJob backup = archive.finalBackupJobId() == null
                ? null
                : backupRepository.findById(archive.finalBackupJobId()).orElse(null);
        Matrix26BackupEncryption encryption = finalBackupMetadata(archive.finalBackupJobId());
        Matrix26ArchiveDestructionPlan draft = new Matrix26ArchiveDestructionPlan(
                null,
                publicId(),
                archive.id(),
                archive.publicId(),
                instance.getId(),
                instance.getCode(),
                instance.getBusinessName(),
                archive.finalBackupJobId(),
                archive.finalBackupPublicId(),
                archive.finalBackupSha256(),
                backup == null ? null : backup.backupDirectory(),
                encryption == null ? null : encryption.packagePath(),
                archive.retentionUntil(),
                archive.retentionStatus(),
                Matrix26ArchiveDestructionStatus.DRAFT,
                1,
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
        repository.addEvent(id, "ARCHIVE_DESTRUCTION_REQUESTED", "COMPLETED", safeActor(actor),
                "Archive destruction planner created for " + archive.instanceCode() + ". Deleted resources: 0.");
        return evaluate(id, safeActor(actor));
    }

    public Matrix26ArchiveDestructionPlan refresh(long planId, String actor) {
        validateEnabled();
        Matrix26ArchiveDestructionPlan plan = plan(planId);
        if (plan.status() == Matrix26ArchiveDestructionStatus.DESTROYING
                || plan.status() == Matrix26ArchiveDestructionStatus.DESTROYED
                || plan.status() == Matrix26ArchiveDestructionStatus.PARTIALLY_DESTROYED) {
            throw new Matrix26PurgeException("This archive destruction plan is already executing or completed and cannot be refreshed.");
        }
        repository.addEvent(planId, "ARCHIVE_DESTRUCTION_REFRESH_REQUESTED", "COMPLETED", safeActor(actor),
                "Archive destruction planner refresh requested. No archive package was destroyed.");
        return evaluate(planId, safeActor(actor));
    }

    public Matrix26ArchiveDestructionPlan approveDestruction(long planId, String approvalConfirmation, String actor) {
        validateDestructionExecutionEnabled();
        Matrix26ArchiveDestructionPlan plan = plan(planId);
        if (plan.status() != Matrix26ArchiveDestructionStatus.READY_FOR_REVIEW) {
            throw new Matrix26PurgeException("Only a READY_FOR_REVIEW archive destruction plan can be approved.");
        }
        String expected = "APPROVE ARCHIVE DESTRUCTION " + plan.instanceCode();
        if (!expected.equals(approvalConfirmation == null ? "" : approvalConfirmation.trim())) {
            throw new Matrix26PurgeException("Type exactly: " + expected);
        }
        assertDestructionSafety(plan);
        repository.prepareExecutionItems(plan.id(), currentRun(plan));
        repository.updatePlanStatus(plan.id(), Matrix26ArchiveDestructionStatus.APPROVED_FOR_DESTRUCTION, null);
        repository.addEvent(plan.id(), "ARCHIVE_DESTRUCTION_APPROVED", Matrix26ArchiveDestructionStatus.APPROVED_FOR_DESTRUCTION.name(), safeActor(actor),
                "Archive destruction was approved after explicit confirmation. No archive package was destroyed yet.");
        return plan(plan.id());
    }

    public Matrix26ArchiveDestructionPlan executeDestruction(
            long planId,
            String destroyConfirmation,
            String irreversibleConfirmation,
            String actor
    ) {
        validateDestructionExecutionEnabled();
        Matrix26ArchiveDestructionPlan plan = plan(planId);
        if (plan.status() != Matrix26ArchiveDestructionStatus.APPROVED_FOR_DESTRUCTION
                && plan.status() != Matrix26ArchiveDestructionStatus.PARTIALLY_DESTROYED) {
            throw new Matrix26PurgeException("Only an APPROVED_FOR_DESTRUCTION plan can destroy an archive package.");
        }
        String expectedDestroy = "DESTROY ARCHIVE PACKAGE " + plan.instanceCode();
        if (!expectedDestroy.equals(destroyConfirmation == null ? "" : destroyConfirmation.trim())) {
            throw new Matrix26PurgeException("Type exactly: " + expectedDestroy);
        }
        String expectedIrreversible = "I UNDERSTAND THIS ARCHIVE CANNOT BE RESTORED";
        if (!expectedIrreversible.equals(irreversibleConfirmation == null ? "" : irreversibleConfirmation.trim())) {
            throw new Matrix26PurgeException("Type exactly: " + expectedIrreversible);
        }
        assertDestructionSafety(plan);
        repository.updatePlanStatus(plan.id(), Matrix26ArchiveDestructionStatus.DESTROYING, null);
        repository.addEvent(plan.id(), "ARCHIVE_DESTRUCTION_STARTED", Matrix26ArchiveDestructionStatus.DESTROYING.name(), safeActor(actor),
                "Final archive package destruction started after the required confirmations.");

        List<String> failures = new ArrayList<>();
        for (Matrix26ArchiveDestructionItem item : repository.items(plan.id())) {
            if (item.disposition() != Matrix26PurgeDisposition.WOULD_DELETE) {
                continue;
            }
            try {
                Matrix26HistoricalArchiveExecutor.ExecutionResult result = historicalArchiveExecutor.destroy(plan, item);
                repository.updateItemExecutionStatus(item.id(), result.status(), result.detail());
                repository.addEvent(plan.id(), "ARCHIVE_DESTRUCTION_ITEM_" + result.status(), result.status(), safeActor(actor),
                        item.resourceType() + ": " + result.detail());
            } catch (RuntimeException ex) {
                failures.add(item.resourceType() + ": " + ex.getMessage());
                repository.updateItemExecutionStatus(item.id(), "FAILED", ex.getMessage());
                repository.addEvent(plan.id(), "ARCHIVE_DESTRUCTION_ITEM_FAILED", "FAILED", safeActor(actor),
                        item.resourceType() + " failed: " + ex.getMessage());
            }
        }

        if (failures.isEmpty()) {
            repository.markArchivePackageDestroyed(plan.archiveRecordId(), safeActor(actor));
            archiveRepository.addEvent(plan.archiveRecordId(), "ARCHIVE_PACKAGE_DESTROYED", "COMPLETED", safeActor(actor),
                    "Final archive package was destroyed by Matrix26 Archive Destruction Executor. Central audit metadata was preserved.");
            repository.updatePlanStatus(plan.id(), Matrix26ArchiveDestructionStatus.DESTROYED, null);
            repository.addEvent(plan.id(), "ARCHIVE_DESTRUCTION_COMPLETED", Matrix26ArchiveDestructionStatus.DESTROYED.name(), safeActor(actor),
                    "Archive package destruction completed. Destroyed items: " + repository.destroyedItemCount(plan.id())
                            + ". Central archive, backup, purge and decommission metadata were preserved.");
        } else {
            String error = String.join(" | ", failures);
            repository.updatePlanStatus(plan.id(), Matrix26ArchiveDestructionStatus.PARTIALLY_DESTROYED, error);
            repository.addEvent(plan.id(), "ARCHIVE_DESTRUCTION_PARTIAL", Matrix26ArchiveDestructionStatus.PARTIALLY_DESTROYED.name(), safeActor(actor),
                    "Archive package destruction needs manual review: " + error);
        }
        return plan(plan.id());
    }

    public String report(long planId) {
        Matrix26ArchiveDestructionPlan plan = plan(planId);
        StringBuilder builder = new StringBuilder();
        builder.append("Matrix26 Archive Destruction Executor - Governance Report\n");
        builder.append("Plan: ").append(plan.publicId()).append('\n');
        builder.append("Instance: ").append(plan.instanceName()).append(" (").append(plan.instanceCode()).append(")\n");
        builder.append("Archive: ").append(plan.archivePublicId()).append('\n');
        builder.append("Final backup: ").append(plan.finalBackupPublicId()).append('\n');
        builder.append("Status: ").append(plan.status()).append('\n');
        builder.append("Reason: ").append(plan.reason()).append("\n\n");
        builder.append("Checks\n");
        for (Matrix26ArchiveDestructionCheck check : checks(planId)) {
            builder.append("- [").append(check.status()).append("] ")
                    .append(check.label()).append(": ").append(nullToBlank(check.detail())).append('\n');
        }
        builder.append("\nItems\n");
        for (Matrix26ArchiveDestructionItem item : items(planId)) {
            builder.append("- [").append(item.disposition()).append("] ")
                    .append(item.resourceType()).append(" | ").append(nullToBlank(item.resourceName()))
                    .append(" | ").append(nullToBlank(item.resourcePath()))
                    .append(" | execution=").append(item.executionStatusLabel())
                    .append(" | ").append(nullToBlank(item.executionDetail()))
                    .append('\n');
        }
        builder.append("\nDestroyed resources in Phase 3H.4: ").append(repository.destroyedItemCount(planId)).append('\n');
        builder.append("Central metadata preservation: archive records, backup metadata, purge records, decommission records and clone links are not deleted by this executor.\n");
        return builder.toString();
    }

    private Matrix26ArchiveDestructionPlan evaluate(long planId, String actor) {
        Matrix26ArchiveDestructionPlan plan = plan(planId);
        int runNumber = plan.runNumber() == null ? 1 : plan.runNumber() + 1;
        repository.markAnalyzing(planId, runNumber);
        List<Matrix26PurgeDisposition> dispositions = new ArrayList<>();
        try {
            Matrix26ArchiveRecord archive = archiveRepository.findById(plan.archiveRecordId())
                    .orElseThrow(() -> new Matrix26PurgeException("The archive record no longer exists."));
            PlatformBusinessClient instance = clientRepository.findById(plan.instanceId())
                    .orElseThrow(() -> new Matrix26PurgeException("The archived instance registry entry is missing."));
            Matrix26BackupJob backup = archive.finalBackupJobId() == null
                    ? null
                    : backupRepository.findById(archive.finalBackupJobId()).orElse(null);
            Matrix26BackupEncryption encryption = finalBackupMetadata(archive.finalBackupJobId());
            boolean finalBackupOk = finalBackupOk(encryption);
            boolean retentionExpired = archive.retentionUntil() != null && !archive.retentionUntil().isAfter(LocalDateTime.now());
            boolean retentionAllowsDestruction = !properties.isArchiveDestructionRequireRetentionExpired() || retentionExpired;
            int cloneCount = repository.associatedCloneCount(archive.id());

            check(planId, runNumber, "FEATURE_ENABLED", "Archive destruction planner is enabled",
                    properties.isArchiveDestructionEnabled(), "This phase is read-only and does not remove files.");
            check(planId, runNumber, "EXECUTION_GATE_RECORDED", "Archive package removal requires the separate execution gate",
                    true, "Execution flag currently enabled: " + properties.isArchiveDestructionExecutionEnabled()
                            + ". Destruction still requires approval and two irreversible confirmations.");
            check(planId, runNumber, "ALLOWLIST", "Instance is allowlisted for archive destruction planning",
                    allowlisted(instance.getCode()), "Allowed instance codes: " + properties.getAllowedInstanceCodes());
            check(planId, runNumber, "PROTECTED_INSTANCE", "Instance is not protected",
                    !instance.isProtectedInstance() && !protectedCode(instance.getCode()),
                    "Production clients, Matrix26 Control Center, and archive clones are protected.");
            check(planId, runNumber, "ORIGINAL_PURGED", "Original instance was operationally purged",
                    "PURGED".equalsIgnoreCase(instance.getStatus()), "Current instance status: " + instance.getStatus());
            check(planId, runNumber, "ARCHIVE_READY", "Archive record remains ready",
                    "READY".equalsIgnoreCase(archive.archiveStatus()), "Current archive status: " + archive.archiveStatus());
            check(planId, runNumber, "FINAL_BACKUP_METADATA", "Final backup remains encrypted, FINAL, protected, and VERIFIED",
                    finalBackupOk,
                    encryption == null ? "No encrypted backup metadata found." : "Package SHA-256: " + nullToBlank(encryption.packageSha256()));
            check(planId, runNumber, "RETENTION_EXPIRED", "Retention period has expired",
                    retentionAllowsDestruction,
                    archive.retentionUntil() == null ? "No retention date found." : "Retention until: " + archive.retentionUntil() + ". Expired: " + retentionExpired);
            check(planId, runNumber, "NO_ACTIVE_OPERATIONS", "No active backup or restore references the archive",
                    !repository.hasActiveBackupOrRestore(instance.getId(), instance.getCode())
                            && !repository.hasActiveArchiveCloneRestore(archive.id()),
                    "Active backup, restore, in-place restore and clone restore jobs block archive destruction planning.");
            check(planId, runNumber, "NO_CLONE_DEPENDENCY", "No archive clone dependency exists",
                    cloneCount == 0,
                    cloneCount == 0 ? "No clone restore links were found." : cloneCount + " clone restore link(s) still reference this final archive.");

            classifyFinalPackage(planId, runNumber, encryption, retentionAllowsDestruction, cloneCount, finalBackupOk, dispositions);
            classifyBackupDirectory(planId, runNumber, backup, encryption, retentionAllowsDestruction, cloneCount, finalBackupOk, dispositions);
            classifyPublicMetadata(planId, runNumber, backup, dispositions);
            classifyCentralMetadata(planId, runNumber, archive, instance, cloneCount, dispositions);

            int blockers = countChecks(planId);
            Matrix26ArchiveDestructionRepository.Counts counts = counts(dispositions, blockers);
            Matrix26ArchiveDestructionStatus status;
            String error = null;
            if (blockers > 0) {
                status = Matrix26ArchiveDestructionStatus.BLOCKED;
                error = "The archive destruction planner found blockers. Deleted resources: 0.";
            } else {
                status = Matrix26ArchiveDestructionStatus.READY_FOR_REVIEW;
            }
            repository.completePlan(planId, status, counts, error);
            repository.addEvent(planId, "ARCHIVE_DESTRUCTION_ANALYZED", status.name(), actor,
                    "Archive destruction planner completed with " + blockers + " blocker(s). Deleted resources: 0.");
            return plan(planId);
        } catch (RuntimeException ex) {
            repository.failPlan(planId, ex.getMessage());
            repository.addEvent(planId, "ARCHIVE_DESTRUCTION_FAILED", "FAILED", actor, ex.getMessage());
            throw ex;
        }
    }

    private void classifyFinalPackage(
            long planId,
            int runNumber,
            Matrix26BackupEncryption encryption,
            boolean retentionAllowsDestruction,
            int cloneCount,
            boolean finalBackupOk,
            List<Matrix26PurgeDisposition> dispositions
    ) {
        if (encryption == null || encryption.packagePath() == null || encryption.packagePath().isBlank()) {
            item(planId, runNumber, "FINAL_ARCHIVE_PACKAGE", "Missing package", "", Matrix26PurgeDisposition.BLOCKED,
                    null, null, "Final archive package metadata is missing.", dispositions);
            return;
        }
        Path packagePath = Path.of(encryption.packagePath()).toAbsolutePath().normalize();
        long size = fileSize(packagePath);
        Matrix26PurgeDisposition disposition;
        String detail;
        if (!Files.exists(packagePath)) {
            disposition = Matrix26PurgeDisposition.BLOCKED;
            detail = "The encrypted package is missing from disk while metadata still references it.";
        } else if (!finalBackupOk) {
            disposition = Matrix26PurgeDisposition.BLOCKED;
            detail = "Final backup metadata is incomplete or no longer VERIFIED.";
        } else if (!retentionAllowsDestruction) {
            disposition = Matrix26PurgeDisposition.PROTECTED;
            detail = "Retention is still active; the final archive package remains protected.";
        } else if (cloneCount > 0) {
            disposition = Matrix26PurgeDisposition.REQUIRES_REVIEW;
            detail = "Clone restore links still exist; package destruction requires separate governance review.";
        } else {
            disposition = Matrix26PurgeDisposition.WOULD_DELETE;
            detail = "Future archive destruction could remove this encrypted package after an additional execution phase.";
        }
        item(planId, runNumber, "FINAL_ARCHIVE_PACKAGE", "package.m26backup", packagePath.toString(), disposition,
                size, Files.exists(packagePath) ? 1 : 0, detail, dispositions);
    }

    private void classifyBackupDirectory(
            long planId,
            int runNumber,
            Matrix26BackupJob backup,
            Matrix26BackupEncryption encryption,
            boolean retentionAllowsDestruction,
            int cloneCount,
            boolean finalBackupOk,
            List<Matrix26PurgeDisposition> dispositions
    ) {
        String directory = backup == null ? null : backup.backupDirectory();
        if ((directory == null || directory.isBlank()) && encryption != null && encryption.packagePath() != null) {
            Path packagePath = Path.of(encryption.packagePath()).toAbsolutePath().normalize();
            directory = packagePath.getParent() == null ? null : packagePath.getParent().toString();
        }
        if (directory == null || directory.isBlank()) {
            item(planId, runNumber, "FINAL_BACKUP_DIRECTORY", "Missing directory", "", Matrix26PurgeDisposition.BLOCKED,
                    null, null, "Backup directory metadata is missing.", dispositions);
            return;
        }
        Path path = Path.of(directory).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            item(planId, runNumber, "FINAL_BACKUP_DIRECTORY", path.getFileName().toString(), path.toString(), Matrix26PurgeDisposition.BLOCKED,
                    0L, 0, "The final backup directory is missing while Matrix26 still references it.", dispositions);
            return;
        }
        DirectorySnapshot snapshot = snapshot(path);
        Matrix26PurgeDisposition disposition = retentionAllowsDestruction && cloneCount == 0 && finalBackupOk
                ? Matrix26PurgeDisposition.WOULD_DELETE
                : Matrix26PurgeDisposition.PROTECTED;
        String detail = disposition == Matrix26PurgeDisposition.WOULD_DELETE
                ? "Future archive destruction could remove the final backup directory after a separate execution phase."
                : "The final backup directory remains protected while retention, metadata, or clone checks are blocking.";
        item(planId, runNumber, "FINAL_BACKUP_DIRECTORY", path.getFileName().toString(), path.toString(), disposition,
                snapshot.sizeBytes(), snapshot.fileCount(), detail, dispositions);
    }

    private void classifyPublicMetadata(
            long planId,
            int runNumber,
            Matrix26BackupJob backup,
            List<Matrix26PurgeDisposition> dispositions
    ) {
        if (backup == null || backup.backupDirectory() == null || backup.backupDirectory().isBlank()) {
            item(planId, runNumber, "PUBLIC_BACKUP_METADATA", "Backup metadata files", "", Matrix26PurgeDisposition.REQUIRES_REVIEW,
                    null, null, "Backup job directory is unavailable; public metadata files cannot be inventoried.", dispositions);
            return;
        }
        Path directory = Path.of(backup.backupDirectory()).toAbsolutePath().normalize();
        for (String fileName : List.of("public-manifest.json", "checksums.sha256", "backup-report.txt")) {
            Path file = directory.resolve(fileName).normalize();
            if (Files.exists(file)) {
                item(planId, runNumber, "PUBLIC_BACKUP_METADATA", fileName, file.toString(), Matrix26PurgeDisposition.WOULD_KEEP,
                        fileSize(file), 1, "Public metadata remains as human-readable evidence unless a later phase explicitly archives it elsewhere.", dispositions);
            } else {
                item(planId, runNumber, "PUBLIC_BACKUP_METADATA", fileName, file.toString(), Matrix26PurgeDisposition.NOT_FOUND,
                        0L, 0, "The expected public metadata file was not found.", dispositions);
            }
        }
    }

    private void classifyCentralMetadata(
            long planId,
            int runNumber,
            Matrix26ArchiveRecord archive,
            PlatformBusinessClient instance,
            int cloneCount,
            List<Matrix26PurgeDisposition> dispositions
    ) {
        item(planId, runNumber, "ARCHIVE_RECORDS", archive.publicId(), "matrix26_archive_*", Matrix26PurgeDisposition.WOULD_KEEP,
                null, null, "Central archive records stay preserved for audit even if package destruction is approved later.", dispositions);
        item(planId, runNumber, "BACKUP_METADATA", archive.finalBackupPublicId(), "matrix26_backup_*", Matrix26PurgeDisposition.WOULD_KEEP,
                null, null, "Backup metadata stays preserved for traceability and compliance.", dispositions);
        item(planId, runNumber, "PURGE_RECORDS", instance.getCode(), "matrix26_purge_*", Matrix26PurgeDisposition.WOULD_KEEP,
                null, null, "Operational purge plans and events stay preserved.", dispositions);
        if (cloneCount > 0) {
            item(planId, runNumber, "CLONE_DEPENDENCY", archive.publicId(), "matrix26_archive_restore_link", Matrix26PurgeDisposition.REQUIRES_REVIEW,
                    null, cloneCount, "Clone restore links still reference this archive and block final package destruction.", dispositions);
        }
    }

    private void check(long planId, int runNumber, String code, String label, boolean passed, String detail) {
        repository.addCheck(planId, runNumber, code, label, passed ? "PASSED" : "BLOCKED", detail);
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

    private int countChecks(long planId) {
        int blockers = 0;
        for (Matrix26ArchiveDestructionCheck check : repository.checks(planId)) {
            if ("BLOCKED".equalsIgnoreCase(check.status())) {
                blockers++;
            }
        }
        return blockers;
    }

    private Matrix26ArchiveDestructionRepository.Counts counts(List<Matrix26PurgeDisposition> dispositions, int blockers) {
        return new Matrix26ArchiveDestructionRepository.Counts(
                blockers,
                count(dispositions, Matrix26PurgeDisposition.WOULD_DELETE),
                count(dispositions, Matrix26PurgeDisposition.WOULD_KEEP),
                count(dispositions, Matrix26PurgeDisposition.PROTECTED),
                count(dispositions, Matrix26PurgeDisposition.REQUIRES_REVIEW)
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


    private void validateDestructionExecutionEnabled() {
        validateEnabled();
        if (!properties.isArchiveDestructionExecutionEnabled()) {
            throw new Matrix26PurgeException("Archive destruction execution is disabled. Set matrix26.control-center.purge.archive-destruction-execution-enabled=true only for the controlled 3H.4 execution window.");
        }
    }

    private void assertDestructionSafety(Matrix26ArchiveDestructionPlan plan) {
        if (!allowlisted(plan.instanceCode()) || protectedCode(plan.instanceCode())) {
            throw new Matrix26PurgeException("This instance is not allowed for archive package destruction.");
        }
        if (plan.blockers() > 0 || repository.hasBlockedCurrentChecks(plan.id())) {
            throw new Matrix26PurgeException("The current archive destruction plan still has safety blockers.");
        }
        if (repository.pendingDestroyItemCount(plan.id(), currentRun(plan)) <= 0) {
            throw new Matrix26PurgeException("The current archive destruction plan has no WOULD_DELETE archive package item.");
        }
        if (repository.associatedCloneCount(plan.archiveRecordId()) > 0) {
            throw new Matrix26PurgeException("Clone restore links still reference this archive. Remove or resolve clone dependencies before destruction.");
        }
        if (repository.hasActiveBackupOrRestore(plan.instanceId(), plan.instanceCode())
                || repository.hasActiveArchiveCloneRestore(plan.archiveRecordId())) {
            throw new Matrix26PurgeException("Active backup or restore operations still reference this archive.");
        }
        if (properties.isArchiveDestructionRequireRetentionExpired()
                && (plan.retentionUntil() == null || plan.retentionUntil().isAfter(LocalDateTime.now()))) {
            throw new Matrix26PurgeException("Archive retention has not expired yet.");
        }
        Matrix26BackupEncryption encryption = finalBackupMetadata(plan.finalBackupJobId());
        if (!finalBackupOk(encryption)) {
            throw new Matrix26PurgeException("Final backup metadata is no longer encrypted, FINAL, protected, and VERIFIED.");
        }
        if (encryption.packagePath() == null || encryption.packagePath().isBlank()) {
            throw new Matrix26PurgeException("Final archive package path is missing from encrypted backup metadata.");
        }
    }

    private int currentRun(Matrix26ArchiveDestructionPlan plan) {
        return plan.runNumber() == null ? 1 : plan.runNumber();
    }

    private Matrix26BackupEncryption finalBackupMetadata(Long jobId) {
        return jobId == null ? null : backupSecurityService.metadata(jobId);
    }

    private boolean finalBackupOk(Matrix26BackupEncryption encryption) {
        return encryption != null
                && encryption.encrypted()
                && encryption.retentionClass() == Matrix26BackupRetentionClass.FINAL
                && encryption.protectedFlag()
                && encryption.verificationStatus() == Matrix26BackupVerificationState.VERIFIED;
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
                    // Keep the planner non-blocking when a file disappears during inventory.
                }
            }
        } catch (IOException ignored) {
            return new DirectorySnapshot(bytes, files);
        }
        return new DirectorySnapshot(bytes, files);
    }

    private long fileSize(Path path) {
        try {
            return Files.exists(path) ? Files.size(path) : 0L;
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private void validateEnabled() {
        if (!properties.isEnabled() || !properties.isArchiveDestructionEnabled()) {
            throw new Matrix26PurgeException("Archive destruction planner is disabled in Matrix26 configuration.");
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
        return "ADS-" + LocalDateTime.now().toString().replaceAll("[-:T.]", "").substring(0, 14)
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
