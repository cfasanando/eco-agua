package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;

public record PersonalFinanceBankScheduleSummary(
        int totalLines,
        int paidLines,
        int pendingLines,
        int overdueLines,
        BigDecimal principalTotal,
        BigDecimal interestTotal,
        BigDecimal insuranceTotal,
        BigDecimal feeTotal,
        BigDecimal scheduledTotal,
        BigDecimal principalPaid,
        BigDecimal principalPending,
        BigDecimal futureTotal,
        BigDecimal futureInterest,
        PersonalFinanceDebtScheduleLine nextInstallment
) {
}
