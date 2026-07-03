package com.ecoamazonas.eco_agua.personalfinance;

public record PersonalFinanceMonthGenerationResult(
        int incomeEventsCreated,
        int fixedExpenseObligationsCreated,
        int debtScheduleLinesCreated,
        int debtScheduleObligationsCreated,
        int simpleDebtObligationsCreated
) {
    public int totalCreated() {
        return incomeEventsCreated
                + fixedExpenseObligationsCreated
                + debtScheduleLinesCreated
                + debtScheduleObligationsCreated
                + simpleDebtObligationsCreated;
    }

    public String summary() {
        return "Ingresos: " + incomeEventsCreated
                + ", costo de vida: " + fixedExpenseObligationsCreated
                + ", cronogramas: " + debtScheduleLinesCreated
                + ", deudas desde cronograma: " + debtScheduleObligationsCreated
                + ", deudas simples: " + simpleDebtObligationsCreated
                + ".";
    }
}
