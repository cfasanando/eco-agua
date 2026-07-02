package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceObligationGroup {
    BASIC_LIVING("Costo de vida básico"),
    DEBT_PAYMENT("Deudas x pagar"),
    STUDY("Estudios"),
    OTHER("Otros compromisos");

    private final String label;

    PersonalFinanceObligationGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
