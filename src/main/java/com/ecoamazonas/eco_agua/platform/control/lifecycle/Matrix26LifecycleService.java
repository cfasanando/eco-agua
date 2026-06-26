package com.ecoamazonas.eco_agua.platform.control.lifecycle;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLog;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLogRepository;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsInventoryService;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlException;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeControlService;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26RuntimeInventoryItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
public class Matrix26LifecycleService {

    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26LifecycleRepository lifecycleRepository;
    private final Matrix26LifecycleProperties properties;
    private final Matrix26RuntimeControlService runtimeControlService;
    private final Matrix26OperationsInventoryService inventoryService;
    private final Matrix26InstanceAuditLogRepository auditRepository;
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public Matrix26LifecycleService(
            PlatformBusinessClientRepository clientRepository,
            Matrix26LifecycleRepository lifecycleRepository,
            Matrix26LifecycleProperties properties,
            Matrix26RuntimeControlService runtimeControlService,
            Matrix26OperationsInventoryService inventoryService,
            Matrix26InstanceAuditLogRepository auditRepository
    ) {
        this.clientRepository = clientRepository;
        this.lifecycleRepository = lifecycleRepository;
        this.properties = properties;
        this.runtimeControlService = runtimeControlService;
        this.inventoryService = inventoryService;
        this.auditRepository = auditRepository;
    }

    public List<Matrix26LifecycleInstanceView> instances() {
        List<Matrix26LifecycleInstanceView> result = new ArrayList<>();
        for (PlatformBusinessClient instance : clientRepository.findAllByOrderByBusinessNameAsc()) {
            result.add(instanceView(instance));
        }
        return result;
    }

    public Matrix26LifecycleInstanceView instanceView(long instanceId) {
        return instanceView(instance(instanceId));
    }

    public List<Matrix26LifecycleJob> recentJobs() {
        return lifecycleRepository.findRecentJobs();
    }

    public Matrix26LifecycleJob job(long id) {
        return lifecycleRepository.findJob(id)
                .orElseThrow(() -> new Matrix26LifecycleException("The requested lifecycle job does not exist."));
    }

    public List<Matrix26LifecycleEvent> events(long jobId) {
        return lifecycleRepository.findEvents(jobId);
    }

    public List<Matrix26LifecycleScheduleState> scheduleStates(long jobId) {
        return lifecycleRepository.findScheduleStates(jobId);
    }

    public Matrix26LifecycleSummary summary() {
        return lifecycleRepository.summary();
    }

