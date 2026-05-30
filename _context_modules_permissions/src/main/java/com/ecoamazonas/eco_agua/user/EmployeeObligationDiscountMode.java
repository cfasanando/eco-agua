package com.ecoamazonas.eco_agua.user;

public enum EmployeeObligationDiscountMode {
    MANUAL("Manual"),
    FIXED_PER_PAYMENT("Monto fijo por pago"),
    PERCENTAGE("Porcentaje del pago");

    private final String label;

    EmployeeObligationDiscountMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
