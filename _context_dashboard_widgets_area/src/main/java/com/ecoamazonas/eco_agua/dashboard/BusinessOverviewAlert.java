package com.ecoamazonas.eco_agua.dashboard;

public class BusinessOverviewAlert {

    private final String severity;
    private final String title;
    private final String message;
    private final String actionLabel;
    private final String actionUrl;

    public BusinessOverviewAlert(
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
}
