package com.ecoamazonas.eco_agua.accounting;

public enum AccountingPeriodCloseStatus {
    OPEN("Abierto"),
    CLOSED("Cerrado");

    private final String label;

    AccountingPeriodCloseStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
