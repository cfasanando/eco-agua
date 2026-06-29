package com.ecoamazonas.eco_agua.platform.control.operations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26OperationAlertService {
    private static final String SYSTEM_ACTOR = "matrix26-alert-sync";

    private final Matrix26OperationsInventoryService inventoryService;
    private final Matrix26OperationsDashboardService dashboardService;
    private final Matrix26OperationAlertRepository alertRepository;

    public Matrix26OperationAlertService(
            Matrix26OperationsInventoryService inventoryService,
            Matrix26OperationsDashboardService dashboardService,
            Matrix26OperationAlertRepository alertRepository
    ) {
        this.inventoryService = inventoryService;
        this.dashboardService = dashboardService;
        this.alertRepository = alertRepository;
    }

    public Matrix26OperationAlertCenterView alertCenter(
            boolean includeClosed,
            Matrix26OperationAlertStatus status,
            Matrix26OperationAlertSeverity severity,
            Matrix26OperationAlertSource source,
            boolean refresh
    ) {
        String syncMessage = synchronize(refresh);
        return new Matrix26OperationAlertCenterView(
                alertRepository.summary(),
                alertRepository.findAlerts(includeClosed, status, severity, source),
                LocalDateTime.now(),
                syncMessage != null,
                syncMessage
        );
    }

    public Matrix26OperationAlert alert(long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new Matrix26RuntimeControlException("Matrix26 operation alert was not found."));
    }

    public List<Matrix26OperationAlertEvent> events(long id) {
        return alertRepository.events(id);
    }

    public void acknowledge(long id, String actor, String note) {
        Matrix26OperationAlert alert = alert(id);
        alertRepository.acknowledge(id, actor, cleanNote(note, "Alert acknowledged."));
        alertRepository.addEvent(id, "ACKNOWLEDGE", Matrix26OperationAlertStatus.ACKNOWLEDGED, actor, cleanNote(note, "Alert acknowledged."));
    }

    public void resolve(long id, String actor, String note) {
        Matrix26OperationAlert alert = alert(id);
        alertRepository.resolve(id, actor, cleanNote(note, "Alert resolved."));
        alertRepository.addEvent(id, "RESOLVE", Matrix26OperationAlertStatus.RESOLVED, actor, cleanNote(note, "Alert resolved."));
    }

    public void ignore(long id, String actor, String note) {
        Matrix26OperationAlert alert = alert(id);
        alertRepository.ignore(id, actor, cleanNote(note, "Alert ignored after administrative review."));
        alertRepository.addEvent(id, "IGNORE", Matrix26OperationAlertStatus.IGNORED, actor, cleanNote(note, "Alert ignored after administrative review."));
    }

    public void reopen(long id, String actor, String note) {
        Matrix26OperationAlert alert = alert(id);
        alertRepository.reopen(id, actor, cleanNote(note, "Alert reopened."));
        alertRepository.addEvent(id, "REOPEN", Matrix26OperationAlertStatus.OPEN, actor, cleanNote(note, "Alert reopened."));
    }

    private String synchronize(boolean refresh) {
        try {
            Matrix26OperationsSnapshot snapshot = inventoryService.snapshot(refresh);
            Matrix26OperationsDashboard dashboard = dashboardService.dashboard(snapshot);
            for (Matrix26OperationsDashboardAlert dashboardAlert : dashboard.alerts()) {
                if (dashboardAlert == null || "SUCCESS".equalsIgnoreCase(dashboardAlert.severity())) {
                    continue;
                }
                syncDashboardAlert(dashboardAlert);
            }
            return null;
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            syncSystemFailure(message);
            return "The alert center synchronized with partial metadata: " + limit(message, 220);
        }
    }

    private void syncDashboardAlert(Matrix26OperationsDashboardAlert dashboardAlert) {
        Matrix26OperationAlertSource source = sourceOf(dashboardAlert);
        Matrix26OperationAlertSeverity severity = Matrix26OperationAlertSeverity.fromDashboard(dashboardAlert.severity());
        String instanceCode = instanceCodeFromHref(dashboardAlert.href());
        String sourceKey = sourceKey(source, instanceCode, dashboardAlert.title(), dashboardAlert.href());
        Optional<Matrix26OperationAlert> existing = alertRepository.findBySourceKey(sourceKey);
        if (existing.isPresent()) {
            Matrix26OperationAlert alert = existing.get();
            Matrix26OperationAlertStatus previous = alert.status();
            alertRepository.markDetected(alert.id(), severity, dashboardAlert.title(), dashboardAlert.detail(), dashboardAlert.href(), dashboardAlert.actionLabel());
            if (previous == Matrix26OperationAlertStatus.RESOLVED || previous == Matrix26OperationAlertStatus.IGNORED) {
                alertRepository.addEvent(alert.id(), "REDETECTED", Matrix26OperationAlertStatus.OPEN, SYSTEM_ACTOR, "Alert was detected again after being closed.");
            }
            return;
        }
        long id = alertRepository.insertDetectedAlert(
                sourceKey,
                source,
                severity,
                instanceCode,
                dashboardAlert.title(),
                dashboardAlert.detail(),
                dashboardAlert.href(),
                dashboardAlert.actionLabel()
        );
        if (id > 0) {
            alertRepository.addEvent(id, "DETECTED", Matrix26OperationAlertStatus.OPEN, SYSTEM_ACTOR, "Alert detected from the Matrix26 operations dashboard snapshot.");
        }
    }

    private void syncSystemFailure(String message) {
        Matrix26OperationAlertSource source = Matrix26OperationAlertSource.SYSTEM;
        String title = "Alert synchronization partial failure";
        String sourceKey = sourceKey(source, null, title, "/control-center/operations/alerts");
        Optional<Matrix26OperationAlert> existing = alertRepository.findBySourceKey(sourceKey);
        if (existing.isPresent()) {
            alertRepository.markDetected(existing.get().id(), Matrix26OperationAlertSeverity.MEDIUM, title, message, "/control-center/operations/alerts", "Review alert center");
            return;
        }
        long id = alertRepository.insertDetectedAlert(
                sourceKey,
                source,
                Matrix26OperationAlertSeverity.MEDIUM,
                null,
                title,
                message,
                "/control-center/operations/alerts",
                "Review alert center"
        );
        if (id > 0) {
            alertRepository.addEvent(id, "DETECTED", Matrix26OperationAlertStatus.OPEN, SYSTEM_ACTOR, "Alert synchronization did not complete with all metadata sources.");
        }
    }

    private Matrix26OperationAlertSource sourceOf(Matrix26OperationsDashboardAlert alert) {
        String text = ((alert.title() == null ? "" : alert.title()) + " " + (alert.href() == null ? "" : alert.href())).toLowerCase(Locale.ROOT);
        if (text.contains("backup") || text.contains("/backups")) {
            return Matrix26OperationAlertSource.BACKUP;
        }
        if (text.contains("restore") || text.contains("/restores")) {
            return Matrix26OperationAlertSource.RESTORE;
        }
        if (text.contains("lifecycle") || text.contains("decommission") || text.contains("/lifecycle")) {
            return Matrix26OperationAlertSource.LIFECYCLE;
        }
        if (text.contains("archive") || text.contains("final archive")) {
            return Matrix26OperationAlertSource.ARCHIVE;
        }
        if (text.contains("purge") || text.contains("/purge")) {
            return Matrix26OperationAlertSource.PURGE;
        }
        if (text.contains("runtime") || text.contains("probe") || text.contains("/operations/runtimes") || text.contains("port")) {
            return Matrix26OperationAlertSource.RUNTIME;
        }
        return Matrix26OperationAlertSource.SYSTEM;
    }

    private String instanceCodeFromHref(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        int index = href.lastIndexOf('/');
        if (index < 0 || index == href.length() - 1) {
            return null;
        }
        String value = href.substring(index + 1);
        if (value.contains("?") || value.length() > 80 || value.matches("\\d+")) {
            return null;
        }
        return value.replace('_', '-');
    }

    private String sourceKey(Matrix26OperationAlertSource source, String instanceCode, String title, String href) {
        String raw = source.name() + "|" + clean(instanceCode) + "|" + clean(title) + "|" + clean(href);
        return (source.name() + "-" + hash(raw)).toLowerCase(Locale.ROOT);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value).substring(0, 32);
        } catch (NoSuchAlgorithmException ex) {
            return String.valueOf(Math.abs(raw.hashCode()));
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanNote(String note, String fallback) {
        return note == null || note.isBlank() ? fallback : note.trim();
    }

    private String limit(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value;
        }
        return value.substring(0, maximum);
    }
}
