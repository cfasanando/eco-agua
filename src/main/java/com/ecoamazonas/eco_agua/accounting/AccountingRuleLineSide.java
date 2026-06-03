package com.ecoamazonas.eco_agua.accounting;

public enum AccountingRuleLineSide {
    DEBIT("Debe"),
    CREDIT("Haber");

    private final String label;

    AccountingRuleLineSide(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
