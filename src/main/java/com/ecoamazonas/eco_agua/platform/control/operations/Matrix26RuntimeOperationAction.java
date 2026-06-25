package com.ecoamazonas.eco_agua.platform.control.operations;

public enum Matrix26RuntimeOperationAction {
    START("Start"),
    STOP("Stop"),
    RESTART("Restart");

    private final String label;

    Matrix26RuntimeOperationAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
