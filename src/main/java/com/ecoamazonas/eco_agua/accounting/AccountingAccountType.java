package com.ecoamazonas.eco_agua.accounting;

public enum AccountingAccountType {
    ASSET("Activo"),
    LIABILITY("Pasivo"),
    EQUITY("Patrimonio"),
    INCOME("Ingreso"),
    EXPENSE("Gasto / costo");

    private final String label;

    AccountingAccountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
