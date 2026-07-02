package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceDebtStatus {
    ACTIVE("Activa"),
    PAID("Pagada"),
    SUSPENDED("Suspendida"),
    CANCELLED("Cancelada");

    private final String label;

    PersonalFinanceDebtStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
