package com.ecoamazonas.eco_agua.platform.control.lifecycle.archive;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupEncryption;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupJob;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupSecurityService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupVerificationState;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission.Matrix26DecommissionJob;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission.Matrix26DecommissionRepository;
import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26RestoreJob;
import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26RestoreService;
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
public class Matrix26ArchiveService {
    private final Matrix26ArchiveRepository repository;
    private final Matrix26ArchiveProperties properties;
    private final Matrix26DecommissionRepository decommissionRepository;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26BackupRepository backupRepository;
    private final Matrix26BackupSecurityService backupSecurityService;
    private final Matrix26RestoreService restoreService;

    public Matrix26ArchiveService(
            Matrix26ArchiveRepository repository,
            Matrix26ArchiveProperties properties,
            Matrix26DecommissionRepository decommissionRepository,
            PlatformBusinessClientRepository clientRepository,
            Matrix26BackupRepository backupRepository,
            Matrix26BackupSecurityService backupSecurityService,
            Matrix26RestoreService restoreService
    ) {
        this.repository = repository;
        this.properties = properties;
        this.decommissionRepository = decommissionRepository;
        this.clientRepository = clientRepository;
        this.backupRepository = backupRepository;
        this.backupSecurityService = backupSecurityService;
        this.restoreService = restoreService;
    }

    public Matrix26ArchiveSummary summary() {
        syncFromDecommissioned("matrix26-system");
        return repository.summary();
    }

    public List<Matrix26ArchiveRecord> records() {
        syncFromDecommissioned("matrix26-system");
        return repository.findAll();
    }

