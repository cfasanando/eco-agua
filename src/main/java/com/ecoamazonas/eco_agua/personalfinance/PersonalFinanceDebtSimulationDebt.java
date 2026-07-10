package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.YearMonth;

public record PersonalFinanceDebtSimulationDebt(
        int position,
        Long debtId,
        String name,
        String typeLabel,
        String classificationLabel,
        String statusLabel,
        BigDecimal initialBalance,
        BigDecimal monthlyRate,
        BigDecimal monthlyPayment,
        BigDecimal lumpSumApplied,
        BigDecimal projectedInterest,
        BigDecimal projectedPaid,
        BigDecimal endingBalance,
        YearMonth projectedPayoffMonth,
        BigDecimal monthlyPaymentFreed,
        boolean selectedTarget,
        boolean bankEstimate,
        boolean negativeAmortizationRisk,
        String reason
) {
    public boolean paidOff() {
        return endingBalance.compareTo(BigDecimal.ZERO) == 0;
    }
}
