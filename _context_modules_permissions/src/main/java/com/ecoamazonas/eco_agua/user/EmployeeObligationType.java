package com.ecoamazonas.eco_agua.user;

public enum EmployeeObligationType {
    LOAN("Préstamo"),
    ADVANCE("Adelanto"),
    OTHER("Otro descuento");

    private final String label;

    EmployeeObligationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
