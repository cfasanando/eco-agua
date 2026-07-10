package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceCashBasis {
    EXPECTED("Ingreso esperado"),
    RECEIVED("Ingreso recibido"),
    MANUAL("Dinero disponible manual");

    private final String label;

    PersonalFinanceCashBasis(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
