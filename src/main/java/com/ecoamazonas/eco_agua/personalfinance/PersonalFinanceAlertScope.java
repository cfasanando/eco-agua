package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceAlertScope {
    ALL("Todas las alertas"),
    URGENT("Vencidas y de hoy"),
    NEXT_7_DAYS("Próximos 7 días"),
    NEXT_15_DAYS("Próximos 15 días"),
    PARTIAL("Pagos parciales");

    private final String label;

    PersonalFinanceAlertScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
