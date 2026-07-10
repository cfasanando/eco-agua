package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceObligationSourceType {
    FIXED_EXPENSE("Gasto fijo"),
    DEBT("Deuda"),
    MANUAL("Manual"),
    DEBT_SCHEDULE("Cronograma de deuda"),
    PRIVATE_LENDER_INTEREST("Cuota de prestamista"),
    AUTO_DEDUCTION("Descuento automático"),
    DEBT_VOLUNTARY_PAYMENT("Abono voluntario a deuda"),
    STUDY_CYCLE("Estudio por ciclo"),
    LIFE_COST("Costo de vida");

    private final String label;

    PersonalFinanceObligationSourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
