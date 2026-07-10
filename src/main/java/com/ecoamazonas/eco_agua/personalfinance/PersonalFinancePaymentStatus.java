package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinancePaymentStatus {
    ACTIVE("Vigente"),
    REVERSED("Revertido");

    private final String label;

    PersonalFinancePaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
