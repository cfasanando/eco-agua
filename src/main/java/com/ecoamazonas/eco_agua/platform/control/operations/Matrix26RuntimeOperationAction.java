package com.ecoamazonas.eco_agua.platform.control.operations;

public enum Matrix26RuntimeOperationAction {
    START("Start"),
    STOP("Stop"),
    RESTART("Restart"),
    FORCE_STOP("Force stop"),
    ADOPT("Adopt process"),
    CLEAN_STALE_PID("Clean stale PID"),
    ROTATE_LOGS("Rotate logs");

    private final String label;

    Matrix26RuntimeOperationAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
