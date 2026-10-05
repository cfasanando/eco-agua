package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceReservedFundTargetType {
    OBLIGATION("Compromiso mensual"),
    DEBT("Deuda"),
    NEGOTIATION("Negociación"),
    FREE("Reserva libre");

    private final String label;

    PersonalFinanceReservedFundTargetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
