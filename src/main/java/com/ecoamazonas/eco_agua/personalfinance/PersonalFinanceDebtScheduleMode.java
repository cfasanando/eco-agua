package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceDebtScheduleMode {
    SIMPLE_MONTHLY("Pago mensual simple"),
    BANK_SCHEDULE("Cronograma bancario"),
    PRIVATE_LENDER_INTEREST("Prestamista con interés mensual"),
    ONE_TIME("Pago único"),
    AUTO_DEDUCTION("Descuento automático"),
    TRACKING_ONLY("Solo seguimiento / mora");

    private final String label;

    PersonalFinanceDebtScheduleMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
