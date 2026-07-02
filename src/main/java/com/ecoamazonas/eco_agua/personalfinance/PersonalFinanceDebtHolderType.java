package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceDebtHolderType {
    OWN_NAME("A mi nombre"),
    THIRD_PARTY_NAME("A nombre de tercero"),
    SHARED("Compartida"),
    UNKNOWN("No definido");

    private final String label;

    PersonalFinanceDebtHolderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
