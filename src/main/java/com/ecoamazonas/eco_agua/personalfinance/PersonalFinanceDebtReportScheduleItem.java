package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinanceDebtReportScheduleItem(
        Long debtId,
        String debtName,
        String classificationLabel,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal insuranceAmount,
        BigDecimal feeAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        PersonalFinanceCurrency currency,
        String statusLabel,
        boolean projected,
        String notes
) {
    public BigDecimal otherAmount() {
        BigDecimal insurance = insuranceAmount == null ? BigDecimal.ZERO : insuranceAmount;
        BigDecimal fees = feeAmount == null ? BigDecimal.ZERO : feeAmount;
        return insurance.add(fees);
    }
}
