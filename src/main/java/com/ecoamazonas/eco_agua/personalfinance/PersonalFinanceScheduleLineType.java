package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceScheduleLineType {
    INSTALLMENT("Cuota"),
    INTEREST("Interés"),
    LENDER_INSTALLMENT("Cuota prestamista"),
    PRINCIPAL("Capital"),
    ONE_TIME("Pago único"),
    AUTO_DEDUCTION("Descuento automático"),
    NEGOTIATION("Negociación"),
    TRACKING("Seguimiento");

    private final String label;

    PersonalFinanceScheduleLineType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
