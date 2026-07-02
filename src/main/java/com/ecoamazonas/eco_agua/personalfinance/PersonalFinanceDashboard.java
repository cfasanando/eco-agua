package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;

public record PersonalFinanceDashboard(
        BigDecimal expectedIncome,
        BigDecimal receivedIncome,
        BigDecimal fixedExpenseTotal,
        BigDecimal debtPressureTotal,
        BigDecimal projectedBalance,
        long activeDebts,
        long fixedExpenses,
        long incomeEvents,
        int year,
        int month
) {
}
