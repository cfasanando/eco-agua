package com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLog;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLogRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupEncryption;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupJob;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupRetentionClass;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupSecurityService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupService;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupVerificationState;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsInventoryService;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeInventoryItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26DecommissionService {

    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26DecommissionRepository repository;
    private final Matrix26DecommissionProperties properties;
    private final Matrix26BackupService backupService;
    private final Matrix26BackupSecurityService backupSecurityService;
    private final Matrix26OperationsInventoryService inventoryService;
    private final Matrix26InstanceAuditLogRepository auditRepository;
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public Matrix26DecommissionService(
            PlatformBusinessClientRepository clientRepository,
            Matrix26DecommissionRepository repository,
            Matrix26DecommissionProperties properties,
            Matrix26BackupService backupService,
            Matrix26BackupSecurityService backupSecurityService,
            Matrix26OperationsInventoryService inventoryService,
            Matrix26InstanceAuditLogRepository auditRepository
    ) {
        this.clientRepository = clientRepository;
        this.repository = repository;
        this.properties = properties;
        this.backupService = backupService;
        this.backupSecurityService = backupSecurityService;
        this.inventoryService = inventoryService;
        this.auditRepository = auditRepository;
    }

    public List<Matrix26DecommissionCandidate> candidates() {
        List<Matrix26DecommissionCandidate> result = new ArrayList<>();
        for (PlatformBusinessClient instance : clientRepository.findAllByOrderByBusinessNameAsc()) {
            result.add(candidate(instance));
        }
        return result;
    }

    public List<Matrix26DecommissionJob> recentJobs() {
        return repository.findRecentJobs();
    }

    public List<Matrix26DecommissionJob> decommissionedJobs() {
        return repository.findDecommissionedJobs();
    }

    public Matrix26DecommissionSummary summary() {
        return repository.summary();
    }

    public Matrix26DecommissionJob job(long id) {
        return repository.findJob(id)
                .orElseThrow(() -> new Matrix26DecommissionException("The requested decommission job does not exist."));
    }

    public List<Matrix26DecommissionCheck> checks(long jobId) {
        return repository.findChecks(jobId);
    }

    public List<Matrix26DecommissionEvent> events(long jobId) {
        return repository.findEvents(jobId);
    }

    public List<Matrix26DecommissionScheduleState> scheduleStates(long jobId) {
        return repository.findScheduleStates(jobId);
    }

    public Matrix26DecommissionJob prepare(
            long instanceId,
            String reason,
            String notes,
            int retentionDays,
            String confirmation,
            String actor
    ) {
        ReentrantLock lock = locks.computeIfAbsent(instanceId, ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new Matrix26DecommissionException("Another decommission operation is already running for this instance.");
        }
        try {
            PlatformBusinessClient instance = instance(instanceId);
            validateFeatureEnabled();
            validateAllowed(instance);
            validateConfirmation(confirmation, "PREPARE DECOMMISSION " + instance.getCode());
            String safeReason = validateReason(reason);
            String safeNotes = safeNotes(notes);
            String safeActor = safeActor(actor);
            int safeRetentionDays = validateRetentionDays(retentionDays);
            validateSuspended(instance);
            validateNoActiveDecommission(instance);
            validateOperationalBlockers(instance);
            Matrix26RuntimeInventoryItem runtime = requireStoppedRuntime(instance);

            long jobId = repository.insertJob(
                    publicId(), instance.getId(), instance.getCode(), instance.getBusinessName(),
                    safeReason, safeNotes, safeActor, safeRetentionDays, normalizedStatus(instance)
            );
            repository.markStarted(jobId, Matrix26DecommissionStatus.PRECHECKING);
            pass(jobId, "ALLOWLIST", "Laboratory allowlist",
                    "Instance is explicitly enabled for controlled decommission.");
            pass(jobId, "INSTANCE_STATUS", "Suspended lifecycle state",
                    "Instance status is SUSPENDED.");
            pass(jobId, "RUNTIME_STOPPED", "Runtime and port stopped",
                    "Port " + instance.getRuntimePort() + " is free and no owned runtime process is active.");
            pass(jobId, "OPERATION_CONFLICTS", "Operational conflict gates",
                    "No active backup, restore, runtime, lifecycle, or decommission operation was found.");
            repository.addEvent(jobId, "PRECHECK_COMPLETED", "COMPLETED", safeActor,
                    "The suspended laboratory passed all non-destructive decommission prechecks.");

            try {
                repository.updateStatus(jobId, Matrix26DecommissionStatus.FINAL_BACKUP_RUNNING);
                repository.addEvent(jobId, "FINAL_BACKUP_STARTED", "RUNNING", safeActor,
                        "Creating a new encrypted full backup specifically for final archive retention.");
                Matrix26BackupJob plainBackup = backupService.createManualFullBackup(instanceId, safeActor, true);
                repository.addEvent(jobId, "FULL_BACKUP_COMPLETED", "COMPLETED", safeActor,
                        "Full backup " + plainBackup.publicId() + " completed before encryption.");

                repository.updateStatus(jobId, Matrix26DecommissionStatus.FINAL_BACKUP_VERIFYING);
                backupSecurityService.encryptBackup(plainBackup.id(), Matrix26BackupRetentionClass.FINAL, safeActor);
                Matrix26BackupEncryption encryption = backupSecurityService.verifyEncryptedBackup(plainBackup.id(), safeActor);
                if (encryption == null
                        || encryption.verificationStatus() != Matrix26BackupVerificationState.VERIFIED
                        || encryption.retentionClass() != Matrix26BackupRetentionClass.FINAL
                        || !encryption.protectedFlag()) {
                    throw new Matrix26DecommissionException(
                            "The final archive did not finish as encrypted, verified, protected, and FINAL."
                    );
                }

                Matrix26DecommissionBackupView finalBackup = repository.finalBackup(plainBackup.id());
                if (!finalBackup.finalArchiveVerified()) {
                    throw new Matrix26DecommissionException("The final archive metadata could not be verified.");
                }
                repository.attachFinalBackup(jobId, finalBackup);
                pass(jobId, "FINAL_BACKUP", "Protected final archive",
                        "Backup " + finalBackup.publicId() + " is encrypted, VERIFIED, FINAL, and deletion-protected.");
                repository.addEvent(jobId, "FINAL_BACKUP_VERIFIED", "COMPLETED", safeActor,
                        "Final archive " + finalBackup.publicId() + " passed independent encrypted verification.");

                repository.snapshotSchedules(jobId, instanceId);
                int disabled = repository.disableSchedules(jobId, instanceId, safeActor);
                repository.setDisabledScheduleCount(jobId, disabled);
                pass(jobId, "SCHEDULES_DISABLED", "Scheduled backups disabled",
                        disabled + " enabled schedule(s) were disabled. Existing backups remain preserved.");
                repository.addEvent(jobId, "SCHEDULES_DISABLED", "COMPLETED", safeActor,
                        disabled + " schedule(s) were disabled for the decommissioned lifecycle.");

                LocalDateTime retentionUntil = LocalDateTime.now().plusDays(safeRetentionDays);
                repository.setRetentionUntil(jobId, retentionUntil);
                repository.updateStatus(jobId, Matrix26DecommissionStatus.READY_TO_DECOMMISSION);
                repository.addEvent(jobId, "READY_TO_DECOMMISSION", "COMPLETED", safeActor,
                        "The final archive is protected. A separate confirmation is required to decommission the instance.");
                audit(instance, "DECOMMISSION_PREPARED", safeActor,
                        "Final archive prepared for decommission: " + finalBackup.publicId(),
                        normalizedStatus(instance), normalizedStatus(instance));
                return job(jobId);
            } catch (RuntimeException ex) {
                repository.fail(jobId, Matrix26DecommissionStatus.FAILED, safeMessage(ex));
                repository.addEvent(jobId, "PREPARATION_FAILED", "FAILED", safeActor, safeMessage(ex));
                fail(jobId, "FINAL_BACKUP", "Protected final archive", safeMessage(ex));
                audit(instance, "DECOMMISSION_PREPARATION_FAILED", safeActor,
                        "Decommission preparation failed: " + safeMessage(ex),
                        normalizedStatus(instance), normalizedStatus(instance));
                throw exception("The decommission plan could not be prepared.", ex);
            }
        } finally {
            lock.unlock();
        }
    }

    public Matrix26DecommissionJob execute(long jobId, String confirmation, String actor) {
        Matrix26DecommissionJob initial = job(jobId);
        ReentrantLock lock = locks.computeIfAbsent(initial.instanceId(), ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new Matrix26DecommissionException("Another decommission operation is already running for this instance.");
        }
        try {
            Matrix26DecommissionJob plan = job(jobId);
            PlatformBusinessClient instance = instance(plan.instanceId());
            String safeActor = safeActor(actor);
            validateFeatureEnabled();
            validateAllowed(instance);
            validateConfirmation(confirmation, "DECOMMISSION " + instance.getCode());
            if (!plan.readyToExecute()) {
                throw new Matrix26DecommissionException("Only a READY_TO_DECOMMISSION plan can be executed.");
            }
            validateSuspended(instance);
            if (repository.hasOtherActiveJob(instance.getId(), jobId)) {
                throw new Matrix26DecommissionException("Another decommission plan is active for this instance.");
            }
            validateOperationalBlockers(instance);
            requireStoppedRuntime(instance);
            if (plan.finalBackupJobId() == null) {
                throw new Matrix26DecommissionException("The plan does not contain a final archive backup.");
            }

            repository.updateStatus(jobId, Matrix26DecommissionStatus.DECOMMISSIONING);
            instance.setStatus("DECOMMISSIONING");
            clientRepository.save(instance);
            repository.addEvent(jobId, "DECOMMISSION_STARTED", "RUNNING", safeActor,
                    "Executing the non-destructive operational decommission transition.");

            try {
                Matrix26BackupEncryption verified = backupSecurityService.verifyEncryptedBackup(
                        plan.finalBackupJobId(), safeActor
                );
                Matrix26DecommissionBackupView finalBackup = repository.finalBackup(plan.finalBackupJobId());
                if (verified == null || verified.verificationStatus() != Matrix26BackupVerificationState.VERIFIED
                        || !finalBackup.finalArchiveVerified()) {
                    throw new Matrix26DecommissionException("The final archive no longer passes verification.");
                }
                pass(jobId, "FINAL_BACKUP_REVERIFIED", "Final archive reverified",
                        "Backup " + finalBackup.publicId() + " remains encrypted, protected, and VERIFIED.");

                repository.snapshotSchedules(jobId, instance.getId());
                int additionallyDisabled = repository.disableSchedules(jobId, instance.getId(), safeActor);
                int totalDisabled = value(plan.disabledScheduleCount()) + additionallyDisabled;
                repository.setDisabledScheduleCount(jobId, totalDisabled);
                if (repository.countEnabledSchedules(instance.getId()) != 0) {
                    throw new Matrix26DecommissionException("One or more backup schedules remain enabled.");
                }
                pass(jobId, "SCHEDULES_FINAL", "Schedules permanently disabled",
                        "No automatic backup schedule remains enabled for this decommissioned instance.");

                Matrix26RuntimeInventoryItem stopped = requireStoppedRuntime(instance);
                if (stopped.portListening() || stopped.processId() != null) {
                    throw new Matrix26DecommissionException("The runtime is not fully stopped.");
                }

                LocalDateTime retentionUntil = plan.retentionUntil() != null
                        ? plan.retentionUntil()
                        : LocalDateTime.now().plusDays(Math.max(1, plan.retentionDays()));
                instance.setStatus("DECOMMISSIONED");
                instance.setRuntimeStatus("STOPPED");
                instance.setLastHealthStatus("DECOMMISSIONED");
                instance.setLastHealthMessage(
                        "Instance decommissioned. Database, runtime, resources, and final archive remain preserved."
                );
                instance.setLastHealthCheckedAt(LocalDateTime.now());
                clientRepository.save(instance);

                repository.complete(jobId, "DECOMMISSIONED", retentionUntil);
                repository.addEvent(jobId, "DECOMMISSION_COMPLETED", "COMPLETED", safeActor,
                        "The instance is no longer operational. No database, runtime, resource, module, or backup was deleted.");
                audit(instance, "INSTANCE_DECOMMISSIONED", safeActor,
                        "Instance decommissioned with protected final archive " + plan.finalBackupPublicId() + ".",
                        "SUSPENDED", "DECOMMISSIONED");
                return job(jobId);
            } catch (RuntimeException ex) {
                instance.setStatus("SUSPENDED");
                instance.setRuntimeStatus("STOPPED");
                clientRepository.save(instance);
                repository.fail(jobId, Matrix26DecommissionStatus.MANUAL_REVIEW_REQUIRED, safeMessage(ex));
                repository.addEvent(jobId, "DECOMMISSION_FAILED", "FAILED", safeActor, safeMessage(ex));
                audit(instance, "INSTANCE_DECOMMISSION_FAILED", safeActor,
                        "Decommission execution requires review: " + safeMessage(ex),
                        "DECOMMISSIONING", "SUSPENDED");
                throw exception("The instance could not be decommissioned.", ex);
            }
        } finally {
            lock.unlock();
        }
    }

    private Matrix26DecommissionCandidate candidate(PlatformBusinessClient instance) {
        boolean allowlisted = isAllowlisted(instance) && !instance.isProtectedInstance();
        boolean runtimeStopped = false;
        String blocker = "";
        try {
            Matrix26RuntimeInventoryItem runtime = runtime(instance);
            runtimeStopped = !runtime.portListening() && runtime.processId() == null;
            if (runtime.portListening() && !runtime.expectedProcess()) {
                blocker = "The registered port is occupied by an unexpected process.";
            } else if (!runtime.configurationPresent() || !runtime.configurationConsistent()) {
                blocker = "Runtime configuration is missing or inconsistent.";
            }
        } catch (RuntimeException ex) {
            blocker = safeMessage(ex);
        }
        if (!properties.isEnabled()) {
            blocker = "Decommission Manager is disabled.";
        } else if (!allowlisted) {
            blocker = instance.isProtectedInstance()
                    ? "Protected instances are read only."
                    : "The instance is outside the decommission laboratory allowlist.";
        } else if (!"SUSPENDED".equals(normalizedStatus(instance))) {
            blocker = "The instance must be SUSPENDED before decommission preparation.";
        } else if (!runtimeStopped && blocker.isBlank()) {
            blocker = "The runtime must be completely stopped and its port must be free.";
        } else if (repository.hasActiveJob(instance.getId())) {
            blocker = "Another decommission plan is active.";
        } else if (blocker.isBlank()) {
            blocker = operationalBlocker(instance);
        }
        int enabledSchedules;
        try {
            enabledSchedules = repository.countEnabledSchedules(instance.getId());
        } catch (RuntimeException ex) {
            enabledSchedules = 0;
            if (blocker.isBlank()) {
                blocker = "Schedule metadata could not be read.";
            }
        }
        return new Matrix26DecommissionCandidate(
                instance,
                allowlisted,
                blocker.isBlank(),
                blocker,
                runtimeStopped,
                enabledSchedules,
                "PREPARE DECOMMISSION " + instance.getCode()
        );
    }

    private void validateOperationalBlockers(PlatformBusinessClient instance) {
        String blocker = operationalBlocker(instance);
        if (!blocker.isBlank()) {
            throw new Matrix26DecommissionException(blocker);
        }
    }

    private String operationalBlocker(PlatformBusinessClient instance) {
        if (repository.hasActiveBackup(instance.getId())) {
            return "A backup job is currently active.";
        }
        if (repository.hasActiveScheduleExecution(instance.getId())) {
            return "A scheduled backup execution is currently active.";
        }
        if (repository.hasActiveRuntimeOperation(instance.getId())) {
            return "A runtime operation is currently active.";
        }
        if (repository.hasActiveCloneRestore(instance.getId(), instance.getCode())) {
            return "A clone restore operation is currently active.";
        }
        if (repository.hasActiveInPlaceRestore(instance.getId())) {
            return "An in-place restore is awaiting completion, confirmation, or rollback.";
        }
        if (repository.hasActiveLifecycle(instance.getId())) {
            return "A suspension or reactivation lifecycle job is currently active.";
        }
        return "";
    }

    private Matrix26RuntimeInventoryItem requireStoppedRuntime(PlatformBusinessClient instance) {
        Matrix26RuntimeInventoryItem runtime = runtime(instance);
        if (!runtime.configurationPresent() || !runtime.configurationConsistent()) {
            throw new Matrix26DecommissionException("Runtime configuration does not match the registered instance.");
        }
        if (runtime.portListening() || runtime.processId() != null) {
            if (runtime.portListening() && !runtime.expectedProcess()) {
                throw new Matrix26DecommissionException(
                        "Port " + instance.getRuntimePort() + " is occupied by an unexpected process."
                );
            }
            throw new Matrix26DecommissionException("The runtime must be stopped before decommission.");
        }
        return runtime;
    }

    private Matrix26RuntimeInventoryItem runtime(PlatformBusinessClient instance) {
        try {
            inventoryService.invalidateCache();
            return inventoryService.runtime(String.valueOf(instance.getId()), true);
        } catch (RuntimeException ex) {
            throw new Matrix26DecommissionException("The registered runtime could not be inspected.", ex);
        }
    }

    private PlatformBusinessClient instance(long instanceId) {
        return clientRepository.findById(instanceId)
                .orElseThrow(() -> new Matrix26DecommissionException("The selected instance does not exist."));
    }

    private void validateSuspended(PlatformBusinessClient instance) {
        if (!"SUSPENDED".equals(normalizedStatus(instance))) {
            throw new Matrix26DecommissionException("Only a SUSPENDED laboratory can be decommissioned.");
        }
    }

    private void validateNoActiveDecommission(PlatformBusinessClient instance) {
        if (repository.hasActiveJob(instance.getId())) {
            throw new Matrix26DecommissionException("Another decommission plan is already active for this instance.");
        }
    }

    private void validateFeatureEnabled() {
        if (!properties.isEnabled()) {
            throw new Matrix26DecommissionException("Decommission Manager is disabled by configuration.");
        }
        if (!backupSecurityService.keyStatus().available()) {
            throw new Matrix26DecommissionException(
                    "The backup master key must be available before preparing a final archive."
            );
        }
    }

    private void validateAllowed(PlatformBusinessClient instance) {
        if (instance.isProtectedInstance()) {
            throw new Matrix26DecommissionException("Protected instances are read only in Lifecycle Manager 3G.2.");
        }
        if (!isAllowlisted(instance)) {
            throw new Matrix26DecommissionException("The instance is outside the decommission laboratory allowlist.");
        }
    }

    private boolean isAllowlisted(PlatformBusinessClient instance) {
        String code = instance.getCode() == null ? "" : instance.getCode().toLowerCase(Locale.ROOT);
        return properties.getAllowedInstanceCodes().stream()
                .filter(value -> value != null)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(code::equals);
    }

    private int validateRetentionDays(int retentionDays) {
        int minimum = Math.max(1, properties.getMinimumRetentionDays());
        int maximum = Math.max(minimum, properties.getMaximumRetentionDays());
        if (retentionDays < minimum || retentionDays > maximum) {
            throw new Matrix26DecommissionException(
                    "Retention days must be between " + minimum + " and " + maximum + "."
            );
        }
        return retentionDays;
    }

    private String validateReason(String reason) {
        String safe = reason == null ? "" : reason.trim();
        int minimum = Math.max(5, properties.getMinimumReasonLength());
        if (safe.length() < minimum) {
            throw new Matrix26DecommissionException("Provide a reason of at least " + minimum + " characters.");
        }
        return limit(safe, 1000);
    }

    private String safeNotes(String notes) {
        return limit(notes == null ? "" : notes.trim(), 8000);
    }

    private void validateConfirmation(String actual, String expected) {
        if (!expected.equals(actual == null ? "" : actual.trim())) {
            throw new Matrix26DecommissionException("The confirmation text does not match: " + expected);
        }
    }

    private String normalizedStatus(PlatformBusinessClient instance) {
        String value = instance.getStatus();
        return value == null || value.isBlank() ? "ACTIVE" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String safeActor(String actor) {
        String value = actor == null || actor.isBlank() ? "matrix26-system" : actor.trim();
        return limit(value, 120);
    }

    private String publicId() {
        return "DCM-" + LocalDateTime.now().toString().replaceAll("[-:.T]", "").substring(0, 14)
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private void pass(long jobId, String code, String label, String detail) {
        repository.upsertCheck(jobId, code, label, "PASSED", detail);
    }

    private void fail(long jobId, String code, String label, String detail) {
        repository.upsertCheck(jobId, code, label, "FAILED", detail);
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private String safeMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && (current.getMessage() == null
                || current.getMessage().toLowerCase(Locale.ROOT).contains("unexpected"))) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = current.getClass().getSimpleName();
        }
        return limit(message.replaceAll("[\\r\\n]+", " "), 1000);
    }

    private Matrix26DecommissionException exception(String message, RuntimeException cause) {
        if (cause instanceof Matrix26DecommissionException decommissionException) {
            return decommissionException;
        }
        return new Matrix26DecommissionException(message + " " + safeMessage(cause), cause);
    }

    private String limit(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value;
        }
        return value.substring(0, maximum);
    }

    private void audit(
            PlatformBusinessClient instance,
            String action,
            String actor,
            String summary,
            String before,
            String after
    ) {
        Matrix26InstanceAuditLog audit = new Matrix26InstanceAuditLog();
        audit.setInstance(instance);
        audit.setAction(action);
        audit.setActorUsername(actor);
        audit.setSummary(limit(summary, 500));
        audit.setBeforeSnapshot("status=" + before + ";runtime=" + instance.getRuntimeProfile());
        audit.setAfterSnapshot("status=" + after + ";runtime=" + instance.getRuntimeProfile());
        auditRepository.save(audit);
    }
}
