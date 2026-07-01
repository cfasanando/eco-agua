package com.ecoamazonas.eco_agua.platform.control.modules.acceptance;

public enum Matrix26FeatureFlagAcceptanceStatus {
    PASSED("Passed", "text-bg-success", "bi-check-circle-fill", 0),
    WARNING("Warning", "text-bg-warning", "bi-exclamation-triangle-fill", 1),
    NOT_TESTED("Not tested", "text-bg-secondary", "bi-dash-circle", 2),
    FAILED("Failed", "text-bg-danger", "bi-x-octagon-fill", 3);

    private final String label;
    private final String badgeClass;
    private final String icon;
    private final int severityRank;

    Matrix26FeatureFlagAcceptanceStatus(String label, String badgeClass, String icon, int severityRank) {
        this.label = label;
        this.badgeClass = badgeClass;
        this.icon = icon;
        this.severityRank = severityRank;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getIcon() {
        return icon;
    }

    public int getSeverityRank() {
        return severityRank;
    }

    public String getCssSuffix() {
        return name().toLowerCase().replace('_', '-');
    }
}
