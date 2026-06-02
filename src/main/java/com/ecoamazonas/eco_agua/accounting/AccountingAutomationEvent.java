package com.ecoamazonas.eco_agua.accounting;

public enum AccountingAutomationEvent {
    SALE_PAID(
            "Venta pagada",
            "Cuando un pedido se confirma y se cobra al contado.",
            "Caja / Bancos",
            "Ventas"
    ),
    SALE_CREDIT(
            "Venta fiada",
            "Cuando un pedido queda pendiente de cobro.",
            "Clientes",
            "Ventas"
    ),
    CREDIT_COLLECTION(
            "Cobro de fiado",
            "Cuando se registra un abono o cancelación de una cuenta por cobrar.",
            "Caja / Bancos",
            "Clientes"
    ),
    EXPENSE_PAID(
            "Egreso pagado",
            "Cuando un gasto se registra y se paga al contado.",
            "Compra / Gasto",
            "Caja / Bancos"
    ),
    EXPENSE_CREDIT(
            "Egreso a crédito",
            "Cuando un gasto queda pendiente de pago.",
            "Compra / Gasto",
            "Proveedores"
    ),
    SUPPLIER_PAYMENT(
            "Pago a proveedor",
            "Cuando se paga total o parcialmente una cuenta por pagar.",
            "Proveedores",
            "Caja / Bancos"
    ),
    OTHER_INCOME(
            "Otro ingreso",
            "Cuando se registra un ingreso que no viene de pedidos.",
            "Caja / Bancos",
            "Otros ingresos"
    ),
    STOCK_PURCHASE(
            "Compra que aumenta stock",
            "Cuando una compra de mercadería incrementa inventario.",
            "Mercaderías / Compras",
            "Caja / Bancos / Proveedores"
    );

    private final String label;
    private final String description;
    private final String suggestedDebit;
    private final String suggestedCredit;

    AccountingAutomationEvent(String label, String description, String suggestedDebit, String suggestedCredit) {
        this.label = label;
        this.description = description;
        this.suggestedDebit = suggestedDebit;
        this.suggestedCredit = suggestedCredit;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getSuggestedDebit() {
        return suggestedDebit;
    }

    public String getSuggestedCredit() {
        return suggestedCredit;
    }
}
