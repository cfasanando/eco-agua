package com.ecoamazonas.eco_agua.accounting;

public enum AccountingRuleAmountBase {
    TOTAL("Total de la operación", "Usa el importe total del documento u operación."),
    NET_BASE("Base imponible / valor venta", "Usa el importe sin IGV cuando la operación tiene impuesto."),
    TAX_IGV("IGV", "Usa el importe del IGV de la operación."),
    PAID_AMOUNT("Importe pagado", "Usa el importe efectivamente cobrado o pagado."),
    PENDING_AMOUNT("Importe pendiente", "Usa el saldo pendiente por cobrar o pagar."),
    STOCK_VALUE("Valor de inventario", "Usa el valor de mercadería o inventario relacionado."),
    FIXED_AMOUNT("Importe fijo", "Usa un importe fijo definido en la línea de la plantilla.");

    private final String label;
    private final String description;

    AccountingRuleAmountBase(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
