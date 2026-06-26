package com.ecoamazonas.eco_agua.platform.control.lifecycle;

public enum Matrix26LifecycleStatus {
    REQUESTED("Requested", "text-bg-secondary"),
    VALIDATING("Validating", "text-bg-info"),
    SUSPENDING("Suspending", "text-bg-warning"),
    SUSPENDED("Suspended", "text-bg-dark"),
    REACTIVATING("Reactivating", "text-bg-info"),
    ACTIVE("Active", "text-bg-success"),
    SUSPENSION_FAILED("Suspension failed", "text-bg-danger"),
    REACTIVATION_FAILED("Reactivation failed", "text-bg-danger"),
    CANCELLED("Cancelled", "text-bg-secondary");

    private final String label;
    private final String badgeClass;

    Matrix26LifecycleStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public boolean isFinalState() {
        return this == SUSPENDED || this == ACTIVE || this == SUSPENSION_FAILED
                || this == REACTIVATION_FAILED || this == CANCELLED;
    }
}
