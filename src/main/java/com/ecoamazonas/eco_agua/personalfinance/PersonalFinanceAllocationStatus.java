package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceAllocationStatus {
    COVERED("Cubierto", "success"),
    PARTIAL("Cobertura parcial", "warning"),
    UNFUNDED("Sin cobertura", "danger");

    private final String label;
    private final String bootstrapClass;

    PersonalFinanceAllocationStatus(String label, String bootstrapClass) {
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
