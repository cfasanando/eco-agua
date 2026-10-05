package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceReservedFundMovementType {
    CREATED("Reserva creada"),
    INCREASED("Importe incrementado"),
    REDUCED("Importe reducido"),
    TRANSFER_OUT("Transferencia enviada"),
    TRANSFER_IN("Transferencia recibida"),
    APPLIED("Aplicada a un pago"),
    RESTORED("Restaurada por reversión"),
    RELEASED("Reserva liberada");

    private final String label;

    PersonalFinanceReservedFundMovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