    public Matrix26LifecycleJob suspend(long instanceId, String reason, String confirmation, String actor) {
        ReentrantLock lock = locks.computeIfAbsent(instanceId, ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new Matrix26LifecycleException("Another lifecycle operation is already running for this instance.");
        }
        try {
            PlatformBusinessClient instance = instance(instanceId);
            validateFeatureEnabled();
            validateAllowed(instance);
            validateConfirmation(confirmation, "SUSPEND " + instance.getCode());
            String safeReason = validateReason(reason);
            String safeActor = safeActor(actor);
            String currentStatus = normalizedStatus(instance);
            if (!("ACTIVE".equals(currentStatus) || "SUSPENSION_FAILED".equals(currentStatus))) {
                throw new Matrix26LifecycleException("Only an active laboratory can be suspended.");
            }
            validateNoActiveLifecycle(instance);
            validateOperationalBlockers(instance);

            Matrix26RuntimeInventoryItem runtime = runtime(instance, true);
            validateRuntimeIdentity(runtime, instance);
            Matrix26LifecycleBackupView backup = requireRecentVerifiedBackup(instance);

            long jobId = lifecycleRepository.insertJob(
                    publicId(), instance.getId(), instance.getCode(), instance.getBusinessName(),
                    Matrix26LifecycleAction.SUSPEND, safeReason, safeActor, currentStatus, null
            );
            lifecycleRepository.markStarted(jobId, Matrix26LifecycleStatus.VALIDATING);
            lifecycleRepository.attachBackup(jobId, backup);
            lifecycleRepository.addEvent(jobId, "PRECHECK", "COMPLETED", safeActor,
                    "Lifecycle, backup, restore, runtime, and allowlist checks passed.");

            boolean wasRunning = runtime.portListening() || runtime.processId() != null;
            lifecycleRepository.setRuntimeWasRunning(jobId, wasRunning);
            instance.setStatus("SUSPENDING");
            clientRepository.save(instance);

            try {
                lifecycleRepository.snapshotEnabledSchedules(jobId, instanceId);
                int paused = lifecycleRepository.pauseSnapshottedSchedules(jobId, safeActor);
                lifecycleRepository.setPausedScheduleCount(jobId, paused);
                lifecycleRepository.addEvent(jobId, "BACKUP_SCHEDULES_PAUSED", "COMPLETED", safeActor,
                        paused + " enabled backup schedule(s) were paused.");
                if (lifecycleRepository.hasActiveBackup(instanceId)
                        || lifecycleRepository.hasActiveScheduleExecution(instanceId)) {
                    throw new Matrix26LifecycleException(
                            "A backup operation started while schedules were being paused. Suspension was cancelled."
                    );
                }

                lifecycleRepository.updateStatus(jobId, Matrix26LifecycleStatus.SUSPENDING);
                if (wasRunning) {
                    runtimeControlService.stop(String.valueOf(instanceId), safeActor, "STOP " + instance.getCode());
                    lifecycleRepository.addEvent(jobId, "RUNTIME_STOPPED", "COMPLETED", safeActor,
                            "Runtime Control stopped the laboratory runtime.");
                } else {
                    lifecycleRepository.addEvent(jobId, "RUNTIME_ALREADY_STOPPED", "COMPLETED", safeActor,
                            "The runtime was already offline; no process was terminated.");
                }

                Matrix26RuntimeInventoryItem stopped = runtime(instance, true);
                if (stopped.portListening() || stopped.processId() != null) {
                    throw new Matrix26LifecycleException("The runtime could not be proven stopped after suspension.");
                }

                instance.setStatus("SUSPENDED");
                instance.setRuntimeStatus("STOPPED");
                instance.setLastHealthStatus("SUSPENDED");
                instance.setLastHealthMessage("Runtime intentionally stopped by Matrix26 Lifecycle Manager.");
                instance.setLastHealthCheckedAt(LocalDateTime.now());
                clientRepository.save(instance);

                lifecycleRepository.complete(jobId, Matrix26LifecycleStatus.SUSPENDED, "SUSPENDED");
                lifecycleRepository.addEvent(jobId, "SUSPENSION_COMPLETED", "COMPLETED", safeActor,
                        "The instance is suspended. Database, runtime files, resources, and backups were preserved.");
                audit(instance, "INSTANCE_SUSPENDED", safeActor,
                        "Instance suspended through Matrix26 Lifecycle Manager.", currentStatus, "SUSPENDED");
                return job(jobId);
            } catch (RuntimeException ex) {
                restoreSchedulesAfterFailedSuspension(jobId, safeActor);
                instance.setStatus("SUSPENSION_FAILED");
                clientRepository.save(instance);
                lifecycleRepository.fail(jobId, Matrix26LifecycleStatus.SUSPENSION_FAILED,
                        safeMessage(ex), "SUSPENSION_FAILED");
                lifecycleRepository.addEvent(jobId, "SUSPENSION_FAILED", "FAILED", safeActor, safeMessage(ex));
                audit(instance, "INSTANCE_SUSPENSION_FAILED", safeActor,
                        "Instance suspension failed: " + safeMessage(ex), currentStatus, "SUSPENSION_FAILED");
                throw lifecycleException("The instance could not be suspended.", ex);
            }
        } finally {
            lock.unlock();
        }
    }

