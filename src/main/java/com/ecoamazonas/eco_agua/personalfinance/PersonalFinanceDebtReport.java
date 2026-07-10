package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public record PersonalFinanceDebtReport(
        LocalDateTime generatedAt,
        LocalDate cutoffDate,
        YearMonth startMonth,
        int months,
        PersonalFinanceDebtReportVersion version,
        String ownerLabel,
        String title,
        String selectionSummary,
        boolean debtContentIncluded,
        boolean livingCostIncluded,
        boolean incomeCapacityIncluded,
        List<PersonalFinanceDebtReportDebt> debts,
        List<PersonalFinanceDebtReportMonth> monthlyProjection,
        List<PersonalFinanceDebtReportCategoryTotal> categoryTotals,
        List<PersonalFinanceDebtReportDebt> cancellationCandidates,
        List<PersonalFinanceDebtReportLivingCostItem> livingCosts,
        List<PersonalFinanceDebtReportIncomeItem> incomeSources,
        BigDecimal knownCapitalPen,
        BigDecimal knownCapitalUsd,
        BigDecimal firstMonthDebtPayments,
        BigDecimal firstMonthIncome,
        BigDecimal firstMonthBasicExpenses,
        BigDecimal firstMonthProjectedBalance,
        BigDecimal futureScheduledPaymentsPen,
        BigDecimal futureScheduledInterestPen,
        BigDecimal projectedLivingCostPeriodPen,
        BigDecimal projectedIncomePeriodPen,
        long activeDebtCount,
        long unknownBalanceCount,
        long highInterestDebtCount,
        long delinquentDebtCount,
        long settlementOpportunityCount
) {
    public boolean shared() {
        return version == PersonalFinanceDebtReportVersion.SHARED;
    }

    public BigDecimal firstMonthDeficitAmount() {
        if (firstMonthProjectedBalance == null || firstMonthProjectedBalance.signum() >= 0) {
            return BigDecimal.ZERO;
        }
        return firstMonthProjectedBalance.abs();
    }

    public boolean hasSelectedContent() {
        return debtContentIncluded || livingCostIncluded || incomeCapacityIncluded;
    }
}
