package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinancePaymentContext(
        Long obligationId,
        String obligationTitle,
        PersonalFinanceCurrency currency,
        LocalDate dueDate,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        BigDecimal pendingAmount,
        Long debtId,
        String debtName,
        BigDecimal debtBalance,
        boolean debtBalanceKnown,
        Long scheduleLineId,
        Integer scheduleLineNumber,
        PersonalFinancePaymentForm form
) {
}