    public Matrix26LifecycleJob reactivate(long instanceId, String reason, String confirmation, String actor) {
        ReentrantLock lock = locks.computeIfAbsent(instanceId, ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new Matrix26LifecycleException("Another lifecycle operation is already running for this instance.");
        }
        try {
            PlatformBusinessClient instance = instance(instanceId);
            validateFeatureEnabled();
            validateAllowed(instance);
            validateConfirmation(confirmation, "REACTIVATE " + instance.getCode());
            String safeReason = validateReason(reason);
            String safeActor = safeActor(actor);
            String currentStatus = normalizedStatus(instance);
            if (!("SUSPENDED".equals(currentStatus) || "REACTIVATION_FAILED".equals(currentStatus))) {
                throw new Matrix26LifecycleException("Only a suspended laboratory can be reactivated.");
            }
            validateNoActiveLifecycle(instance);
            validateOperationalBlockers(instance);

            Matrix26LifecycleJob suspensionJob = lifecycleRepository.findLatestSuspension(instanceId)
                    .orElseThrow(() -> new Matrix26LifecycleException(
                            "The suspension record required to restore backup schedules was not found."
                    ));
            Matrix26RuntimeInventoryItem runtime = runtime(instance, true);
            validateRuntimeIdentity(runtime, instance);

            long jobId = lifecycleRepository.insertJob(
                    publicId(), instance.getId(), instance.getCode(), instance.getBusinessName(),
                    Matrix26LifecycleAction.REACTIVATE, safeReason, safeActor, currentStatus, suspensionJob.id()
            );
            lifecycleRepository.markStarted(jobId, Matrix26LifecycleStatus.REACTIVATING);
            lifecycleRepository.addEvent(jobId, "PRECHECK", "COMPLETED", safeActor,
                    "Lifecycle, restore, runtime, and allowlist checks passed.");
            instance.setStatus("REACTIVATING");
            clientRepository.save(instance);

            try {
                if (!runtime.portListening() && runtime.processId() == null) {
                    runtimeControlService.start(String.valueOf(instanceId), safeActor);
                    lifecycleRepository.addEvent(jobId, "RUNTIME_STARTED", "COMPLETED", safeActor,
                            "Runtime Control started the laboratory runtime.");
                } else if (runtime.expectedProcess()) {
                    lifecycleRepository.addEvent(jobId, "RUNTIME_ALREADY_RUNNING", "COMPLETED", safeActor,
                            "The expected runtime process was already active.");
                } else {
                    throw new Matrix26LifecycleException("Port " + instance.getRuntimePort()
                            + " is occupied by an unexpected process.");
                }

                Matrix26RuntimeInventoryItem online = runtime(instance, true);
                if (!online.online()) {
                    throw new Matrix26LifecycleException("The reactivated runtime did not pass the HTTP health check.");
                }

                int restored = lifecycleRepository.restoreSnapshottedSchedules(suspensionJob.id(), safeActor);
                lifecycleRepository.setPausedScheduleCount(jobId, restored);
                lifecycleRepository.addEvent(jobId, "BACKUP_SCHEDULES_RESTORED", "COMPLETED", safeActor,
                        restored + " backup schedule(s) were re-enabled.");

                instance.setStatus("ACTIVE");
                instance.setRuntimeStatus("ONLINE");
                instance.setLastHealthStatus("ONLINE");
                instance.setLastHealthMessage("Runtime reactivated and verified by Matrix26 Lifecycle Manager.");
                instance.setLastHealthCheckedAt(LocalDateTime.now());
                clientRepository.save(instance);

                lifecycleRepository.complete(jobId, Matrix26LifecycleStatus.ACTIVE, "ACTIVE");
                lifecycleRepository.addEvent(jobId, "REACTIVATION_COMPLETED", "COMPLETED", safeActor,
                        "The instance is active and its previous backup schedules were restored.");
                audit(instance, "INSTANCE_REACTIVATED", safeActor,
                        "Instance reactivated through Matrix26 Lifecycle Manager.", currentStatus, "ACTIVE");
                return job(jobId);
            } catch (RuntimeException ex) {
                instance.setStatus("REACTIVATION_FAILED");
                clientRepository.save(instance);
                lifecycleRepository.fail(jobId, Matrix26LifecycleStatus.REACTIVATION_FAILED,
                        safeMessage(ex), "REACTIVATION_FAILED");
                lifecycleRepository.addEvent(jobId, "REACTIVATION_FAILED", "FAILED", safeActor, safeMessage(ex));
                audit(instance, "INSTANCE_REACTIVATION_FAILED", safeActor,
                        "Instance reactivation failed: " + safeMessage(ex), currentStatus, "REACTIVATION_FAILED");
                throw lifecycleException("The instance could not be reactivated.", ex);
            }
        } finally {
            lock.unlock();
        }
    }