    public Matrix26ArchiveRecord record(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new Matrix26ArchiveException("The archive record does not exist."));
    }

    public List<Matrix26ArchiveEvent> events(long id) {
        return repository.events(id);
    }

    public List<Matrix26ArchiveRestoreLink> restoreLinks(long id) {
        return repository.restoreLinks(id);
    }

    public List<Matrix26ArchiveRestoreLink> recentRestoreLinks() {
        return repository.recentRestoreLinks();
    }

    public List<Matrix26ArchiveRecord> refresh(String actor) {
        syncFromDecommissioned(safeActor(actor));
        return repository.findAll();
    }

    public Matrix26ArchiveRecord verify(long archiveRecordId, String actor) {
        Matrix26ArchiveRecord record = record(archiveRecordId);
        validateEnabled(record.instanceCode());
        Matrix26BackupEncryption verification = backupSecurityService.verifyEncryptedBackup(
                record.finalBackupJobId(), safeActor(actor)
        );
        if (verification == null
                || verification.verificationStatus() != Matrix26BackupVerificationState.VERIFIED
                || !verification.protectedFlag()) {
            Matrix26ArchiveRecord failed = withVerification(record, "BLOCKED", "FINAL_BACKUP_NOT_VERIFIED",
                    "The final archive could not be reverified as encrypted, protected, and VERIFIED.");
            repository.updateRecord(failed);
            repository.addEvent(record.id(), "FINAL_ARCHIVE_VERIFY", "FAILED", safeActor(actor), failed.lastError());
            throw new Matrix26ArchiveException(failed.lastError());
        }
        Matrix26ArchiveRecord verified = withVerification(record, "READY", retentionStatus(record.retentionUntil()), null);
        repository.updateRecord(verified);
        repository.addEvent(record.id(), "FINAL_ARCHIVE_VERIFY", "COMPLETED", safeActor(actor),
                "Final archive " + record.finalBackupPublicId() + " remains encrypted, deletion-protected, and VERIFIED.");
        return record(record.id());
    }

    public Matrix26RestoreJob restoreAsClone(long archiveRecordId, boolean startAfterRestore, String confirmation, String actor) {
        Matrix26ArchiveRecord record = verify(archiveRecordId, actor);
        validateRestorable(record);
        String expected = "RESTORE ARCHIVE " + properties.getCloneInstanceCode();
        if (!expected.equals(confirmation == null ? "" : confirmation.trim())) {
            throw new Matrix26ArchiveException("Type exactly: " + expected);
        }
        Matrix26RestoreJob restore = restoreService.restoreArchivedClone(
                record.finalBackupJobId(),
                properties.getCloneInstanceCode(),
                properties.getCloneInstanceName(),
                properties.getCloneDatabaseName(),
                properties.getCloneRuntimeProfile(),
                properties.getCloneRuntimePort(),
                properties.getClonePublicUrl(),
                startAfterRestore,
                "RESTORE " + properties.getCloneInstanceCode(),
                safeActor(actor)
        );
        repository.insertRestoreLink(
                record.id(),
                restore.id(),
                restore.publicId(),
                restore.targetInstanceCode(),
                restore.targetDatabaseName(),
                restore.targetRuntimeProfile(),
                restore.targetRuntimePort(),
                restore.completed() ? "COMPLETED" : restore.status().name(),
                safeActor(actor)
        );
        repository.addEvent(record.id(), "ARCHIVE_RESTORED_AS_CLONE", "COMPLETED", safeActor(actor),
                "Final archive restored as isolated clone " + restore.targetInstanceCode()
                        + " on port " + restore.targetRuntimePort() + ". The original remains DECOMMISSIONED.");
        return restore;
    }

    private void syncFromDecommissioned(String actor) {
        if (!properties.isEnabled()) {
            return;
        }
        for (Matrix26DecommissionJob job : decommissionRepository.findDecommissionedJobs()) {
            if (job.finalBackupJobId() == null || !allowlisted(job.instanceCode())) {
                continue;
            }
            repository.findByDecommissionJob(job.id()).ifPresentOrElse(
                    existing -> repository.updateRecord(buildRecord(existing.id(), existing.publicId(), job)),
                    () -> {
                        Matrix26ArchiveRecord record = buildRecord(null, publicId(), job);
                        long id = repository.insertRecord(record);
                        repository.addEvent(id, "ARCHIVE_RECORD_CREATED", "COMPLETED", actor,
                                "Historical archive registered for decommission job " + job.publicId() + ".");
                    }
            );
        }
    }

    private Matrix26ArchiveRecord buildRecord(Long id, String publicId, Matrix26DecommissionJob job) {
        PlatformBusinessClient instance = clientRepository.findById(job.instanceId())
                .orElseThrow(() -> new Matrix26ArchiveException("The archived instance registry entry is missing."));
        Matrix26BackupJob backup = backupRepository.findById(job.finalBackupJobId())
                .orElseThrow(() -> new Matrix26ArchiveException("The final backup metadata is missing."));
        Matrix26BackupEncryption encryption = backupSecurityService.metadata(backup.id());
        boolean ready = "DECOMMISSIONED".equalsIgnoreCase(instance.getStatus())
                && encryption != null
                && encryption.encrypted()
                && encryption.protectedFlag()
                && encryption.verificationStatus() == Matrix26BackupVerificationState.VERIFIED;
        int runtimeFiles = countFiles(Path.of("runtime-clients").resolve(nullToBlank(instance.getRuntimeProfile())));
        int dataFiles = countFiles(Path.of("runtime-data").resolve(nullToBlank(instance.getCode())));
        String summary = "Runtime profile: " + nullToBlank(instance.getRuntimeProfile())
                + " | Database: " + nullToBlank(instance.getDatabaseName())
                + " | Port: " + nullToBlank(instance.getRuntimePort())
                + " | Runtime files: " + runtimeFiles
                + " | Data files: " + dataFiles
                + " | Backup: " + backup.publicId();
        LocalDateTime now = LocalDateTime.now();
        return new Matrix26ArchiveRecord(
                id,
                publicId,
                job.id(),
                job.publicId(),
                instance.getId(),
                instance.getCode(),
                instance.getBusinessName(),
                instance.getStatus(),
                backup.id(),
                backup.publicId(),
                encryption == null ? job.finalBackupSha256() : encryption.packageSha256(),
                encryption == null ? job.finalBackupKeyId() : encryption.keyId(),
                encryption == null ? job.finalBackupVerifiedAt() : encryption.verifiedAt(),
                job.retentionUntil(),
                ready ? "READY" : "BLOCKED",
                retentionStatus(job.retentionUntil()),
                runtimeFiles,
                dataFiles,
                summary,
                id == null ? now : null,
                now,
                ready ? now : null,
                ready ? null : "The final archive must be DECOMMISSIONED, encrypted, deletion-protected, and VERIFIED."
        );
    }

    private Matrix26ArchiveRecord withVerification(Matrix26ArchiveRecord record, String status, String retentionStatus, String error) {
        return new Matrix26ArchiveRecord(
                record.id(), record.publicId(), record.decommissionJobId(), record.decommissionPublicId(),
                record.instanceId(), record.instanceCode(), record.instanceName(), record.instanceStatus(),
                record.finalBackupJobId(), record.finalBackupPublicId(), record.finalBackupSha256(),
                record.finalBackupKeyId(), record.finalBackupVerifiedAt(), record.retentionUntil(), status,
                retentionStatus, record.runtimeFileCount(), record.dataFileCount(), record.inventorySummary(),
                record.createdAt(), LocalDateTime.now(), error == null ? LocalDateTime.now() : record.lastVerifiedAt(), error
        );
    }

    private void validateRestorable(Matrix26ArchiveRecord record) {
        validateEnabled(record.instanceCode());
        if (!record.restorableAsClone()) {
            throw new Matrix26ArchiveException("Only READY archives for DECOMMISSIONED instances can be restored as isolated clones.");
        }
        if (!clientRepository.findById(record.instanceId()).map(client -> "DECOMMISSIONED".equalsIgnoreCase(client.getStatus())).orElse(false)) {
            throw new Matrix26ArchiveException("The original instance must remain DECOMMISSIONED before archive restoration.");
        }
    }

    private void validateEnabled(String instanceCode) {
        if (!properties.isEnabled()) {
            throw new Matrix26ArchiveException("Archive Manager is disabled by configuration.");
        }
        if (!allowlisted(instanceCode)) {
            throw new Matrix26ArchiveException("The instance is outside the archive laboratory allowlist.");
        }
    }

    private boolean allowlisted(String instanceCode) {
        String code = instanceCode == null ? "" : instanceCode.toLowerCase(Locale.ROOT);
        return properties.getAllowedInstanceCodes().stream()
                .filter(value -> value != null)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(code::equals);
    }

    private int countFiles(Path directory) {
        if (directory == null || directory.toString().isBlank() || !Files.isDirectory(directory)) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(directory)) {
            long count = stream.filter(Files::isRegularFile).count();
            return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        } catch (IOException ex) {
            return 0;
        }
    }

    private String retentionStatus(LocalDateTime retentionUntil) {
        if (retentionUntil == null) {
            return "UNKNOWN";
        }
        return retentionUntil.isAfter(LocalDateTime.now()) ? "RETENTION_ACTIVE" : "RETENTION_EXPIRED";
    }

    private String publicId() {
        return "ARC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private String safeActor(String actor) {
        String value = actor == null || actor.isBlank() ? "matrix26-system" : actor.trim();
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private String nullToBlank(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
