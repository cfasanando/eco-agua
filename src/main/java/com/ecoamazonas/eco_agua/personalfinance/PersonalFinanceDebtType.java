package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceDebtType {
    CREDIT_CARD("Tarjeta de crédito"),
    BANK_LOAN("Préstamo bancario propio"),
    BANK_THIRD_PARTY("Banco por tercero"),
    PRIVATE_LENDER("Prestamista"),
    THIRD_PARTY_LOAN("Deuda a tercero / familiar"),
    STORE_CREDIT("Crédito en tienda"),
    RECURRING_COMMITMENT("Compromiso mensual"),
    STUDY_CYCLE("Estudio por ciclo"),
    OTHER("Otra deuda");

    private final String label;

    PersonalFinanceDebtType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
