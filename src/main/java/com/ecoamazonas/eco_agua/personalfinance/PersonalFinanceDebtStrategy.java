package com.ecoamazonas.eco_agua.personalfinance;

public enum PersonalFinanceDebtStrategy {
    HIGHEST_INTEREST(
            "Mayor interés primero",
            "Dirige el dinero extra a la deuda con la tasa mensual más alta para reducir el costo financiero."
    ),
    LOWEST_BALANCE(
            "Menor saldo primero",
            "Busca cerrar deudas pequeñas antes para reducir rápidamente la cantidad de compromisos abiertos."
    ),
    HIGHEST_MONTHLY_PAYMENT(
            "Mayor cuota primero",
            "Prioriza la deuda con la cuota mensual más alta para liberar capacidad de pago."
    ),
    FREE_CASH_FLOW(
            "Liberar flujo más rápido",
            "Prioriza la mejor relación entre saldo pendiente y cuota mensual para liberar flujo en menos tiempo."
    ),
    CUSTOM_SELECTION(
            "Selección manual",
            "Aplica el dinero extra únicamente a las deudas seleccionadas, en el orden en que aparecen."
    );

    private final String label;
    private final String description;

    PersonalFinanceDebtStrategy(String label, String description) {
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
