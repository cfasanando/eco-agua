package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinanceMonthlyPlanItem(
        Long id,
        String title,
        String sourceLabel,
        PersonalFinanceObligationSourceType sourceType,
        PersonalFinanceObligationGroup group,
        PersonalFinanceCurrency currency,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        BigDecimal pendingAmount,
        LocalDate dueDate,
        PersonalFinanceObligationStatus status,
        PersonalFinancePriority priority,
        String notes,
        boolean generated
) {
    public boolean isPaid() {
        return status == PersonalFinanceObligationStatus.PAID || pendingAmount.compareTo(BigDecimal.ZERO) == 0;
    }
}
