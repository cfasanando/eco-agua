package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinancePaymentMethod {
    CASH("Efectivo"),
    BANK_TRANSFER("Transferencia bancaria"),
    YAPE("Yape"),
    PLIN("Plin"),
    CARD("Tarjeta"),
    DIRECT_DEBIT("Débito automático"),
    OTHER("Otro");

    private final String label;

    PersonalFinancePaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
