package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record PersonalFinancePriorityPlan(
        YearMonth month,
        PersonalFinanceCurrency currency,
        PersonalFinanceCashBasis cashBasis,
        BigDecimal manualCash,
        BigDecimal expectedIncome,
        BigDecimal receivedIncome,
        BigDecimal alreadyPaid,
        BigDecimal cashBasisAmount,
        BigDecimal availableCash,
        BigDecimal essentialPending,
        BigDecimal debtPending,
        BigDecimal otherPending,
        BigDecimal totalPending,
        BigDecimal allocatedTotal,
        BigDecimal unfundedTotal,
        BigDecimal unfundedDebtTotal,
        BigDecimal remainingCash,
        BigDecimal essentialGap,
        BigDecimal debtPressurePercentage,
        String largestDebtTitle,
        BigDecimal largestDebtAmount,
        BigDecimal largestDebtSharePercentage,
        PersonalFinanceHealthLevel healthLevel,
        String headline,
        String recommendation,
        List<PersonalFinancePriorityPlanItem> items,
        long coveredCount,
        long partialCount,
        long unfundedCount,
        long excludedIncomeCount,
        long excludedObligationCount
) {
    public boolean hasUnfundedAmount() {
        return unfundedTotal.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasExcludedCurrencies() {
        return excludedIncomeCount > 0 || excludedObligationCount > 0;
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }
}
