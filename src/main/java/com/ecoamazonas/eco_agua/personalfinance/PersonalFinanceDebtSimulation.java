package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public record PersonalFinanceDebtSimulation(
        PersonalFinanceDebtSimulationOptions options,
        LocalDateTime generatedAt,
        int debtCount,
        long excludedCurrencyCount,
        BigDecimal initialDebt,
        BigDecimal currentMonthlyPressure,
        BigDecimal lumpSumApplied,
        BigDecimal lumpSumUnused,
        BigDecimal totalMonthlyBudget,
        BigDecimal baselineEndingBalance,
        BigDecimal strategyEndingBalance,
        BigDecimal balanceAdvantage,
        BigDecimal baselineInterest,
        BigDecimal strategyInterest,
        BigDecimal estimatedInterestSaved,
        BigDecimal strategyTotalPaid,
        BigDecimal monthlyPressureFreed,
        YearMonth baselineDebtFreeMonth,
        YearMonth strategyDebtFreeMonth,
        int remainingDebtCount,
        int negativeAmortizationCount,
        List<PersonalFinanceDebtSimulationDebt> debts,
        List<PersonalFinanceDebtSimulationMonth> months,
        List<String> warnings
) {
    public boolean hasDebts() {
        return debtCount > 0;
    }

    public boolean debtFreeWithinHorizon() {
        return strategyDebtFreeMonth != null;
    }

    public boolean baselineDebtFreeWithinHorizon() {
        return baselineDebtFreeMonth != null;
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean hasCustomSelectionWarning() {
        return options.customSelection() && options.targetDebtIds().isEmpty();
    }
}
