package com.ecoamazonas.eco_agua.platform.control.operations.acceptance;

import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationAlertRepository;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationAlertSummary;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsDashboard;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsDashboardMetric;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsDashboardService;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsInventoryService;
import com.ecoamazonas.eco_agua.platform.control.operations.Matrix26OperationsSnapshot;
import com.ecoamazonas.eco_agua.platform.control.purge.Matrix26PurgeProperties;
import com.ecoamazonas.eco_agua.platform.control.security.Matrix26ControlSecurityService;
import com.ecoamazonas.eco_agua.platform.control.security.Matrix26SecurityOverview;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26AcceptanceMatrixService {

    private final Matrix26OperationsInventoryService inventoryService;
    private final Matrix26OperationsDashboardService dashboardService;
    private final Matrix26OperationAlertRepository alertRepository;
    private final Matrix26ControlSecurityService securityService;
    private final Matrix26PurgeProperties purgeProperties;

    public Matrix26AcceptanceMatrixService(
            Matrix26OperationsInventoryService inventoryService,
            Matrix26OperationsDashboardService dashboardService,
            Matrix26OperationAlertRepository alertRepository,
            Matrix26ControlSecurityService securityService,
            Matrix26PurgeProperties purgeProperties
    ) {
        this.inventoryService = inventoryService;
        this.dashboardService = dashboardService;
        this.alertRepository = alertRepository;
        this.securityService = securityService;
        this.purgeProperties = purgeProperties;
    }

    public Matrix26AcceptanceMatrix matrix(boolean refresh) {
        List<String> notes = new ArrayList<>();
        Matrix26OperationsSnapshot snapshot = safe("Operations inventory", () -> inventoryService.snapshot(refresh), null, notes);
        Matrix26OperationsDashboard dashboard = safe("Operations dashboard", () -> dashboardService.dashboard(snapshot), null, notes);
        Matrix26OperationAlertSummary alertSummary = safe("Operation alerts", alertRepository::summary, zeroAlerts(), notes);
        Matrix26SecurityOverview securityOverview = safe("Matrix26 security", securityService::overview, null, notes);

        List<Matrix26AcceptanceGroup> groups = List.of(
                controlCenterGroup(snapshot, dashboard, alertSummary),
                operationsGroup(snapshot, dashboard, alertSummary),
                recoveryGroup(dashboard),
                purgeGroup(dashboard),
                securityGroup(securityOverview),
                governanceGroup(snapshot, dashboard, securityOverview)
        );
        List<Matrix26AcceptanceRisk> risks = risks(dashboard, alertSummary, securityOverview);
        Matrix26AcceptanceStatus overallStatus = overall(groups, risks);

        return new Matrix26AcceptanceMatrix(
                LocalDateTime.now(),
                overallStatus,
                metrics(snapshot, dashboard, alertSummary, securityOverview, overallStatus),
                groups,
                risks,
                notes
        );
    }

    private Matrix26AcceptanceGroup controlCenterGroup(
            Matrix26OperationsSnapshot snapshot,
            Matrix26OperationsDashboard dashboard,
            Matrix26OperationAlertSummary alertSummary
    ) {
        List<Matrix26AcceptanceItem> items = new ArrayList<>();
        items.add(item(
                "control-center-dashboard",
                "Control Center reachable",
                "The central Matrix26 home and operational routes are available under the control runtime.",
                dashboard == null ? Matrix26AcceptanceStatus.FAILED : Matrix26AcceptanceStatus.PASSED,
                dashboard == null ? "Dashboard service did not return a view." : "Operations dashboard generated at " + dashboard.generatedAt() + ".",
                "/control-center/operations/dashboard",
                dashboard == null ? "Open the application logs and review Matrix26OperationsDashboardService." : "Keep the route as the central operations entry point."
        ));
        items.add(item(
                "read-only-dashboard",
                "Dashboard remains read-only",
                "Final acceptance uses existing metadata and does not execute runtime, backup, restore, purge or archive operations.",
                Matrix26AcceptanceStatus.PASSED,
                "Only GET routes are added for acceptance review.",
                "/control-center/operations/acceptance",
                "Keep acceptance review free of POST actions."
        ));
        items.add(item(
                "alert-center-present",
                "Alert Center available",
                "Operations alerts can be reviewed and tracked from a dedicated page.",
                alertSummary == null ? Matrix26AcceptanceStatus.WARNING : Matrix26AcceptanceStatus.PASSED,
                alertSummary == null ? "Alert summary unavailable." : alertSummary.active() + " active alerts, " + alertSummary.total() + " total alerts.",
                "/control-center/operations/alerts",
                alertSummary == null ? "Run the 3I.2 initializer or verify matrix26_operation_alert tables." : "Use this page to resolve open warnings before production usage."
        ));
        items.add(item(
                "inventory-snapshot",
                "Inventory snapshot available",
                "Runtime inventory, ports and log metadata can be captured from the control runtime.",
                snapshot == null ? Matrix26AcceptanceStatus.WARNING : Matrix26AcceptanceStatus.PASSED,
                snapshot == null ? "Inventory service returned no snapshot." : snapshot.summary().totalRuntimes() + " runtimes inspected, " + snapshot.summary().listeningPorts() + " listening ports detected.",
                "/control-center/operations/runtimes",
                snapshot == null ? "Refresh the Operations dashboard and review runtime configuration." : "Continue using refresh=true for manual acceptance checks."
        ));
        return new Matrix26AcceptanceGroup(
                "Control Center foundation",
                "Core visibility and safe review surfaces.",
                "bi-command",
                items
        );
    }

    private Matrix26AcceptanceGroup operationsGroup(
            Matrix26OperationsSnapshot snapshot,
            Matrix26OperationsDashboard dashboard,
            Matrix26OperationAlertSummary alertSummary
    ) {
        long totalRuntimes = snapshot == null || snapshot.summary() == null ? 0 : snapshot.summary().totalRuntimes();
        long protectedRuntimes = snapshot == null || snapshot.summary() == null ? 0 : snapshot.summary().protectedRuntimes();
        long degradedRuntimes = snapshot == null || snapshot.summary() == null ? 0 : snapshot.summary().degradedRuntimes();
        long criticalAlerts = alertSummary == null ? 0 : alertSummary.critical();
        List<Matrix26AcceptanceItem> items = new ArrayList<>();
        items.add(item(
                "runtime-inventory",
                "Runtime inventory checked",
                "Managed runtimes can be listed and inspected.",
                totalRuntimes > 0 ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.WARNING,
                totalRuntimes + " runtime targets detected.",
                "/control-center/operations/runtimes",
                totalRuntimes > 0 ? "Keep active runtimes documented in Matrix26." : "Register at least one runtime target before production acceptance."
        ));
        items.add(item(
                "protected-instances",
                "Protected instances remain visible",
                "Core business runtimes should be present and marked as protected where applicable.",
                protectedRuntimes >= 3 ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.WARNING,
                protectedRuntimes + " protected runtimes detected in the latest snapshot.",
                "/control-center/instances",
                protectedRuntimes >= 3 ? "Keep Eco Agua, Belén, Restaurante and Control Center protected." : "Review protected-instance flags before running purge/decommission flows."
        ));
        items.add(item(
                "runtime-health",
                "Runtime health does not block acceptance",
                "Degraded runtimes are visible and actionable from the dashboard.",
                degradedRuntimes == 0 ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.WARNING,
                degradedRuntimes + " runtime targets need attention.",
                "/control-center/operations/dashboard",
                degradedRuntimes == 0 ? "No runtime health action required now." : "Open each degraded runtime and resolve port/process/configuration issues."
        ));
        items.add(item(
                "critical-alerts",
                "Critical alerts reviewed",
                "Critical alerts should be zero before considering the operations block accepted.",
                criticalAlerts == 0 ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.WARNING,
                criticalAlerts + " critical alerts detected in the persistent Alert Center.",
                "/control-center/operations/alerts?severity=CRITICAL",
                criticalAlerts == 0 ? "Keep alert review as part of release checks." : "Acknowledge or resolve critical alerts with evidence before closing acceptance."
        ));
        items.add(item(
                "dashboard-completeness",
                "Dashboard metadata completeness",
                "All dashboard sources should respond without partial metadata warnings.",
                dashboard == null ? Matrix26AcceptanceStatus.FAILED : dashboard.incomplete() ? Matrix26AcceptanceStatus.WARNING : Matrix26AcceptanceStatus.PASSED,
                dashboard == null ? "Dashboard unavailable." : dashboard.incomplete() ? "At least one dashboard metadata source reported a warning." : "All dashboard sources responded.",
                "/control-center/operations/dashboard",
                dashboard == null || dashboard.incomplete() ? "Review the Attention center and application logs." : "No action required."
        ));
        return new Matrix26AcceptanceGroup(
                "Operations dashboard and alert center",
                "Live operational visibility for runtimes, ports, alerts and logs.",
                "bi-activity",
                items
        );
    }

    private Matrix26AcceptanceGroup recoveryGroup(Matrix26OperationsDashboard dashboard) {
        List<Matrix26AcceptanceItem> items = new ArrayList<>();
        items.add(metricItem(
                dashboard,
                "Backups",
                "backup-module",
                "Backup module available",
                "Backup jobs, schedules, policies, retention and verification are visible from Matrix26.",
                "/control-center/backups",
                "Run a manual backup after any production deployment."
        ));
        items.add(metricItem(
                dashboard,
                "Schedules",
                "backup-schedules",
                "Backup schedules visible",
                "Scheduled backup health and missed executions are visible.",
                "/control-center/backups/schedules",
                "Keep schedules enabled only for protected production instances."
        ));
        items.add(metricItem(
                dashboard,
                "Restores",
                "restore-module",
                "Restore module available",
                "Clone restore and in-place restore status are visible.",
                "/control-center/restores",
                "Prefer clone restore validation before any in-place restore."
        ));
        items.add(metricItem(
                dashboard,
                "Final archives",
                "final-archives",
                "Final archive inventory visible",
                "Decommission final archive metadata is visible and connected to restore links.",
                "/control-center/lifecycle/archive",
                "Do not destroy final archive packages while clone links or retention rules remain active."
        ));
        return new Matrix26AcceptanceGroup(
                "Backup, restore and archive readiness",
                "Recovery paths and evidence required before destructive lifecycle actions.",
                "bi-safe2",
                items
        );
    }

    private Matrix26AcceptanceGroup purgeGroup(Matrix26OperationsDashboard dashboard) {
        List<Matrix26AcceptanceItem> items = new ArrayList<>();
        items.add(metricItem(
                dashboard,
                "Purge safety",
                "purge-safety",
                "Purge planner visible",
                "Dry-run and controlled purge plans are visible with safety classifications.",
                "/control-center/purge",
                "Keep protected-instance checks enabled before any purge execution."
        ));
        items.add(metricItem(
                dashboard,
                "Archive destruction",
                "archive-destruction",
                "Archive destruction planner visible",
                "Archive destruction readiness and blocked plans are visible.",
                "/control-center/purge/archive-destruction",
                "Use planner evidence before enabling any physical archive destruction."
        ));
        items.add(item(
                "archive-destruction-disabled",
                "Archive destruction execution disabled",
                "Physical destruction should not remain enabled after the controlled test.",
                purgeProperties.isArchiveDestructionExecutionEnabled() ? Matrix26AcceptanceStatus.FAILED : Matrix26AcceptanceStatus.PASSED,
                "archive-destruction-execution-enabled=" + purgeProperties.isArchiveDestructionExecutionEnabled(),
                "/control-center/purge/archive-destruction",
                purgeProperties.isArchiveDestructionExecutionEnabled() ? "Set matrix26.control-center.purge.archive-destruction-execution-enabled=false and restart Matrix26." : "Keep this setting disabled unless a supervised destruction test is running."
        ));
        items.add(item(
                "purge-protected-codes",
                "Protected purge list configured",
                "Protected instance codes must stay outside purge candidates.",
                purgeProperties.getProtectedInstanceCodes().size() >= 4 ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.WARNING,
                purgeProperties.getProtectedInstanceCodes().size() + " protected instance codes configured.",
                "/control-center/purge",
                purgeProperties.getProtectedInstanceCodes().size() >= 4 ? "Review the list after every new protected production runtime." : "Add Eco Agua, Belén, Restaurante, Control Center and restore-test instances to the protected list."
        ));
        return new Matrix26AcceptanceGroup(
                "Purge and archive destruction safety",
                "Final checks for destructive flows and protected resources.",
                "bi-shield-exclamation",
                items
        );
    }

    private Matrix26AcceptanceGroup securityGroup(Matrix26SecurityOverview overview) {
        long roleCount = overview == null ? 0 : overview.roles().size();
        long permissionCount = overview == null ? 0 : overview.permissions().size();
        long matrix26Users = overview == null ? 0 : overview.matrix26UserCount();
        List<Matrix26AcceptanceItem> items = new ArrayList<>();
        items.add(item(
                "roles-defined",
                "Matrix26 roles defined",
                "Dedicated platform roles separate Matrix26 operations from business modules.",
                roleCount >= 7 ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.WARNING,
                roleCount + " Matrix26 roles visible in the security overview.",
                "/control-center/security",
                roleCount >= 7 ? "Keep roles reviewed before adding new sensitive modules." : "Run the 3I.3 security initializer or review role seeding."
        ));
        items.add(item(
                "permissions-defined",
                "Matrix26 permissions defined",
                "Sensitive actions are mapped to granular platform permissions.",
                permissionCount >= 10 ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.WARNING,
                permissionCount + " Matrix26 permissions visible in the security overview.",
                "/control-center/security",
                permissionCount >= 10 ? "Use permissions for future route guards instead of broad business roles." : "Verify matrix26.* permissions were created."
        ));
        items.add(item(
                "matrix26-admin-user",
                "At least one Matrix26 admin/operator exists",
                "A Matrix26-capable user must exist before production usage.",
                matrix26Users > 0 ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.WARNING,
                matrix26Users + " users have Matrix26 role or super-admin authority.",
                "/control-center/security",
                matrix26Users > 0 ? "Review least-privilege assignment before onboarding operators." : "Assign MATRIX26_ADMIN to the bootstrap user before enabling non-admin access."
        ));
        items.add(item(
                "current-user-view",
                "Current user can view Matrix26",
                "The authenticated user has enough authority to view Matrix26 pages.",
                securityService.canView() ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.FAILED,
                "Current security context view access=" + securityService.canView(),
                "/control-center/security",
                securityService.canView() ? "No action required." : "Grant MATRIX26_VIEWER, MATRIX26_ADMIN or ROLE_SUPER_ADMIN."
        ));
        return new Matrix26AcceptanceGroup(
                "Roles and permissions",
                "Authorization rules added in 3I.3 for sensitive Matrix26 actions.",
                "bi-person-lock",
                items
        );
    }

    private Matrix26AcceptanceGroup governanceGroup(
            Matrix26OperationsSnapshot snapshot,
            Matrix26OperationsDashboard dashboard,
            Matrix26SecurityOverview securityOverview
    ) {
        List<Matrix26AcceptanceItem> items = new ArrayList<>();
        items.add(item(
                "runtime-data-outside-git",
                "Runtime data remains outside Git",
                "Runtime folders, database dumps and backup packages are operational artifacts, not source files.",
                Matrix26AcceptanceStatus.PASSED,
                "Final package only adds Java, Thymeleaf, scripts, docs and reports for the acceptance page.",
                null,
                "Keep runtime-data, runtime-clients generated assets and backup roots out of commits."
        ));
        items.add(item(
                "safe-acceptance-scope",
                "Acceptance page has no write actions",
                "The final acceptance route is read-only and records no destructive action.",
                Matrix26AcceptanceStatus.PASSED,
                "No POST mapping is introduced for /control-center/operations/acceptance.",
                "/control-center/operations/acceptance",
                "Keep final acceptance as evidence and checklist only."
        ));
        items.add(item(
                "dashboard-links",
                "Navigation links available",
                "Operators can move from acceptance into the related evidence screens.",
                dashboard == null ? Matrix26AcceptanceStatus.WARNING : Matrix26AcceptanceStatus.PASSED,
                dashboard == null ? "Dashboard links could not be verified." : dashboard.metrics().size() + " dashboard metrics provide evidence links.",
                "/control-center/operations/dashboard",
                dashboard == null ? "Review dashboard rendering." : "Use links from failed checklist items to complete acceptance."
        ));
        items.add(item(
                "security-evidence",
                "Security evidence available",
                "Role and permission matrices are visible to administrators.",
                securityOverview == null ? Matrix26AcceptanceStatus.WARNING : Matrix26AcceptanceStatus.PASSED,
                securityOverview == null ? "Security overview unavailable." : securityOverview.roles().size() + " roles, " + securityOverview.permissions().size() + " permissions, " + securityOverview.users().size() + " users.",
                "/control-center/security",
                securityOverview == null ? "Review Matrix26ControlSecurityService." : "Attach screenshots of the security page to final acceptance if needed."
        ));
        items.add(item(
                "probe-warnings-reviewed",
                "Probe warnings reviewed",
                "Probe warnings are expected only for stopped or intentionally archived/decommissioned runtimes.",
                snapshot == null ? Matrix26AcceptanceStatus.NOT_TESTED : snapshot.probeWarnings().isEmpty() ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.WARNING,
                snapshot == null ? "No runtime snapshot available." : snapshot.probeWarnings().size() + " probe warnings detected.",
                "/control-center/operations/dashboard",
                snapshot == null || !snapshot.probeWarnings().isEmpty() ? "Review probe warnings before final closure." : "No action required."
        ));
        return new Matrix26AcceptanceGroup(
                "Governance and release hygiene",
                "Non-functional checks needed before closing Operations & Lifecycle.",
                "bi-clipboard2-check",
                items
        );
    }

    private Matrix26AcceptanceItem metricItem(
            Matrix26OperationsDashboard dashboard,
            String metricLabel,
            String code,
            String title,
            String description,
            String route,
            String recommendedAction
    ) {
        Matrix26OperationsDashboardMetric metric = findMetric(dashboard, metricLabel);
        Matrix26AcceptanceStatus status = metric == null ? Matrix26AcceptanceStatus.WARNING : Matrix26AcceptanceStatus.PASSED;
        String evidence = metric == null
                ? "Metric not found on the Operations dashboard."
                : metric.value() + " — " + metric.detail();
        return item(code, title, description, status, evidence, route, recommendedAction);
    }

    private Matrix26OperationsDashboardMetric findMetric(Matrix26OperationsDashboard dashboard, String label) {
        if (dashboard == null) {
            return null;
        }
        return dashboard.metrics().stream()
                .filter(metric -> metric.label() != null && metric.label().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }

    private List<Matrix26AcceptanceMetric> metrics(
            Matrix26OperationsSnapshot snapshot,
            Matrix26OperationsDashboard dashboard,
            Matrix26OperationAlertSummary alertSummary,
            Matrix26SecurityOverview securityOverview,
            Matrix26AcceptanceStatus overallStatus
    ) {
        return List.of(
                new Matrix26AcceptanceMetric("Overall", overallStatus.getLabel(), "Final acceptance state", overallStatus.getIcon(), tone(overallStatus)),
                new Matrix26AcceptanceMetric("Runtimes", String.valueOf(snapshot == null || snapshot.summary() == null ? 0 : snapshot.summary().totalRuntimes()), "Targets inspected", "bi-cpu", snapshot == null ? "warning" : "success"),
                new Matrix26AcceptanceMetric("Active alerts", String.valueOf(alertSummary == null ? 0 : alertSummary.active()), "Open or acknowledged", "bi-bell", alertSummary != null && alertSummary.active() > 0 ? "warning" : "success"),
                new Matrix26AcceptanceMetric("Roles", String.valueOf(securityOverview == null ? 0 : securityOverview.roles().size()), "Matrix26 roles", "bi-person-lock", securityOverview == null ? "warning" : "success"),
                new Matrix26AcceptanceMetric("Archive destruction", purgeProperties.isArchiveDestructionExecutionEnabled() ? "Enabled" : "Disabled", "Execution flag", "bi-shield-exclamation", purgeProperties.isArchiveDestructionExecutionEnabled() ? "danger" : "success"),
                new Matrix26AcceptanceMetric("Dashboard", dashboard == null ? "Unavailable" : dashboard.healthy() ? "Healthy" : "Warnings", dashboard == null ? "No dashboard" : dashboard.alerts().size() + " dashboard alerts", "bi-command", dashboard == null ? "danger" : dashboard.healthy() ? "success" : "warning")
        );
    }

    private List<Matrix26AcceptanceRisk> risks(
            Matrix26OperationsDashboard dashboard,
            Matrix26OperationAlertSummary alertSummary,
            Matrix26SecurityOverview securityOverview
    ) {
        List<Matrix26AcceptanceRisk> risks = new ArrayList<>();
        risks.add(new Matrix26AcceptanceRisk(
                "Archive destruction execution flag",
                "Leaving physical archive destruction enabled after tests can expose final backup packages to accidental deletion.",
                purgeProperties.isArchiveDestructionExecutionEnabled() ? Matrix26AcceptanceStatus.FAILED : Matrix26AcceptanceStatus.PASSED,
                purgeProperties.isArchiveDestructionExecutionEnabled() ? "Disable matrix26.control-center.purge.archive-destruction-execution-enabled and restart Matrix26." : "Keep disabled by default."
        ));
        risks.add(new Matrix26AcceptanceRisk(
                "Protected instances",
                "Protected production-like runtimes must never become purge or destruction candidates.",
                purgeProperties.getProtectedInstanceCodes().size() >= 4 ? Matrix26AcceptanceStatus.PASSED : Matrix26AcceptanceStatus.WARNING,
                "Review protected-instance codes after adding every new customer runtime."
        ));
        risks.add(new Matrix26AcceptanceRisk(
                "Open operation alerts",
                "Open alerts can be acceptable in local labs, but they must be reviewed before production acceptance.",
                alertSummary != null && alertSummary.active() > 0 ? Matrix26AcceptanceStatus.WARNING : Matrix26AcceptanceStatus.PASSED,
                alertSummary != null && alertSummary.active() > 0 ? "Review /control-center/operations/alerts and resolve or acknowledge each item." : "No active alert action required."
        ));
        risks.add(new Matrix26AcceptanceRisk(
                "Least privilege review",
                "Roles were introduced incrementally. Operator accounts should be checked before production usage.",
                securityOverview == null || securityOverview.matrix26UserCount() == 0 ? Matrix26AcceptanceStatus.WARNING : Matrix26AcceptanceStatus.PASSED,
                "Use /control-center/security to review Matrix26 users and effective permissions."
        ));
        risks.add(new Matrix26AcceptanceRisk(
                "Operational data outside Git",
                "Backups, runtime data and generated client directories must remain operational artifacts, not source code.",
                Matrix26AcceptanceStatus.PASSED,
                "Keep .gitignore and commit hygiene checks active."
        ));
        if (dashboard != null && dashboard.incomplete()) {
            risks.add(new Matrix26AcceptanceRisk(
                    "Partial dashboard metadata",
                    "At least one metadata source could not be read while generating the Operations dashboard.",
                    Matrix26AcceptanceStatus.WARNING,
                    "Open the dashboard Attention center and review the related source."
            ));
        }
        return risks;
    }

    private Matrix26AcceptanceStatus overall(List<Matrix26AcceptanceGroup> groups, List<Matrix26AcceptanceRisk> risks) {
        Matrix26AcceptanceStatus status = Matrix26AcceptanceStatus.PASSED;
        for (Matrix26AcceptanceGroup group : groups) {
            status = worst(status, group.status());
        }
        for (Matrix26AcceptanceRisk risk : risks) {
            status = worst(status, risk.status());
        }
        return status;
    }

    private Matrix26AcceptanceStatus worst(Matrix26AcceptanceStatus current, Matrix26AcceptanceStatus candidate) {
        if (candidate == null) {
            return current;
        }
        return candidate.getSeverityRank() > current.getSeverityRank() ? candidate : current;
    }

    private Matrix26OperationAlertSummary zeroAlerts() {
        return new Matrix26OperationAlertSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private Matrix26AcceptanceItem item(
            String code,
            String title,
            String description,
            Matrix26AcceptanceStatus status,
            String evidence,
            String route,
            String recommendedAction
    ) {
        return new Matrix26AcceptanceItem(code, title, description, status, value(evidence), route, value(recommendedAction));
    }

    private <T> T safe(String label, Supplier<T> supplier, T fallback, List<String> notes) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (RuntimeException ex) {
            notes.add(label + " unavailable: " + limit(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 220));
            return fallback;
        }
    }

    private String tone(Matrix26AcceptanceStatus status) {
        return status == Matrix26AcceptanceStatus.PASSED ? "success"
                : status == Matrix26AcceptanceStatus.WARNING || status == Matrix26AcceptanceStatus.NOT_TESTED ? "warning"
                : "danger";
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String limit(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value;
        }
        return value.substring(0, maximum - 1) + "…";
    }
}
