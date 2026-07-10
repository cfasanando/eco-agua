package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinanceDebtNegotiationComparison(
        BigDecimal currentBalance,
        BigDecimal currentMonthlyPayment,
        BigDecimal currentMonthlyRate,
        Integer currentEstimatedMonths,
        LocalDate currentEstimatedEndDate,
        BigDecimal currentProjectedInterest,
        BigDecimal currentProjectedTotal,
        boolean currentPlanAmortizes,
        BigDecimal proposalTotal,
        BigDecimal proposalEstimatedInterest,
        BigDecimal differenceAgainstCurrentBalance,
        BigDecimal estimatedSavingsAgainstCurrentPlan,
        BigDecimal monthlyRelief,
        LocalDate proposalEndDate
) {
}
