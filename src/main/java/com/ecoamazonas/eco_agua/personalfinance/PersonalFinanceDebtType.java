package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceDebtType {
    CREDIT_CARD("Tarjeta de crédito"),
    BANK_LOAN("Préstamo bancario"),
    THIRD_PARTY_LOAN("Préstamo a terceros"),
    STORE_CREDIT("Crédito en tienda"),
    RECURRING_COMMITMENT("Compromiso mensual"),
    OTHER("Otra deuda");

    private final String label;

    PersonalFinanceDebtType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
