package com.ecoamazonas.eco_agua.platform.control.operations;

import java.time.LocalDateTime;

public record Matrix26OperationAlert(
        Long id,
        String sourceKey,
        Matrix26OperationAlertSource source,
        Matrix26OperationAlertSeverity severity,
        Matrix26OperationAlertStatus status,
        String instanceCode,
        String title,
        String message,
        String href,
        String actionLabel,
        LocalDateTime firstDetectedAt,
        LocalDateTime lastDetectedAt,
        Integer detectCount,
        LocalDateTime acknowledgedAt,
        String acknowledgedBy,
        LocalDateTime resolvedAt,
        String resolvedBy,
        LocalDateTime ignoredAt,
        String ignoredBy,
        String resolutionNotes
) {
    public int detections() {
        return detectCount == null ? 0 : detectCount;
    }

    public boolean openLike() {
        return status == Matrix26OperationAlertStatus.OPEN || status == Matrix26OperationAlertStatus.ACKNOWLEDGED;
    }

    public String sourceLabel() {
        return source == null ? "System" : source.getLabel();
    }

    public String sourceIcon() {
        return source == null ? "bi-command" : source.getIconClass();
    }
}
