package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceDebtStatus {
    ACTIVE("Activa"),
    OVERDUE("Atrasada"),
    STOPPED_PAYMENT("Dejé de pagar"),
    COLLECTION("En cobranza"),
    PENDING_NEGOTIATION("Pendiente de negociación"),
    NEGOTIATION("En negociación"),
    REPROGRAMMED("Reprogramada"),
    PAID("Pagada"),
    SUSPENDED("Suspendida"),
    CANCELLED("Cancelada");

    private final String label;

    PersonalFinanceDebtStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