    private Matrix26LifecycleInstanceView instanceView(PlatformBusinessClient instance) {
        boolean allowlisted = isAllowlisted(instance);
        Matrix26RuntimeInventoryItem runtime = null;
        String runtimeState = "Unavailable";
        boolean runtimeOnline = false;
        boolean portListening = false;
        boolean expectedProcess = false;
        String runtimeProblem = "";
        try {
            runtime = runtime(instance, false);
            runtimeState = runtime.state().name();
            runtimeOnline = runtime.online();
            portListening = runtime.portListening();
            expectedProcess = runtime.expectedProcess();
            if (portListening && !expectedProcess) {
                runtimeProblem = "The registered port is occupied by an unexpected process.";
            } else if (!runtime.configurationPresent() || !runtime.configurationConsistent()) {
                runtimeProblem = "Runtime configuration is missing or does not match the instance registry.";
            }
        } catch (RuntimeException ex) {
            runtimeProblem = safeMessage(ex);
        }

        Matrix26LifecycleBackupView backup;
        int schedules;
        try {
            backup = lifecycleRepository.latestVerifiedBackup(instance.getId());
            schedules = lifecycleRepository.countEnabledSchedules(instance.getId());
        } catch (RuntimeException ex) {
            backup = new Matrix26LifecycleBackupView(null, null, null, null);
            schedules = 0;
            if (runtimeProblem.isBlank()) {
                runtimeProblem = "Lifecycle metadata could not be read.";
            }
        }
        boolean recentBackup = isRecent(backup);
        String blocker = blockingReason(instance, allowlisted, runtimeProblem, recentBackup);
        boolean blocked = !blocker.isBlank();
        String status = normalizedStatus(instance);
        boolean canSuspend = !blocked && ("ACTIVE".equals(status) || "SUSPENSION_FAILED".equals(status));
        boolean canReactivate = !blocked && ("SUSPENDED".equals(status) || "REACTIVATION_FAILED".equals(status));

        return new Matrix26LifecycleInstanceView(
                instance, allowlisted, runtimeOnline, portListening, expectedProcess, runtimeState,
                schedules, backup, recentBackup, blocked, blocker, canSuspend, canReactivate,
                "SUSPEND " + instance.getCode(), "REACTIVATE " + instance.getCode()
        );
    }

    private String blockingReason(
            PlatformBusinessClient instance,
            boolean allowlisted,
            String runtimeProblem,
            boolean recentBackup
    ) {
        if (!properties.isEnabled()) {
            return "Lifecycle Manager is disabled.";
        }
        if (!allowlisted) {
            return instance.isProtectedInstance()
                    ? "Protected instances are read only."
                    : "The instance is outside the lifecycle laboratory allowlist.";
        }
        if (instance.isProtectedInstance()) {
            return "Protected instances cannot be suspended from this phase.";
        }
        if (!runtimeProblem.isBlank()) {
            return runtimeProblem;
        }
        if (lifecycleRepository.hasActiveJob(instance.getId())) {
            return "Another lifecycle job is active.";
        }
        String operational = operationalBlocker(instance);
        if (!operational.isBlank()) {
            return operational;
        }
        String status = normalizedStatus(instance);
        if (("ACTIVE".equals(status) || "SUSPENSION_FAILED".equals(status)) && !recentBackup) {
            return "A recent encrypted and verified backup is required before suspension.";
        }
        return "";
    }

    private String operationalBlocker(PlatformBusinessClient instance) {
        if (lifecycleRepository.hasActiveBackup(instance.getId())) {
            return "A backup job is currently active.";
        }
        if (lifecycleRepository.hasActiveScheduleExecution(instance.getId())) {
            return "A scheduled backup execution is currently active.";
        }
        if (lifecycleRepository.hasActiveRuntimeOperation(instance.getId())) {
            return "A runtime operation is currently active.";
        }
        if (lifecycleRepository.hasActiveCloneRestore(instance.getId(), instance.getCode())) {
            return "A clone restore operation is currently active.";
        }
        if (lifecycleRepository.hasActiveInPlaceRestore(instance.getId())) {
            return "An in-place restore is awaiting completion, confirmation, or rollback.";
        }
        return "";
    }

    private void validateOperationalBlockers(PlatformBusinessClient instance) {
        String blocker = operationalBlocker(instance);
        if (!blocker.isBlank()) {
            throw new Matrix26LifecycleException(blocker);
        }
    }

    private void validateNoActiveLifecycle(PlatformBusinessClient instance) {
        if (lifecycleRepository.hasActiveJob(instance.getId())) {
            throw new Matrix26LifecycleException("Another lifecycle job is already active for this instance.");
        }
    }

    private Matrix26LifecycleBackupView requireRecentVerifiedBackup(PlatformBusinessClient instance) {
        Matrix26LifecycleBackupView backup = lifecycleRepository.latestVerifiedBackup(instance.getId());
        if (!isRecent(backup)) {
            int hours = Math.max(1, properties.getMaximumVerifiedBackupAgeHours());
            throw new Matrix26LifecycleException(
                    "Create and verify an encrypted full backup from the last " + hours
                            + " hours before suspending this instance."
            );
        }
        return backup;
    }

    private boolean isRecent(Matrix26LifecycleBackupView backup) {
        if (backup == null || !backup.available()) {
            return false;
        }
        LocalDateTime reference = backup.verifiedAt() != null ? backup.verifiedAt() : backup.completedAt();
        if (reference == null) {
            return false;
        }
        return reference.isAfter(LocalDateTime.now().minusHours(
                Math.max(1, properties.getMaximumVerifiedBackupAgeHours())
        ));
    }

