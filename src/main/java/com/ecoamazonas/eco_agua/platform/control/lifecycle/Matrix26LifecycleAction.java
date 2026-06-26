package com.ecoamazonas.eco_agua.platform.control.lifecycle;

public enum Matrix26LifecycleAction {
    SUSPEND("Suspend"),
    REACTIVATE("Reactivate");

    private final String label;

    Matrix26LifecycleAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
