package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceDebtReportVersion {
    PRIVATE("Privado completo"),
    SHARED("Compartible");

    private final String label;

    PersonalFinanceDebtReportVersion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
