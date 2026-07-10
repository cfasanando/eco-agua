package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinancePaymentOrigin {
    MANUAL("Registro manual"),
    QUICK_MONTHLY("Registro rápido del plan"),
    QUICK_SCHEDULE("Registro rápido del cronograma"),
    LEGACY_MIGRATION("Migrado desde estado anterior");

    private final String label;

    PersonalFinancePaymentOrigin(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
