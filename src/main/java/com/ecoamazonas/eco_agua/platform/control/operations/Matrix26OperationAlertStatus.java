package com.ecoamazonas.eco_agua.platform.control.operations;

public enum Matrix26OperationAlertStatus {
    OPEN("Open", "text-bg-danger"),
    ACKNOWLEDGED("Acknowledged", "text-bg-primary"),
    RESOLVED("Resolved", "text-bg-success"),
    IGNORED("Ignored", "text-bg-secondary");

    private final String label;
    private final String badgeClass;

    Matrix26OperationAlertStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
