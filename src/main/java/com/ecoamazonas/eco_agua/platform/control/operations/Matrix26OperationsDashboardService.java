package com.ecoamazonas.eco_agua.platform.control.operations;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.PlatformBusinessClientRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupJob;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupScheduleRepository;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupScheduleSummary;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupStatus;
import com.ecoamazonas.eco_agua.platform.control.backups.Matrix26BackupSummary;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.Matrix26LifecycleJob;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.Matrix26LifecycleRepository;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.Matrix26LifecycleStatus;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.Matrix26LifecycleSummary;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.archive.Matrix26ArchiveRepository;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.archive.Matrix26ArchiveRestoreLink;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.archive.Matrix26ArchiveSummary;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission.Matrix26DecommissionJob;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission.Matrix26DecommissionRepository;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission.Matrix26DecommissionStatus;
import com.ecoamazonas.eco_agua.platform.control.lifecycle.decommission.Matrix26DecommissionSummary;
import com.ecoamazonas.eco_agua.platform.control.purge.Matrix26ArchiveDestructionPlan;
import com.ecoamazonas.eco_agua.platform.control.purge.Matrix26ArchiveDestructionRepository;
import com.ecoamazonas.eco_agua.platform.control.purge.Matrix26ArchiveDestructionStatus;
import com.ecoamazonas.eco_agua.platform.control.purge.Matrix26ArchiveDestructionSummary;
import com.ecoamazonas.eco_agua.platform.control.purge.Matrix26PurgePlan;
import com.ecoamazonas.eco_agua.platform.control.purge.Matrix26PurgeProperties;
import com.ecoamazonas.eco_agua.platform.control.purge.Matrix26PurgeRepository;
import com.ecoamazonas.eco_agua.platform.control.purge.Matrix26PurgeStatus;
import com.ecoamazonas.eco_agua.platform.control.purge.Matrix26PurgeSummary;
import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26InPlaceRestoreJob;
import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26InPlaceRestoreRepository;
import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26InPlaceRestoreStatus;
import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26InPlaceRestoreSummary;
import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26RestoreJob;
import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26RestoreRepository;
import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26RestoreStatus;
import com.ecoamazonas.eco_agua.platform.control.restores.Matrix26RestoreSummary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26OperationsDashboardService {

    private final PlatformBusinessClientRepository clientRepository;
    private final Matrix26BackupRepository backupRepository;
    private final Matrix26BackupScheduleRepository backupScheduleRepository;
    private final Matrix26RestoreRepository restoreRepository;
    private final Matrix26InPlaceRestoreRepository inPlaceRestoreRepository;
    private final Matrix26LifecycleRepository lifecycleRepository;
    private final Matrix26DecommissionRepository decommissionRepository;
    private final Matrix26ArchiveRepository archiveRepository;
    private final Matrix26PurgeRepository purgeRepository;
    private final Matrix26ArchiveDestructionRepository archiveDestructionRepository;
    private final Matrix26RuntimeControlService runtimeControlService;
    private final Matrix26PurgeProperties purgeProperties;

    public Matrix26OperationsDashboardService(
            PlatformBusinessClientRepository clientRepository,
            Matrix26BackupRepository backupRepository,
            Matrix26BackupScheduleRepository backupScheduleRepository,
            Matrix26RestoreRepository restoreRepository,
            Matrix26InPlaceRestoreRepository inPlaceRestoreRepository,
            Matrix26LifecycleRepository lifecycleRepository,
            Matrix26DecommissionRepository decommissionRepository,
            Matrix26ArchiveRepository archiveRepository,
            Matrix26PurgeRepository purgeRepository,
            Matrix26ArchiveDestructionRepository archiveDestructionRepository,
            Matrix26RuntimeControlService runtimeControlService,
            Matrix26PurgeProperties purgeProperties
    ) {
        this.clientRepository = clientRepository;
        this.backupRepository = backupRepository;
        this.backupScheduleRepository = backupScheduleRepository;
        this.restoreRepository = restoreRepository;
        this.inPlaceRestoreRepository = inPlaceRestoreRepository;
        this.lifecycleRepository = lifecycleRepository;
        this.decommissionRepository = decommissionRepository;
        this.archiveRepository = archiveRepository;
        this.purgeRepository = purgeRepository;
        this.archiveDestructionRepository = archiveDestructionRepository;
        this.runtimeControlService = runtimeControlService;
        this.purgeProperties = purgeProperties;
    }

    public Matrix26OperationsDashboard dashboard(Matrix26OperationsSnapshot snapshot) {
        List<Matrix26OperationsDashboardAlert> alerts = new ArrayList<>();
        List<PlatformBusinessClient> instances = safe(
                "Instance registry",
                clientRepository::findAllByOrderByBusinessNameAsc,
                List.of(),
                alerts
        );
        Matrix26BackupSummary backupSummary = safe("Backup summary", backupRepository::summary, new Matrix26BackupSummary(0, 0, 0, 0, 0), alerts);
        Matrix26BackupScheduleSummary scheduleSummary = safe("Backup schedule summary", backupScheduleRepository::summary, new Matrix26BackupScheduleSummary(0, 0, 0, 0, 0), alerts);
        Matrix26RestoreSummary restoreSummary = safe("Clone restore summary", restoreRepository::summary, new Matrix26RestoreSummary(0, 0, 0, 0), alerts);
        Matrix26InPlaceRestoreSummary inPlaceRestoreSummary = safe("In-place restore summary", inPlaceRestoreRepository::summary, new Matrix26InPlaceRestoreSummary(0, 0, 0, 0), alerts);
        Matrix26LifecycleSummary lifecycleSummary = safe("Lifecycle summary", lifecycleRepository::summary, new Matrix26LifecycleSummary(0, 0, 0, 0), alerts);
        Matrix26DecommissionSummary decommissionSummary = safe("Decommission summary", decommissionRepository::summary, new Matrix26DecommissionSummary(0, 0, 0, 0), alerts);
        Matrix26ArchiveSummary archiveSummary = safe("Archive summary", archiveRepository::summary, new Matrix26ArchiveSummary(0, 0, 0, 0), alerts);
        Matrix26PurgeSummary purgeSummary = safe("Purge summary", purgeRepository::summary, new Matrix26PurgeSummary(0, 0, 0, 0, 0), alerts);
        Matrix26ArchiveDestructionSummary archiveDestructionSummary = safe("Archive destruction summary", archiveDestructionRepository::summary, new Matrix26ArchiveDestructionSummary(0, 0, 0, 0, 0), alerts);

        List<Matrix26BackupJob> backupJobs = safe("Recent backups", backupRepository::findRecent, List.of(), alerts);
        List<Matrix26RestoreJob> restoreJobs = safe("Recent clone restores", restoreRepository::findRecent, List.of(), alerts);
        List<Matrix26InPlaceRestoreJob> inPlaceRestoreJobs = safe("Recent in-place restores", inPlaceRestoreRepository::findRecent, List.of(), alerts);
        List<Matrix26LifecycleJob> lifecycleJobs = safe("Recent lifecycle jobs", lifecycleRepository::findRecentJobs, List.of(), alerts);
        List<Matrix26DecommissionJob> decommissionJobs = safe("Recent decommission jobs", decommissionRepository::findRecentJobs, List.of(), alerts);
        List<Matrix26PurgePlan> purgePlans = safe("Recent purge plans", purgeRepository::recentPlans, List.of(), alerts);
        List<Matrix26ArchiveDestructionPlan> destructionPlans = safe("Recent archive destruction plans", archiveDestructionRepository::recentPlans, List.of(), alerts);
        List<Matrix26ArchiveRestoreLink> archiveRestoreLinks = safe("Recent archive restore links", archiveRepository::recentRestoreLinks, List.of(), alerts);
        List<Matrix26RuntimeOperation> runtimeOperations = safe("Runtime operations", runtimeControlService::recentOperations, List.of(), alerts);

        alerts.addAll(snapshotAlerts(snapshot));
        alerts.addAll(statusAlerts(
                backupSummary,
                scheduleSummary,
                restoreSummary,
                inPlaceRestoreSummary,
                lifecycleSummary,
                decommissionSummary,
                purgeSummary,
                archiveDestructionSummary,
                archiveRestoreLinks
        ));

        List<Matrix26OperationsDashboardMetric> metrics = metrics(
                instances,
                snapshot.summary(),
                backupSummary,
                scheduleSummary,
                restoreSummary,
                inPlaceRestoreSummary,
                lifecycleSummary,
                decommissionSummary,
                archiveSummary,
                purgeSummary,
                archiveDestructionSummary
        );

        return new Matrix26OperationsDashboard(
                LocalDateTime.now(),
                metrics,
                alerts.stream().limit(12).toList(),
                activities(backupJobs, restoreJobs, inPlaceRestoreJobs, lifecycleJobs, decommissionJobs, purgePlans, destructionPlans, runtimeOperations),
                instanceHealth(instances, snapshot),
                purgeProperties.isArchiveDestructionExecutionEnabled(),
                purgeProperties.isExecutionEnabled(),
                alerts.stream().anyMatch(alert -> "WARNING".equals(alert.severity()) && alert.title().contains("unavailable"))
        );
    }

    private List<Matrix26OperationsDashboardMetric> metrics(
            List<PlatformBusinessClient> instances,
            Matrix26OperationsSummary operationsSummary,
            Matrix26BackupSummary backupSummary,
            Matrix26BackupScheduleSummary scheduleSummary,
            Matrix26RestoreSummary restoreSummary,
            Matrix26InPlaceRestoreSummary inPlaceRestoreSummary,
            Matrix26LifecycleSummary lifecycleSummary,
            Matrix26DecommissionSummary decommissionSummary,
            Matrix26ArchiveSummary archiveSummary,
            Matrix26PurgeSummary purgeSummary,
            Matrix26ArchiveDestructionSummary archiveDestructionSummary
    ) {
        long activeInstances = instances.stream().filter(instance -> same(instance.getStatus(), "ACTIVE")).count();
        long protectedInstances = instances.stream().filter(PlatformBusinessClient::isProtectedInstance).count();
        long decommissionedInstances = instances.stream().filter(instance -> same(instance.getStatus(), "DECOMMISSIONED")).count();
        long purgedInstances = instances.stream().filter(instance -> same(instance.getStatus(), "PURGED") || same(instance.getRuntimeStatus(), "PURGED")).count();
        long restoreRunning = restoreSummary.running() + inPlaceRestoreSummary.awaitingConfirmation();
        long restoreFailed = restoreSummary.failed() + inPlaceRestoreSummary.failed();

        return List.of(
                new Matrix26OperationsDashboardMetric("Instances", String.valueOf(instances.size()), activeInstances + " active, " + protectedInstances + " protected", "bi-hdd-stack", "primary", "/control-center/instances"),
                new Matrix26OperationsDashboardMetric("Runtime health", String.valueOf(operationsSummary.onlineRuntimes()), operationsSummary.degradedRuntimes() + " need attention, " + operationsSummary.offlineRuntimes() + " offline", "bi-cpu", tone(operationsSummary.degradedRuntimes() + operationsSummary.offlineRuntimes()), "/control-center/operations/runtimes"),
                new Matrix26OperationsDashboardMetric("Backups", String.valueOf(backupSummary.completed()), backupSummary.failed() + " failed, " + backupSummary.running() + " running", "bi-database-check", tone(backupSummary.failed()), "/control-center/backups"),
                new Matrix26OperationsDashboardMetric("Schedules", String.valueOf(scheduleSummary.activeSchedules()), scheduleSummary.openAlerts() + " open alerts, " + scheduleSummary.missedExecutions() + " missed", "bi-calendar2-week", tone(scheduleSummary.openAlerts() + scheduleSummary.missedExecutions()), "/control-center/backups/schedules"),
                new Matrix26OperationsDashboardMetric("Restores", String.valueOf(restoreSummary.completed() + inPlaceRestoreSummary.completed()), restoreFailed + " failed, " + restoreRunning + " pending/running", "bi-arrow-counterclockwise", tone(restoreFailed + restoreRunning), "/control-center/restores"),
                new Matrix26OperationsDashboardMetric("Lifecycle", String.valueOf(lifecycleSummary.suspendedInstances()), lifecycleSummary.activeJobs() + " active jobs, " + decommissionedInstances + " decommissioned", "bi-pause-circle", tone(lifecycleSummary.failedJobs() + lifecycleSummary.activeJobs()), "/control-center/lifecycle"),
                new Matrix26OperationsDashboardMetric("Final archives", String.valueOf(archiveSummary.readyArchives()), archiveSummary.cloneRestores() + " clone restores, " + archiveSummary.verifiedArchives() + " verified", "bi-safe2", "info", "/control-center/lifecycle/archive"),
                new Matrix26OperationsDashboardMetric("Purge safety", String.valueOf(purgeSummary.blocked()), purgeSummary.dryRunReady() + " ready, " + purgedInstances + " purged", "bi-search-heart", tone(purgeSummary.blocked()), "/control-center/purge"),
                new Matrix26OperationsDashboardMetric("Archive destruction", archiveDestructionSummary.readyForReview() + " ready", archiveDestructionSummary.blocked() + " blocked, execution " + (purgeProperties.isArchiveDestructionExecutionEnabled() ? "enabled" : "disabled"), "bi-shield-exclamation", purgeProperties.isArchiveDestructionExecutionEnabled() ? "danger" : "success", "/control-center/purge/archive-destruction")
        );
    }

    private List<Matrix26OperationsDashboardAlert> snapshotAlerts(Matrix26OperationsSnapshot snapshot) {
        List<Matrix26OperationsDashboardAlert> alerts = new ArrayList<>();
        for (String warning : snapshot.probeWarnings()) {
            alerts.add(new Matrix26OperationsDashboardAlert("WARNING", "bi-exclamation-triangle-fill", "Probe warning", warning, "/control-center/operations", "Review inventory"));
        }
        for (Matrix26RuntimeInventoryItem runtime : snapshot.runtimes()) {
            if (runtime.state() == Matrix26RuntimeState.PORT_OCCUPIED || runtime.state() == Matrix26RuntimeState.DEGRADED) {
                alerts.add(new Matrix26OperationsDashboardAlert("WARNING", runtime.state().getIconClass(), runtime.target().businessName(), runtime.stateDetail(), "/control-center/operations/runtimes/" + runtime.target().key(), "Open runtime"));
            }
            if (runtime.state() == Matrix26RuntimeState.CONFIGURATION_MISSING || runtime.state() == Matrix26RuntimeState.RUNTIME_MISSING) {
                alerts.add(new Matrix26OperationsDashboardAlert("CRITICAL", runtime.state().getIconClass(), runtime.target().businessName(), runtime.stateDetail(), "/control-center/operations/runtimes/" + runtime.target().key(), "Open runtime"));
            }
        }
        if (alerts.isEmpty()) {
            alerts.add(new Matrix26OperationsDashboardAlert("SUCCESS", "bi-shield-check", "No critical runtime alerts", "The latest runtime inventory did not report blocking operational issues.", "/control-center/operations/runtimes", "View runtimes"));
        }
        return alerts;
    }

    private List<Matrix26OperationsDashboardAlert> statusAlerts(
            Matrix26BackupSummary backupSummary,
            Matrix26BackupScheduleSummary scheduleSummary,
            Matrix26RestoreSummary restoreSummary,
            Matrix26InPlaceRestoreSummary inPlaceRestoreSummary,
            Matrix26LifecycleSummary lifecycleSummary,
            Matrix26DecommissionSummary decommissionSummary,
            Matrix26PurgeSummary purgeSummary,
            Matrix26ArchiveDestructionSummary archiveDestructionSummary,
            List<Matrix26ArchiveRestoreLink> archiveRestoreLinks
    ) {
        List<Matrix26OperationsDashboardAlert> alerts = new ArrayList<>();
        if (backupSummary.failed() > 0) {
            alerts.add(new Matrix26OperationsDashboardAlert("WARNING", "bi-database-exclamation", "Failed backups", backupSummary.failed() + " backup jobs failed and should be reviewed.", "/control-center/backups", "Review backups"));
        }
        if (scheduleSummary.openAlerts() > 0 || scheduleSummary.missedExecutions() > 0) {
            alerts.add(new Matrix26OperationsDashboardAlert("WARNING", "bi-bell", "Backup schedule alerts", scheduleSummary.openAlerts() + " open alerts and " + scheduleSummary.missedExecutions() + " missed executions detected.", "/control-center/backups/alerts", "Open alerts"));
        }
        if (restoreSummary.failed() + inPlaceRestoreSummary.failed() > 0) {
            alerts.add(new Matrix26OperationsDashboardAlert("CRITICAL", "bi-arrow-counterclockwise", "Restore failures", "At least one restore job requires review before new destructive operations.", "/control-center/restores", "Review restores"));
        }
        if (lifecycleSummary.activeJobs() > 0 || decommissionSummary.readyJobs() > 0) {
            alerts.add(new Matrix26OperationsDashboardAlert("WARNING", "bi-pause-circle", "Lifecycle actions pending", lifecycleSummary.activeJobs() + " lifecycle jobs are active and " + decommissionSummary.readyJobs() + " decommission jobs are ready.", "/control-center/lifecycle", "Review lifecycle"));
        }
        if (purgeSummary.blocked() > 0) {
            alerts.add(new Matrix26OperationsDashboardAlert("WARNING", "bi-search-heart", "Blocked purge plans", purgeSummary.blocked() + " purge plans are blocked by safety checks.", "/control-center/purge", "Review purge"));
        }
        if (archiveDestructionSummary.blocked() > 0) {
            alerts.add(new Matrix26OperationsDashboardAlert("WARNING", "bi-shield-exclamation", "Blocked archive destruction plans", archiveDestructionSummary.blocked() + " archive destruction plans are blocked or waiting for retention/clone review.", "/control-center/purge/archive-destruction", "Review archives"));
        }
        if (!archiveRestoreLinks.isEmpty()) {
            alerts.add(new Matrix26OperationsDashboardAlert("INFO", "bi-arrow-repeat", "Archive clone links exist", archiveRestoreLinks.size() + " recent archive restore links should be checked before destroying archive packages.", "/control-center/lifecycle/archive/restores", "View links"));
        }
        if (purgeProperties.isArchiveDestructionExecutionEnabled()) {
            alerts.add(new Matrix26OperationsDashboardAlert("CRITICAL", "bi-radioactive", "Archive destruction execution is enabled", "Disable it after the controlled test to avoid leaving physical destruction available.", "/control-center/purge/archive-destruction", "Review setting"));
        }
        return alerts;
    }

    private List<Matrix26OperationsDashboardActivity> activities(
            List<Matrix26BackupJob> backupJobs,
            List<Matrix26RestoreJob> restoreJobs,
            List<Matrix26InPlaceRestoreJob> inPlaceRestoreJobs,
            List<Matrix26LifecycleJob> lifecycleJobs,
            List<Matrix26DecommissionJob> decommissionJobs,
            List<Matrix26PurgePlan> purgePlans,
            List<Matrix26ArchiveDestructionPlan> destructionPlans,
            List<Matrix26RuntimeOperation> runtimeOperations
    ) {
        List<Matrix26OperationsDashboardActivity> activities = new ArrayList<>();
        backupJobs.stream().limit(10).forEach(job -> activities.add(new Matrix26OperationsDashboardActivity(
                preferredDate(job.completedAt(), job.startedAt(), job.requestedAt()),
                "Backup",
                job.instanceCode(),
                value(job.publicId(), "Backup job"),
                job.status().getLabel(),
                job.status().getBadgeClass(),
                value(job.verificationSummary(), value(job.lastError(), job.backupType())),
                "/control-center/backups/" + job.id()
        )));
        restoreJobs.stream().limit(10).forEach(job -> activities.add(new Matrix26OperationsDashboardActivity(
                preferredDate(job.completedAt(), job.startedAt(), job.requestedAt()),
                "Clone restore",
                job.targetInstanceCode(),
                value(job.publicId(), "Restore job"),
                job.status().getLabel(),
                job.status().getBadgeClass(),
                value(job.lastError(), job.sourceInstanceCode() + " to " + job.targetInstanceCode()),
                "/control-center/restores/" + job.id()
        )));
        inPlaceRestoreJobs.stream().limit(10).forEach(job -> activities.add(new Matrix26OperationsDashboardActivity(
                preferredDate(job.completedAt(), job.confirmedAt(), job.requestedAt()),
                "In-place restore",
                job.sourceInstanceCode(),
                value(job.publicId(), "In-place restore"),
                job.status().getLabel(),
                job.status().getBadgeClass(),
                value(job.lastError(), value(job.backupPublicId(), "Awaiting operational review")),
                "/control-center/restores/in-place/" + job.id()
        )));
        lifecycleJobs.stream().limit(10).forEach(job -> activities.add(new Matrix26OperationsDashboardActivity(
                preferredDate(job.completedAt(), job.startedAt(), job.requestedAt()),
                "Lifecycle",
                job.instanceCode(),
                job.action().getLabel(),
                job.status().getLabel(),
                job.status().getBadgeClass(),
                value(job.lastError(), value(job.reason(), "Lifecycle job")),
                "/control-center/lifecycle/jobs/" + job.id()
        )));
        decommissionJobs.stream().limit(10).forEach(job -> activities.add(new Matrix26OperationsDashboardActivity(
                preferredDate(job.completedAt(), job.startedAt(), job.requestedAt()),
                "Decommission",
                job.instanceCode(),
                value(job.publicId(), "Decommission job"),
                job.status().getLabel(),
                job.status().getBadgeClass(),
                value(job.lastError(), value(job.reason(), "Final archive protected")),
                "/control-center/lifecycle/decommission/" + job.id()
        )));
        purgePlans.stream().limit(10).forEach(plan -> activities.add(new Matrix26OperationsDashboardActivity(
                preferredDate(plan.evaluatedAt(), plan.requestedAt(), null),
                "Purge",
                plan.instanceCode(),
                value(plan.publicId(), "Purge plan"),
                label(plan.status()),
                badge(plan.status()),
                value(plan.lastError(), plan.wouldDelete() + " would delete, " + plan.protectedResources() + " protected"),
                "/control-center/purge/" + plan.id()
        )));
        destructionPlans.stream().limit(10).forEach(plan -> activities.add(new Matrix26OperationsDashboardActivity(
                preferredDate(plan.evaluatedAt(), plan.requestedAt(), null),
                "Archive destruction",
                plan.instanceCode(),
                value(plan.publicId(), "Archive destruction plan"),
                label(plan.status()),
                badge(plan.status()),
                value(plan.lastError(), plan.wouldDelete() + " would delete, " + plan.protectedResources() + " protected"),
                "/control-center/purge/archive-destruction/" + plan.id()
        )));
        runtimeOperations.stream().limit(10).forEach(operation -> activities.add(new Matrix26OperationsDashboardActivity(
                preferredDate(operation.getCompletedAt(), operation.getStartedAt(), operation.getRequestedAt()),
                "Runtime",
                operation.getInstanceCode(),
                operation.getAction().getLabel(),
                operation.getStatus().getLabel(),
                operation.getStatus().getBadgeClass(),
                value(operation.getMessage(), value(operation.getErrorDetail(), operation.getFinalState())),
                "/control-center/operations/runtimes/" + operation.getRuntimeKey()
        )));
        activities.sort(Comparator.comparing(Matrix26OperationsDashboardActivity::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return activities.stream().limit(18).toList();
    }

    private List<Matrix26OperationsDashboardInstance> instanceHealth(
            List<PlatformBusinessClient> instances,
            Matrix26OperationsSnapshot snapshot
    ) {
        Map<Long, Matrix26RuntimeInventoryItem> runtimesByInstance = new LinkedHashMap<>();
        for (Matrix26RuntimeInventoryItem runtime : snapshot.runtimes()) {
            if (runtime.target().instanceId() != null) {
                runtimesByInstance.put(runtime.target().instanceId(), runtime);
            }
        }
        return instances.stream()
                .map(instance -> instanceHealth(instance, runtimesByInstance.get(instance.getId())))
                .sorted(Comparator.comparing(Matrix26OperationsDashboardInstance::attentionLevel).thenComparing(Matrix26OperationsDashboardInstance::name, String.CASE_INSENSITIVE_ORDER))
                .limit(20)
                .toList();
    }

    private Matrix26OperationsDashboardInstance instanceHealth(PlatformBusinessClient instance, Matrix26RuntimeInventoryItem runtime) {
        String level = "SUCCESS";
        String detail = "No operational issue detected in the latest dashboard snapshot.";
        if (instance.isProtectedInstance()) {
            level = "INFO";
            detail = "Protected instance. Runtime actions and destructive operations remain restricted.";
        }
        if (runtime != null && runtime.state() != Matrix26RuntimeState.ONLINE && runtime.state() != Matrix26RuntimeState.OFFLINE) {
            level = runtime.state() == Matrix26RuntimeState.PORT_OCCUPIED || runtime.state() == Matrix26RuntimeState.RUNTIME_MISSING ? "CRITICAL" : "WARNING";
            detail = runtime.stateDetail();
        }
        if (same(instance.getStatus(), "DECOMMISSIONED")) {
            level = "WARNING";
            detail = "Instance is decommissioned and must remain out of active runtime control.";
        }
        if (same(instance.getStatus(), "PURGED") || same(instance.getRuntimeStatus(), "PURGED")) {
            level = "INFO";
            detail = "Operational resources were purged while central metadata remains preserved.";
        }
        return new Matrix26OperationsDashboardInstance(
                instance.getCode(),
                instance.getBusinessName(),
                value(instance.getStatus(), "UNKNOWN"),
                value(instance.getRuntimeStatus(), "UNKNOWN"),
                value(instance.getDatabaseName(), "—"),
                instance.getRuntimePort(),
                instance.isProtectedInstance(),
                level,
                detail,
                "/control-center/instances/" + instance.getId()
        );
    }

    private <T> T safe(String area, Supplier<T> supplier, T fallback, List<Matrix26OperationsDashboardAlert> alerts) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (RuntimeException ex) {
            alerts.add(new Matrix26OperationsDashboardAlert(
                    "WARNING",
                    "bi-exclamation-triangle",
                    area + " unavailable",
                    limit(ex.getMessage(), 240),
                    "/control-center/operations/dashboard",
                    "Refresh"
            ));
            return fallback;
        }
    }

    private String tone(long issueCount) {
        return issueCount > 0 ? "warning" : "success";
    }

    private boolean same(String value, String expected) {
        return value != null && value.equalsIgnoreCase(expected);
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private LocalDateTime preferredDate(LocalDateTime first, LocalDateTime second, LocalDateTime third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private String label(Enum<?> status) {
        if (status == null) {
            return "Unknown";
        }
        return status.name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private String badge(Enum<?> status) {
        if (status == Matrix26PurgeStatus.PURGED || status == Matrix26ArchiveDestructionStatus.DESTROYED) {
            return "text-bg-success";
        }
        if (status == Matrix26PurgeStatus.BLOCKED || status == Matrix26ArchiveDestructionStatus.BLOCKED) {
            return "text-bg-warning";
        }
        if (status == Matrix26PurgeStatus.FAILED || status == Matrix26ArchiveDestructionStatus.FAILED || status == Matrix26ArchiveDestructionStatus.PARTIALLY_DESTROYED) {
            return "text-bg-danger";
        }
        if (status == Matrix26PurgeStatus.READY_TO_PURGE || status == Matrix26PurgeStatus.DRY_RUN_READY || status == Matrix26ArchiveDestructionStatus.READY_FOR_REVIEW || status == Matrix26ArchiveDestructionStatus.APPROVED_FOR_DESTRUCTION) {
            return "text-bg-primary";
        }
        return "text-bg-secondary";
    }

    private String limit(String value, int length) {
        if (value == null || value.length() <= length) {
            return value;
        }
        return value.substring(0, length - 1) + "…";
    }
}
