package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceIncomeStatus {
    PLANNED("Planeado"),
    RECEIVED("Recibido"),
    MISSED("No recibido"),
    CANCELLED("Cancelado");

    private final String label;

    PersonalFinanceIncomeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
