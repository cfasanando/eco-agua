package com.ecoamazonas.eco_agua.accounting;

public class AccountingControlPanelAlert {

    private final String severity;
    private final String title;
    private final String detail;
    private final String actionUrl;
    private final String actionLabel;

    public AccountingControlPanelAlert(String severity, String title, String detail, String actionUrl, String actionLabel) {
        this.severity = severity;
        this.title = title;
        this.detail = detail;
        this.actionUrl = actionUrl;
        this.actionLabel = actionLabel;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public boolean hasAction() {
        return actionUrl != null && !actionUrl.isBlank() && actionLabel != null && !actionLabel.isBlank();
    }

    public String getAlertClass() {
        return switch (severity) {
            case "danger" -> "alert-danger";
            case "warning" -> "alert-warning";
            case "success" -> "alert-success";
            default -> "alert-info";
        };
    }

    public String getBadgeClass() {
        return switch (severity) {
            case "danger" -> "text-bg-danger";
            case "warning" -> "text-bg-warning";
            case "success" -> "text-bg-success";
            default -> "text-bg-info";
        };
    }

    public String getSeverityLabel() {
        return switch (severity) {
            case "danger" -> "Crítico";
            case "warning" -> "Revisar";
            case "success" -> "Correcto";
            default -> "Información";
        };
    }
}
