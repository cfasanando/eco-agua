package com.ecoamazonas.eco_agua.dashboard;

public class AreaDashboardMetric {

    private final String label;
    private final String value;
    private final String helper;
    private final String badgeClass;
    private final String actionLabel;
    private final String actionUrl;

    public AreaDashboardMetric(
            String label,
            String value,
            String helper,
            String badgeClass,
            String actionLabel,
            String actionUrl
    ) {
        this.label = label;
        this.value = value;
        this.helper = helper;
        this.badgeClass = badgeClass;
        this.actionLabel = actionLabel;
        this.actionUrl = actionUrl;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public String getHelper() {
        return helper;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public boolean hasAction() {
        return actionUrl != null && !actionUrl.isBlank();
    }
}
