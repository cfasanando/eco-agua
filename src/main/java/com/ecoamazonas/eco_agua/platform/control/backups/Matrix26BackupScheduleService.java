package com.ecoamazonas.eco_agua.platform.control.backups;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLog;
import com.ecoamazonas.eco_agua.platform.control.Matrix26InstanceAuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26BackupScheduleService {

    private static final Set<String> RECOVERABLE_ALERT_CODES = Set.of(
            "MASTER_KEY_UNAVAILABLE",
            "DUMP_TOOL_UNAVAILABLE",
            "BACKUP_STORAGE_UNAVAILABLE",
            "LOW_DISK_SPACE",
            "SCHEDULED_BACKUP_FAILED"
    );

    private final Matrix26BackupScheduleRepository scheduleRepository;
    private final Matrix26BackupRepository backupRepository;
    private final Matrix26BackupService backupService;
    private final Matrix26BackupSecurityService backupSecurityService;
    private final Matrix26BackupProperties properties;
    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26InstanceAuditLogRepository auditLogRepository;
    private final AtomicBoolean schedulerLock = new AtomicBoolean(false);

    public Matrix26BackupScheduleService(
            Matrix26BackupScheduleRepository scheduleRepository,
            Matrix26BackupRepository backupRepository,
            Matrix26BackupService backupService,
            Matrix26BackupSecurityService backupSecurityService,
            Matrix26BackupProperties properties,
            PlatformBusinessClientRepository clientRepository,
            Matrix26InstanceAuditLogRepository auditLogRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.backupRepository = backupRepository;
        this.backupService = backupService;
        this.backupSecurityService = backupSecurityService;
        this.properties = properties;
        this.clientRepository = clientRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<Matrix26BackupSchedule> schedules() {
        return scheduleRepository.findSchedules();
    }

    public Matrix26BackupSchedule schedule(long id) {
        return scheduleRepository.findSchedule(id)
                .orElseThrow(() -> new Matrix26BackupException("The requested backup schedule does not exist."));
    }

    public List<Matrix26BackupScheduleExecution> recentExecutions() {
        return scheduleRepository.findRecentExecutions(300);
    }

    public List<Matrix26BackupScheduleExecution> executionsForSchedule(long scheduleId) {
        return scheduleRepository.findExecutionsBySchedule(scheduleId);
    }

    public List<Matrix26BackupAlert> alerts(boolean openOnly) {
        return scheduleRepository.findAlerts(openOnly);
    }

    public Matrix26BackupScheduleSummary summary() {
        return scheduleRepository.summary();
    }

    public List<Matrix26BackupCalendarEvent> calendar(int days) {
        int safeDays = Math.max(7, Math.min(days, 90));
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = from.plusDays(safeDays);
        List<Matrix26BackupCalendarEvent> events = new ArrayList<>();

        for (Matrix26BackupScheduleExecution execution : scheduleRepository.findCalendarExecutions(from, to)) {
            events.add(new Matrix26BackupCalendarEvent(
                    execution.plannedAt(),
                    execution.instanceCode(),
                    execution.instanceName(),
                    execution.scheduleName(),
                    "EXECUTION",
                    execution.status().getLabel(),
                    execution.status().getBadgeClass(),
                    execution.id(),
                    execution.backupJobId()
            ));
        }

        for (Matrix26BackupSchedule schedule : scheduleRepository.findEnabledSchedules()) {
            LocalDateTime cursor = schedule.nextRunAt();
            int guard = 0;
            while (cursor != null && cursor.isBefore(to) && guard++ < 100) {
                if (!cursor.isBefore(from)) {
                    events.add(new Matrix26BackupCalendarEvent(
                            cursor,
                            schedule.instanceCode(),
                            schedule.instanceName(),
                            schedule.name(),
                            "PLANNED",
                            "Planned",
                            "text-bg-info",
                            null,
                            null
                    ));
                }
                cursor = nextAfter(schedule, cursor);
            }
        }

        return events.stream()
                .sorted(java.util.Comparator.comparing(Matrix26BackupCalendarEvent::scheduledAt))
                .toList();
    }

    public Matrix26BackupSchedule saveSchedule(
            Long scheduleId,
            long instanceId,
            String name,
            String frequencyValue,
            Integer dayOfWeek,
            Integer dayOfMonth,
            int hourOfDay,
            int minuteOfHour,
            String timezone,
            boolean encryptionRequired,
            String retentionClassValue,
            int maxAttempts,
            int retryDelayMinutes,
            String missedPolicyValue,
            boolean enabled,
            String actor
    ) {
        PlatformBusinessClient instance = allowedInstance(instanceId);
        Matrix26BackupScheduleFrequency frequency = Matrix26BackupScheduleFrequency.from(frequencyValue);
        Matrix26BackupRetentionClass retentionClass = Matrix26BackupRetentionClass.from(retentionClassValue);
        Matrix26BackupMissedPolicy missedPolicy = Matrix26BackupMissedPolicy.from(missedPolicyValue);
        String safeTimezone = validateTimezone(timezone);
        int safeHour = bounded(hourOfDay, 0, 23);
        int safeMinute = bounded(minuteOfHour, 0, 59);
        int safeDayOfWeek = bounded(dayOfWeek == null ? DayOfWeek.SUNDAY.getValue() : dayOfWeek, 1, 7);
        int safeDayOfMonth = bounded(dayOfMonth == null ? 1 : dayOfMonth, 1, 28);
        int safeAttempts = bounded(maxAttempts, 1, 5);
        int safeRetryDelay = bounded(retryDelayMinutes, 1, 1440);
        String safeName = safeText(name, "Backup schedule", 160);
        String safeActor = safeActor(actor);

        LocalDateTime now = LocalDateTime.now(ZoneId.of(safeTimezone));
        Matrix26BackupSchedule provisional = new Matrix26BackupSchedule(
                scheduleId,
                instance.getId(),
                instance.getCode(),
                instance.getBusinessName(),
                safeName,
                frequency,
                frequency == Matrix26BackupScheduleFrequency.WEEKLY ? safeDayOfWeek : null,
                frequency == Matrix26BackupScheduleFrequency.MONTHLY ? safeDayOfMonth : null,
                safeHour,
                safeMinute,
                safeTimezone,
                true,
                retentionClass,
                safeAttempts,
                safeRetryDelay,
                missedPolicy,
                enabled,
                null,
                null,
                null,
                safeActor,
                now,
                safeActor,
                now
        );
        LocalDateTime nextRun = enabled ? nextAfter(provisional, now.minusMinutes(1)) : null;
        Matrix26BackupSchedule schedule = new Matrix26BackupSchedule(
                scheduleId,
                provisional.instanceId(),
                provisional.instanceCode(),
                provisional.instanceName(),
                provisional.name(),
                provisional.frequency(),
                provisional.dayOfWeek(),
                provisional.dayOfMonth(),
                provisional.hourOfDay(),
                provisional.minuteOfHour(),
                provisional.timezone(),
                provisional.encryptionRequired(),
                provisional.retentionClass(),
                provisional.maxAttempts(),
                provisional.retryDelayMinutes(),
                provisional.missedPolicy(),
                provisional.enabled(),
                nextRun,
                scheduleId == null ? null : schedule(scheduleId).lastRunAt(),
                scheduleId == null ? null : schedule(scheduleId).lastStatus(),
                scheduleId == null ? safeActor : schedule(scheduleId).createdBy(),
                scheduleId == null ? now : schedule(scheduleId).createdAt(),
                safeActor,
                now
        );

        long id;
        if (scheduleId == null) {
            id = scheduleRepository.insertSchedule(schedule);
            writeAudit(instance, safeActor, "BACKUP_SCHEDULE_CREATED",
                    "Backup schedule created: " + safeName + ".");
        } else {
            Matrix26BackupSchedule existing = schedule(scheduleId);
            if (!existing.instanceId().equals(instanceId)) {
                throw new Matrix26BackupException("A backup schedule cannot be moved to another instance.");
            }
            scheduleRepository.updateSchedule(schedule);
            id = scheduleId;
            writeAudit(instance, safeActor, "BACKUP_SCHEDULE_UPDATED",
                    "Backup schedule updated: " + safeName + ".");
        }
        return schedule(id);
    }

    public Matrix26BackupSchedule setEnabled(long scheduleId, boolean enabled, String actor) {
        Matrix26BackupSchedule schedule = schedule(scheduleId);
        PlatformBusinessClient instance = allowedInstance(schedule.instanceId());
        LocalDateTime nextRun = enabled
                ? nextAfter(schedule, LocalDateTime.now(ZoneId.of(schedule.timezone())).minusMinutes(1))
                : null;
        scheduleRepository.setScheduleEnabled(scheduleId, enabled, nextRun, safeActor(actor));
        writeAudit(instance, actor, enabled ? "BACKUP_SCHEDULE_ENABLED" : "BACKUP_SCHEDULE_DISABLED",
                (enabled ? "Enabled" : "Disabled") + " backup schedule: " + schedule.name() + ".");
        return schedule(scheduleId);
    }

    public Matrix26BackupScheduleExecution runNow(long scheduleId, String actor) {
        Matrix26BackupSchedule schedule = schedule(scheduleId);
        allowedInstance(schedule.instanceId());
        LocalDateTime now = LocalDateTime.now(ZoneId.of(schedule.timezone())).withSecond(0).withNano(0);
        Matrix26BackupScheduleExecution execution = newExecution(
                schedule,
                now,
                Matrix26BackupScheduleExecutionStatus.QUEUED,
                "MANUAL_SCHEDULE",
                null
        );
        long executionId = scheduleRepository.insertExecution(execution)
                .orElseThrow(() -> new Matrix26BackupException("An execution already exists for this schedule window."));
        processExecution(executionId, safeActor(actor));
        return scheduleRepository.findExecution(executionId).orElseThrow();
    }

    public void processSchedulerTick() {
        if (!properties.isSchedulingEnabled() || !schedulerLock.compareAndSet(false, true)) {
            return;
        }
        try {
            processRetries();
            for (Matrix26BackupSchedule schedule : scheduleRepository.findEnabledSchedules()) {
                evaluateSchedule(schedule);
            }
            evaluateHealthAlerts();
        } finally {
            schedulerLock.set(false);
        }
    }

    public void recoverOnStartup() {
        processSchedulerTick();
    }

    public void resolveAlert(long alertId, String actor) {
        scheduleRepository.resolveAlert(alertId, safeActor(actor));
    }

    private void processRetries() {
        LocalDateTime now = LocalDateTime.now();
        for (Matrix26BackupScheduleExecution execution : scheduleRepository.findRetryable(now)) {
            processExecution(execution.id(), "matrix26-scheduler");
        }
    }

    private void evaluateSchedule(Matrix26BackupSchedule schedule) {
        ZoneId zone = ZoneId.of(schedule.timezone());
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime cursor = schedule.nextRunAt();
        if (cursor == null || cursor.isAfter(now)) {
            return;
        }

        List<LocalDateTime> dueSlots = new ArrayList<>();
        int guard = 0;
        while (cursor != null && !cursor.isAfter(now) && guard++ < 400) {
            dueSlots.add(cursor);
            cursor = nextAfter(schedule, cursor);
        }
        if (dueSlots.isEmpty()) {
            return;
        }

        LocalDateTime latest = dueSlots.get(dueSlots.size() - 1);
        for (int index = 0; index < dueSlots.size() - 1; index++) {
            recordFinalExecution(
                    schedule,
                    dueSlots.get(index),
                    Matrix26BackupScheduleExecutionStatus.MISSED,
                    "MISSED_SUPERSEDED",
                    "A newer missed execution replaced this window."
            );
        }

        long latenessMinutes = Math.max(0, Duration.between(latest, now).toMinutes());
        if (latenessMinutes <= properties.getSchedulerGraceMinutes()) {
            queueAndProcess(schedule, latest, "SCHEDULED");
        } else {
            switch (schedule.missedPolicy()) {
                case RUN_ON_STARTUP -> queueAndProcess(schedule, latest, "MISSED_RUN_ON_STARTUP");
                case SKIP -> recordFinalExecution(
                        schedule,
                        latest,
                        Matrix26BackupScheduleExecutionStatus.SKIPPED,
                        "MISSED_SKIPPED",
                        "The missed execution policy skipped this backup window."
                );
                case MARK_AS_MISSED -> recordFinalExecution(
                        schedule,
                        latest,
                        Matrix26BackupScheduleExecutionStatus.MISSED,
                        "MISSED_RECORDED",
                        "Matrix26 was not available during this backup window."
                );
            }
        }
        scheduleRepository.updateScheduleAfterEvaluation(
                schedule.id(),
                cursor,
                latest,
                latestExecutionStatus(schedule.id())
        );
    }

    private void queueAndProcess(Matrix26BackupSchedule schedule, LocalDateTime plannedAt, String triggerType) {
        Matrix26BackupScheduleExecution execution = newExecution(
                schedule,
                plannedAt,
                Matrix26BackupScheduleExecutionStatus.QUEUED,
                triggerType,
                null
        );
        Optional<Long> executionId = scheduleRepository.insertExecution(execution);
        executionId.ifPresent(id -> processExecution(id, "matrix26-scheduler"));
    }

    private void recordFinalExecution(
            Matrix26BackupSchedule schedule,
            LocalDateTime plannedAt,
            Matrix26BackupScheduleExecutionStatus status,
            String triggerType,
            String message
    ) {
        Matrix26BackupScheduleExecution execution = newExecution(schedule, plannedAt, status, triggerType, message);
        Optional<Long> executionId = scheduleRepository.insertExecution(execution);
        if (executionId.isPresent() && status == Matrix26BackupScheduleExecutionStatus.MISSED) {
            createAlert(
                    schedule,
                    executionId.get(),
                    "BACKUP_EXECUTION_MISSED",
                    Matrix26BackupAlertSeverity.WARNING,
                    "Scheduled backup missed",
                    schedule.name() + " did not run at " + plannedAt + "."
            );
        }
    }

    private void processExecution(long executionId, String actor) {
        Matrix26BackupScheduleExecution execution = scheduleRepository.findExecution(executionId)
                .orElseThrow(() -> new Matrix26BackupException("The scheduled execution does not exist."));
        Matrix26BackupSchedule schedule = schedule(execution.scheduleId());
        PlatformBusinessClient instance = allowedInstance(schedule.instanceId());

        if (scheduleRepository.hasActiveExecution(instance.getId(), executionId)
                || backupRepository.hasActiveJob(instance.getId())) {
            scheduleRepository.markExecutionFinal(
                    executionId,
                    Matrix26BackupScheduleExecutionStatus.SKIPPED,
                    "Another backup operation is already active for this instance."
            );
            createAlert(
                    schedule,
                    executionId,
                    "BACKUP_OPERATION_CONFLICT",
                    Matrix26BackupAlertSeverity.WARNING,
                    "Scheduled backup skipped",
                    "Another backup, verification, or retention operation was active."
            );
            return;
        }

        if (schedule.encryptionRequired() && !backupSecurityService.keyStatus().available()) {
            failExecution(
                    schedule,
                    execution,
                    "MASTER_KEY_UNAVAILABLE",
                    Matrix26BackupAlertSeverity.CRITICAL,
                    "Backup master key unavailable",
                    backupSecurityService.keyStatus().message()
            );
            return;
        }
        Matrix26BackupToolStatus tool = backupService.toolStatus();
        if (!tool.available()) {
            failExecution(
                    schedule,
                    execution,
                    "DUMP_TOOL_UNAVAILABLE",
                    Matrix26BackupAlertSeverity.CRITICAL,
                    "Database dump tool unavailable",
                    tool.message()
            );
            return;
        }
        try {
            validateStorage();
        } catch (Matrix26BackupException ex) {
            boolean lowSpace = ex.getMessage() != null && ex.getMessage().startsWith("LOW_DISK_SPACE:");
            failExecution(
                    schedule,
                    execution,
                    lowSpace ? "LOW_DISK_SPACE" : "BACKUP_STORAGE_UNAVAILABLE",
                    Matrix26BackupAlertSeverity.CRITICAL,
                    lowSpace ? "Low backup storage" : "Backup storage unavailable",
                    lowSpace ? ex.getMessage().substring("LOW_DISK_SPACE:".length()).trim() : ex.getMessage()
            );
            return;
        }

        if (scheduleRepository.markExecutionRunning(executionId) == 0) {
            return;
        }

        Matrix26BackupScheduleExecution running = scheduleRepository.findExecution(executionId).orElseThrow();
        try {
            Matrix26BackupJob job = backupService.createScheduledFullBackup(
                    schedule.instanceId(),
                    safeActor(actor)
            );
            if (schedule.encryptionRequired()) {
                job = backupSecurityService.encryptBackup(
                        job.id(),
                        schedule.retentionClass(),
                        safeActor(actor)
                );
            }
            scheduleRepository.completeExecution(executionId, job.id(), job.publicId());
            scheduleRepository.updateScheduleAfterEvaluation(
                    schedule.id(),
                    schedule.nextRunAt(),
                    running.plannedAt(),
                    Matrix26BackupScheduleExecutionStatus.COMPLETED.name()
            );
            scheduleRepository.resolveAlerts(
                    schedule.instanceId(),
                    schedule.id(),
                    new ArrayList<>(RECOVERABLE_ALERT_CODES),
                    safeActor(actor)
            );
            writeAudit(instance, actor, "SCHEDULED_BACKUP_COMPLETED",
                    "Scheduled backup completed: " + job.publicId() + ".");
        } catch (Exception ex) {
            failExecution(
                    schedule,
                    running,
                    "SCHEDULED_BACKUP_FAILED",
                    Matrix26BackupAlertSeverity.CRITICAL,
                    "Scheduled backup failed",
                    safeMessage(ex)
            );
        }
    }

    private void failExecution(
            Matrix26BackupSchedule schedule,
            Matrix26BackupScheduleExecution execution,
            String alertCode,
            Matrix26BackupAlertSeverity severity,
            String title,
            String message
    ) {
        Matrix26BackupScheduleExecution current = scheduleRepository.findExecution(execution.id()).orElse(execution);
        int attempts = current.attemptCount() == null ? 0 : current.attemptCount();
        int maximum = current.maxAttempts() == null ? schedule.maxAttempts() : current.maxAttempts();
        if (attempts < maximum) {
            LocalDateTime retryAt = LocalDateTime.now().plusMinutes(schedule.retryDelayMinutes());
            scheduleRepository.updateExecutionFailure(
                    execution.id(),
                    Matrix26BackupScheduleExecutionStatus.RETRY_WAITING,
                    retryAt,
                    message
            );
        } else {
            scheduleRepository.updateExecutionFailure(
                    execution.id(),
                    Matrix26BackupScheduleExecutionStatus.FAILED,
                    null,
                    message
            );
        }
        createAlert(schedule, execution.id(), alertCode, severity, title, message);
        PlatformBusinessClient instance = clientRepository.findById(schedule.instanceId()).orElse(null);
        if (instance != null) {
            writeAudit(instance, "matrix26-scheduler", "SCHEDULED_BACKUP_FAILED",
                    schedule.name() + ": " + message);
        }
    }

    private void evaluateHealthAlerts() {
        for (Matrix26BackupSchedule schedule : scheduleRepository.findEnabledSchedules()) {
            if (schedule.encryptionRequired() && !backupSecurityService.keyStatus().available()) {
                createAlert(
                        schedule,
                        null,
                        "MASTER_KEY_UNAVAILABLE",
                        Matrix26BackupAlertSeverity.CRITICAL,
                        "Backup master key unavailable",
                        backupSecurityService.keyStatus().message()
                );
            }
            if (!backupService.toolStatus().available()) {
                createAlert(
                        schedule,
                        null,
                        "DUMP_TOOL_UNAVAILABLE",
                        Matrix26BackupAlertSeverity.CRITICAL,
                        "Database dump tool unavailable",
                        backupService.toolStatus().message()
                );
            }
            try {
                validateStorage();
                scheduleRepository.resolveAlerts(
                        schedule.instanceId(),
                        schedule.id(),
                        List.of("LOW_DISK_SPACE", "BACKUP_STORAGE_UNAVAILABLE"),
                        "matrix26-scheduler"
                );
            } catch (Matrix26BackupException ex) {
                boolean lowSpace = ex.getMessage() != null && ex.getMessage().startsWith("LOW_DISK_SPACE:");
                createAlert(
                        schedule,
                        null,
                        lowSpace ? "LOW_DISK_SPACE" : "BACKUP_STORAGE_UNAVAILABLE",
                        Matrix26BackupAlertSeverity.CRITICAL,
                        lowSpace ? "Low backup storage" : "Backup storage unavailable",
                        lowSpace ? ex.getMessage().substring("LOW_DISK_SPACE:".length()).trim() : ex.getMessage()
                );
            }
            Optional<Matrix26BackupJob> latest = backupRepository.findByInstanceId(schedule.instanceId()).stream()
                    .filter(Matrix26BackupJob::isCompleted)
                    .findFirst();
            if (latest.isEmpty()
                    || latest.get().completedAt() == null
                    || latest.get().completedAt().isBefore(LocalDateTime.now().minusHours(24))) {
                createAlert(
                        schedule,
                        null,
                        "NO_RECENT_SUCCESSFUL_BACKUP",
                        Matrix26BackupAlertSeverity.WARNING,
                        "No recent successful backup",
                        "No completed backup was detected during the last 24 hours."
                );
            } else {
                scheduleRepository.resolveAlerts(
                        schedule.instanceId(),
                        schedule.id(),
                        List.of("NO_RECENT_SUCCESSFUL_BACKUP"),
                        "matrix26-scheduler"
                );
            }
        }
    }

    private void createAlert(
            Matrix26BackupSchedule schedule,
            Long executionId,
            String code,
            Matrix26BackupAlertSeverity severity,
            String title,
            String message
    ) {
        if (scheduleRepository.hasOpenAlert(schedule.instanceId(), schedule.id(), code)) {
            return;
        }
        scheduleRepository.insertAlert(new Matrix26BackupAlert(
                null,
                schedule.instanceId(),
                schedule.instanceCode(),
                schedule.id(),
                executionId,
                code,
                severity,
                Matrix26BackupAlertStatus.OPEN,
                title,
                safeText(message, "Backup operation requires attention.", 4000),
                LocalDateTime.now(),
                null,
                null
        ));
    }

    private Matrix26BackupScheduleExecution newExecution(
            Matrix26BackupSchedule schedule,
            LocalDateTime plannedAt,
            Matrix26BackupScheduleExecutionStatus status,
            String triggerType,
            String error
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new Matrix26BackupScheduleExecution(
                null,
                schedule.id(),
                schedule.instanceId(),
                schedule.instanceCode(),
                schedule.instanceName(),
                schedule.name(),
                plannedAt,
                status,
                0,
                schedule.maxAttempts(),
                status == Matrix26BackupScheduleExecutionStatus.QUEUED ? now : null,
                null,
                status.isFinalState() ? now : null,
                null,
                null,
                null,
                triggerType,
                error,
                now,
                now
        );
    }

    private PlatformBusinessClient allowedInstance(long instanceId) {
        PlatformBusinessClient instance = clientRepository.findById(instanceId)
                .orElseThrow(() -> new Matrix26BackupException("The selected instance does not exist."));
        Matrix26BackupCandidate candidate = backupService.candidates().stream()
                .filter(value -> value.instanceId().equals(instanceId))
                .findFirst()
                .orElseThrow(() -> new Matrix26BackupException("The selected instance is not available for backups."));
        if (!candidate.allowed()) {
            throw new Matrix26BackupException(candidate.restrictionReason());
        }
        return instance;
    }

    private LocalDateTime nextAfter(Matrix26BackupSchedule schedule, LocalDateTime reference) {
        int hour = schedule.hourOfDay();
        int minute = schedule.minuteOfHour();
        return switch (schedule.frequency()) {
            case DAILY -> {
                LocalDateTime candidate = reference.toLocalDate().atTime(hour, minute);
                if (!candidate.isAfter(reference)) {
                    candidate = candidate.plusDays(1);
                }
                yield candidate;
            }
            case WEEKLY -> {
                int target = schedule.dayOfWeek() == null ? DayOfWeek.SUNDAY.getValue() : schedule.dayOfWeek();
                LocalDate date = reference.toLocalDate();
                int delta = target - date.getDayOfWeek().getValue();
                if (delta < 0) {
                    delta += 7;
                }
                LocalDateTime candidate = date.plusDays(delta).atTime(hour, minute);
                if (!candidate.isAfter(reference)) {
                    candidate = candidate.plusWeeks(1);
                }
                yield candidate;
            }
            case MONTHLY -> {
                int day = schedule.dayOfMonth() == null ? 1 : schedule.dayOfMonth();
                YearMonth month = YearMonth.from(reference);
                LocalDateTime candidate = month.atDay(Math.min(day, month.lengthOfMonth())).atTime(hour, minute);
                if (!candidate.isAfter(reference)) {
                    YearMonth nextMonth = month.plusMonths(1);
                    candidate = nextMonth.atDay(Math.min(day, nextMonth.lengthOfMonth())).atTime(hour, minute);
                }
                yield candidate;
            }
        };
    }

    private String latestExecutionStatus(long scheduleId) {
        return scheduleRepository.findExecutionsBySchedule(scheduleId).stream()
                .findFirst()
                .map(value -> value.status().name())
                .orElse(null);
    }

    private void validateStorage() {
        try {
            Path root = backupService.backupRoot();
            Files.createDirectories(root);
            FileStore store = Files.getFileStore(root);
            if (store.getUsableSpace() < properties.getMinimumFreeBytes()) {
                throw new Matrix26BackupException(
                        "LOW_DISK_SPACE: Backup storage has less than "
                                + Matrix26BackupService.formatBytes(properties.getMinimumFreeBytes())
                                + " available."
                );
            }
        } catch (IOException ex) {
            throw new Matrix26BackupException("Backup storage could not be accessed: " + safeMessage(ex), ex);
        }
    }

    private String validateTimezone(String timezone) {
        String value = timezone == null || timezone.isBlank()
                ? properties.getScheduleTimezone()
                : timezone.trim();
        try {
            ZoneId.of(value);
            return value;
        } catch (ZoneRulesException ex) {
            throw new Matrix26BackupException("The selected timezone is invalid.");
        }
    }

    private int bounded(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private String safeActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "matrix26-system";
        }
        return actor.length() <= 120 ? actor : actor.substring(0, 120);
    }

    private String safeText(String value, String fallback, int maximum) {
        String result = value == null || value.isBlank() ? fallback : value.trim();
        return result.length() <= maximum ? result : result.substring(0, maximum);
    }

    private String safeMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex == null ? "Unknown backup error." : ex.getClass().getSimpleName();
        }
        String value = ex.getMessage()
                .replaceAll("(?i)(password|secret|token|api[-_.]?key|private[-_.]?key)=\\S+", "$1=***REDACTED***");
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    @Transactional
    protected void writeAudit(PlatformBusinessClient instance, String actor, String action, String summary) {
        Matrix26InstanceAuditLog log = new Matrix26InstanceAuditLog();
        log.setInstance(instance);
        log.setActorUsername(safeActor(actor));
        log.setAction(action);
        log.setSummary(summary.length() <= 500 ? summary : summary.substring(0, 500));
        log.setAfterSnapshot("{\"instanceCode\":\"" + instance.getCode() + "\"}");
        auditLogRepository.save(log);
    }
}
