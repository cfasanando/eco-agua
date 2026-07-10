package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.YearMonth;

public record PersonalFinanceMonthlyLiveSummary(
        YearMonth month,
        PersonalFinanceCurrency currency,
        BigDecimal expectedIncome,
        BigDecimal receivedIncome,
        BigDecimal paidTotal,
        BigDecimal pendingTotal,
        BigDecimal realBalance,
        BigDecimal projectedBalance,
        long totalPayments,
        long paidPayments,
        long pendingPayments,
        long totalIncomes,
        long receivedIncomes
) {
    public int completionPercentage() {
        if (totalPayments <= 0) {
            return 100;
        }
        return (int) Math.min(100, Math.round((paidPayments * 100.0d) / totalPayments));
    }
}
