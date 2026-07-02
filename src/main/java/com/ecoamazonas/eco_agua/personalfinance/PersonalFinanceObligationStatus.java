package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceObligationStatus {
    PENDING("Pendiente"),
    PARTIAL("Parcial"),
    PAID("Pagado"),
    OVERDUE("Vencido"),
    CANCELLED("Cancelado");

    private final String label;

    PersonalFinanceObligationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
