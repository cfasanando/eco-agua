package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceReservedFundStatus {
    ACTIVE("Activa", "primary"),
    APPLIED("Aplicada", "success"),
    RELEASED("Liberada", "secondary");

    private final String label;
    private final String bootstrapClass;

    PersonalFinanceReservedFundStatus(String label, String bootstrapClass) {
        this.label = label;
        this.bootstrapClass = bootstrapClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBootstrapClass() {
        return bootstrapClass;
    }
}
