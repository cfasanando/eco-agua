package com.ecoamazonas.eco_agua.dashboard;

public class ManagementDashboardAlert {

    private final String severity;
    private final String title;
    private final String message;
    private final String actionLabel;
    private final String actionUrl;

    public ManagementDashboardAlert(
            String severity,
            String title,
            String message,
            String actionLabel,
            String actionUrl
    ) {
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.actionLabel = actionLabel;
        this.actionUrl = actionUrl;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public String getAlertClass() {
        if ("danger".equalsIgnoreCase(severity)) {
            return "alert-danger";
        }
        if ("warning".equalsIgnoreCase(severity)) {
            return "alert-warning";
        }
        if ("success".equalsIgnoreCase(severity)) {
            return "alert-success";
        }
        return "alert-info";
    }

    public String getBadgeClass() {
        if ("danger".equalsIgnoreCase(severity)) {
            return "text-bg-danger";
        }
        if ("warning".equalsIgnoreCase(severity)) {
            return "text-bg-warning";
        }
        if ("success".equalsIgnoreCase(severity)) {
            return "text-bg-success";
        }
        return "text-bg-info";
    }

    public boolean hasAction() {
        return actionUrl != null && !actionUrl.isBlank() && actionLabel != null && !actionLabel.isBlank();
    }
}
