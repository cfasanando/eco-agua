package com.ecoamazonas.eco_agua.platform.control.restores;

public enum Matrix26RestoreCheckStatus {
    MATCH("Match", "text-bg-success", 0),
    WARNING("Warning", "text-bg-warning", 1),
    MISMATCH("Mismatch", "text-bg-danger", 2),
    NOT_APPLICABLE("Not applicable", "text-bg-secondary", 0),
    FAILED("Failed", "text-bg-danger", 3);

    private final String label;
    private final String badgeClass;
    private final int severity;

    Matrix26RestoreCheckStatus(String label, String badgeClass, int severity) {
        this.label = label;
        this.badgeClass = badgeClass;
        this.severity = severity;
    }

    public String getLabel() { return label; }
    public String getBadgeClass() { return badgeClass; }
    public int getSeverity() { return severity; }
}
