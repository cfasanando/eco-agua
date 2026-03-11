package com.ecoamazonas.eco_agua.category;

public enum PersonnelMode {
    NONE("Sin regla de personal"),
    FIXED("Personal fijo"),
    PERCENT_OF_SALES("Porcentaje de ventas"),
    MIXED("Mixto");

    private final String label;

    PersonnelMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPersonnelCategory() {
        return this != NONE;
    }

    public boolean isVariableRule() {
        return this == PERCENT_OF_SALES || this == MIXED;
    }
}
