package com.ecoamazonas.eco_agua.category;

public enum CostBehavior {
    FIXED_STRUCTURAL("Fijo estructural"),
    VARIABLE_DIRECT("Variable directo"),
    VARIABLE_OPERATIONAL("Variable operativo"),
    NON_OPERATING("No operativo");

    private final String label;

    CostBehavior(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
