package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record PersonalFinanceMonthlyPlan(
        YearMonth month,
        List<PersonalFinanceIncomeEvent> incomes,
        List<PersonalFinanceMonthlyPlanItem> basicLivingItems,
        List<PersonalFinanceMonthlyPlanItem> debtItems,
        List<PersonalFinanceMonthlyPlanItem> otherItems,
        List<PersonalFinanceDelinquentDebtItem> delinquentDebts,
        BigDecimal expectedIncome,
        BigDecimal receivedIncome,
        BigDecimal basicLivingTotal,
        BigDecimal debtTotal,
        BigDecimal otherTotal,
        BigDecimal obligationsTotal,
        BigDecimal paidTotal,
        BigDecimal pendingTotal,
        BigDecimal projectedBalance,
        BigDecimal cashAfterBasicLiving,
        BigDecimal cashAfterAllObligations,
        BigDecimal delinquentDebtPenTotal,
        BigDecimal delinquentDebtUsdTotal,
        PersonalFinanceDebtPortfolioSummary debtPortfolio,
        long delinquentDebtCount,
        long overdueCount,
        long highInterestDebtCount,
        long reviewDueCount
) {
    public boolean hasDeficit() {
        return projectedBalance.compareTo(BigDecimal.ZERO) < 0;
    }
}
