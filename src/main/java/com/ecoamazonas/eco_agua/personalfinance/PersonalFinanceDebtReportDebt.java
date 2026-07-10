package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PersonalFinanceDebtReportDebt(
        Long id,
        String name,
        String creditorName,
        String holderLabel,
        String contactName,
        String typeLabel,
        String classificationLabel,
        String scheduleModeLabel,
        String statusLabel,
        String priorityLabel,
        String collectionStatusLabel,
        String negotiationStatusLabel,
        PersonalFinanceCurrency currency,
        BigDecimal originalAmount,
        BigDecimal outstandingBalance,
        BigDecimal monthlyPayment,
        BigDecimal monthlyInterestRate,
        LocalDate nextDueDate,
        LocalDate estimatedEndDate,
        boolean estimatedEndDateApproximate,
        int remainingInstallments,
        BigDecimal futurePrincipal,
        BigDecimal futureInterest,
        BigDecimal futurePayments,
        boolean balanceKnown,
        boolean bankBalanceReference,
        boolean highInterest,
        boolean delinquent,
        boolean settlementOpportunity,
        String notes,
        List<PersonalFinanceDebtReportScheduleItem> scheduleItems
) {
}
