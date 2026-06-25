package com.ecoamazonas.eco_agua.platform.control.operations;

public enum Matrix26ManagedRuntimeState {
    STOPPED("Stopped", "text-bg-secondary"),
    STARTING("Starting", "text-bg-info"),
    ONLINE("Online", "text-bg-success"),
    STOPPING("Stopping", "text-bg-warning"),
    RESTARTING("Restarting", "text-bg-info"),
    DEGRADED("Degraded", "text-bg-warning"),
    FAILED("Failed", "text-bg-danger"),
    UNKNOWN("Unknown", "text-bg-dark");

    private final String label;
    private final String badgeClass;

    Matrix26ManagedRuntimeState(String label, String badgeClass) {
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