    private void validateRuntimeIdentity(Matrix26RuntimeInventoryItem runtime, PlatformBusinessClient instance) {
        if (!runtime.configurationPresent() || !runtime.configurationConsistent()) {
            throw new Matrix26LifecycleException("Runtime configuration does not match the registered instance.");
        }
        if (runtime.portListening() && !runtime.expectedProcess()) {
            throw new Matrix26LifecycleException("Port " + instance.getRuntimePort()
                    + " is occupied by an unexpected process.");
        }
    }

    private Matrix26RuntimeInventoryItem runtime(PlatformBusinessClient instance, boolean refresh) {
        try {
            inventoryService.invalidateCache();
            return inventoryService.runtime(String.valueOf(instance.getId()), refresh);
        } catch (RuntimeException ex) {
            throw new Matrix26LifecycleException("The registered runtime could not be inspected.", ex);
        }
    }

    private void restoreSchedulesAfterFailedSuspension(long jobId, String actor) {
        try {
            lifecycleRepository.restoreSnapshottedSchedules(jobId, actor);
            lifecycleRepository.addEvent(jobId, "SCHEDULE_COMPENSATION", "COMPLETED", actor,
                    "Backup schedules were restored after the suspension failed.");
        } catch (RuntimeException ignored) {
            lifecycleRepository.addEvent(jobId, "SCHEDULE_COMPENSATION", "FAILED", actor,
                    "Matrix26 could not automatically restore all paused schedules.");
        }
    }

    private PlatformBusinessClient instance(long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new Matrix26LifecycleException("The requested instance does not exist."));
    }

    private void validateFeatureEnabled() {
        if (!properties.isEnabled()) {
            throw new Matrix26LifecycleException("Lifecycle Manager is disabled by configuration.");
        }
    }

    private void validateAllowed(PlatformBusinessClient instance) {
        if (instance.isProtectedInstance()) {
            throw new Matrix26LifecycleException("Protected instances are read only in Lifecycle Manager 3G.1.");
        }
        if (!isAllowlisted(instance)) {
            throw new Matrix26LifecycleException("The instance is outside the lifecycle laboratory allowlist.");
        }
    }

    private boolean isAllowlisted(PlatformBusinessClient instance) {
        String code = instance.getCode() == null ? "" : instance.getCode().toLowerCase(Locale.ROOT);
        return properties.getAllowedInstanceCodes().stream()
                .filter(value -> value != null)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(code::equals);
    }

    private String validateReason(String reason) {
        String safe = reason == null ? "" : reason.trim();
        int minimum = Math.max(5, properties.getMinimumReasonLength());
        if (safe.length() < minimum) {
            throw new Matrix26LifecycleException("Provide an operational reason of at least " + minimum + " characters.");
        }
        return safe.length() <= 1000 ? safe : safe.substring(0, 1000);
    }

    private void validateConfirmation(String actual, String expected) {
        if (!expected.equals(actual == null ? "" : actual.trim())) {
            throw new Matrix26LifecycleException("The confirmation text does not match: " + expected);
        }
    }

    private String normalizedStatus(PlatformBusinessClient instance) {
        String value = instance.getStatus();
        return value == null || value.isBlank() ? "ACTIVE" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String safeActor(String actor) {
        String value = actor == null || actor.isBlank() ? "matrix26-system" : actor.trim();
        return value.length() <= 120 ? value : value.substring(0, 120);
    }

    private String publicId() {
        return "LCY-" + LocalDateTime.now().toString().replaceAll("[-:.T]", "").substring(0, 14)
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String safeMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getMessage() != null
                && current.getMessage().toLowerCase(Locale.ROOT).contains("unexpectedly")) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = current.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private Matrix26LifecycleException lifecycleException(String message, RuntimeException cause) {
        if (cause instanceof Matrix26LifecycleException lifecycleException) {
            return lifecycleException;
        }
        if (cause instanceof Matrix26RuntimeControlException) {
            return new Matrix26LifecycleException(message + " " + safeMessage(cause), cause);
        }
        return new Matrix26LifecycleException(message, cause);
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
        audit.setSummary(summary);
        audit.setBeforeSnapshot("status=" + before + ";runtime=" + instance.getRuntimeProfile());
        audit.setAfterSnapshot("status=" + after + ";runtime=" + instance.getRuntimeProfile());
        auditRepository.save(audit);
    }
}
