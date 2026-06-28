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
                .orElseThrow(() -> new Matrix26PurgeException("The purge dry run plan does not exist."));
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
        repository.addEvent(planId, "PURGE_DRY_RUN_REFRESH_REQUESTED", "COMPLETED", safeActor(actor),
                "Dry run refresh requested. Previous evidence remains stored; current run will be shown on the detail page.");
        return evaluate(planId, safeActor(actor));
    }

    public String report(long planId) {
        Matrix26PurgePlan plan = plan(planId);
        StringBuilder builder = new StringBuilder();
        builder.append("Matrix26 Purge Manager - Dry Run Report\n");
        builder.append("Plan: ").append(plan.publicId()).append('\n');
        builder.append("Instance: ").append(plan.instanceName()).append(" (").append(plan.instanceCode()).append(")\n");
        builder.append("Status: ").append(plan.status()).append('\n');
        builder.append("Eligible for future purge: ").append(plan.eligibleForFuturePurge()).append('\n');
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
                    .append(" | ").append(nullToBlank(item.detail()))
                    .append('\n');
        }
        builder.append("\nDeleted resources in Phase 3H.1: 0\n");
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
                    properties.isEnabled(), "Only non-destructive classification is available in Phase 3H.1.");
            check(planId, runNumber, "ALLOWLIST", "Instance is allowlisted for dry run",
                    allowlisted(instance.getCode()), "Allowed instance codes: " + properties.getAllowedInstanceCodes());
            check(planId, runNumber, "PROTECTED_INSTANCE", "Instance is not protected",
                    !instance.isProtectedInstance() && !protectedCode(instance.getCode()),
                    "Protected runtime clients 8081, 8082, 8084 and Matrix26 8091 are blocked.");
            check(planId, runNumber, "DECOMMISSIONED", "Original instance is decommissioned",
                    "DECOMMISSIONED".equalsIgnoreCase(instance.getStatus()),
                    "Current instance status: " + instance.getStatus());
            check(planId, runNumber, "ARCHIVE_READY", "Final archive is ready",
                    "READY".equalsIgnoreCase(archive.archiveStatus()),
                    "Current archive status: " + archive.archiveStatus());

            Matrix26BackupEncryption encryption = plan.finalBackupJobId() == null
                    ? null
                    : backupSecurityService.metadata(plan.finalBackupJobId());
            boolean finalBackupOk = encryption != null
                    && encryption.encrypted()
                    && encryption.retentionClass() == Matrix26BackupRetentionClass.FINAL
                    && encryption.protectedFlag()
                    && encryption.verificationStatus() == Matrix26BackupVerificationState.VERIFIED;
            check(planId, runNumber, "FINAL_BACKUP", "Final backup is encrypted, FINAL, protected, and VERIFIED",
                    finalBackupOk,
                    encryption == null ? "No encrypted backup metadata found." : "Package SHA-256: " + nullToBlank(encryption.packageSha256()));

            boolean retentionExpired = archive.retentionUntil() != null && !archive.retentionUntil().isAfter(LocalDateTime.now());
            boolean retentionAllowed = !properties.isRequireRetentionExpired() || retentionExpired;
            check(planId, runNumber, "RETENTION", "Retention policy does not allow operational purge yet unless configured",
                    retentionAllowed,
                    archive.retentionUntil() == null
                            ? "No retention date found."
                            : "Retention until: " + archive.retentionUntil() + ". Expired: " + retentionExpired);

            boolean portAvailable = instance.getRuntimePort() == null || isPortAvailable(instance.getRuntimePort());
            check(planId, runNumber, "ORIGINAL_PORT", "Original runtime port is free",
                    portAvailable,
                    instance.getRuntimePort() == null ? "No runtime port configured." : "Port " + instance.getRuntimePort());
            check(planId, runNumber, "NO_ACTIVE_OPERATIONS", "No active backup or restore blocks the dry run",
                    !repository.hasActiveBackupOrRestore(instance.getId(), instance.getCode()),
                    "Active backup, clone restore, and in-place restore states are checked in Matrix26 metadata.");
            check(planId, runNumber, "SCHEDULES_DISABLED", "Backup schedules are disabled",
                    !repository.hasEnabledSchedules(instance.getId()),
                    "Total schedules registered: " + repository.totalSchedules(instance.getId()));

            int cloneCount = repository.associatedCloneCount(archive.id());
            check(planId, runNumber, "ARCHIVE_CLONES", "Archive restore clones require review before real purge",
                    true,
                    cloneCount == 0 ? "No clone restore links were found." : cloneCount + " clone restore link(s) are associated with this archive.");

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
                "Future operational purge would drop only this archived laboratory schema after separate confirmations. Phase 3H.1 does not execute a schema removal command.", dispositions);
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
                "Future operational purge would remove this archived laboratory directory. Phase 3H.1 only inventories it.", dispositions);
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
                "Backups stay preserved during Phase 3H.1. A future phase must protect the FINAL archive before considering non-final backups.", dispositions);
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
                null, null, "The historical instance row remains as audit evidence in this dry run.", dispositions);
        item(planId, runNumber, "DECOMMISSION_RECORDS", archive.decommissionPublicId(), "matrix26_decommission_*", Matrix26PurgeDisposition.WOULD_KEEP,
                null, null, "Decommission records stay available for governance and future audit.", dispositions);
        item(planId, runNumber, "ARCHIVE_RECORDS", archive.publicId(), "matrix26_archive_*", Matrix26PurgeDisposition.WOULD_KEEP,
                null, cloneCount, "Archive records and restore links stay available; clone restores are not confused with the original instance.", dispositions);
        item(planId, runNumber, "SCHEDULES", instance.getCode(), "matrix26_backup_schedule", Matrix26PurgeDisposition.WOULD_KEEP,
                null, repository.totalSchedules(instance.getId()), "Disabled schedule metadata stays preserved in this dry run.", dispositions);
        if (cloneCount > 0) {
            item(planId, runNumber, "ARCHIVE_CLONE_LINKS", archive.publicId(), "matrix26_archive_restore_link", Matrix26PurgeDisposition.REQUIRES_REVIEW,
                    null, cloneCount, "Associated clone restore links exist. A real purge must confirm whether clones are still needed.", dispositions);
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
