package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinanceDelinquentDebtItem(
        Long id,
        String name,
        String creditorName,
        PersonalFinanceCurrency currency,
        BigDecimal currentBalance,
        BigDecimal previousMonthlyPayment,
        PersonalFinanceDebtStatus status,
        PersonalFinancePriority priority,
        PersonalFinanceCollectionStatus collectionStatus,
        PersonalFinanceNegotiationStatus negotiationStatus,
        LocalDate lastPaymentDate,
        LocalDate delinquencyStartDate,
        long overdueDays,
        LocalDate nextReviewDate,
        String contactName,
        String notes
) {
    public boolean reviewDue() {
        return nextReviewDate != null && !nextReviewDate.isAfter(LocalDate.now());
    }
}
