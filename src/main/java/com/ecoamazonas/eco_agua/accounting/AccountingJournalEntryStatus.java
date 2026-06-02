package com.ecoamazonas.eco_agua.accounting;

public enum AccountingJournalEntryStatus {
    DRAFT("Borrador"),
    POSTED("Registrado"),
    CANCELLED("Anulado");

    private final String label;

    AccountingJournalEntryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
