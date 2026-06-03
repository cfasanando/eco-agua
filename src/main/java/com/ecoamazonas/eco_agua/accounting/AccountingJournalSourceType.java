package com.ecoamazonas.eco_agua.accounting;

public enum AccountingJournalSourceType {
    MANUAL("Manual"),
    SALE("Venta"),
    PURCHASE("Compra"),
    INCOME("Ingreso"),
    PAYMENT("Pago / cobro"),
    ADJUSTMENT("Ajuste");

    private final String label;

    AccountingJournalSourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
