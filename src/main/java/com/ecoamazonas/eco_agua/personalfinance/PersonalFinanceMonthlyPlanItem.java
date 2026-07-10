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
        boolean generated,
        Long debtId,
        String debtName,
        PersonalFinanceDebtClassification debtClassification,
        BigDecimal debtOutstandingBalance,
        boolean debtBalanceKnown,
        boolean bankBalanceReference,
        boolean settlementOpportunity
) {
    public boolean isPaid() {
        return status == PersonalFinanceObligationStatus.PAID || safe(pendingAmount).compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean hasLinkedDebt() {
        return debtId != null;
    }

    public BigDecimal settlementGap() {
        if (!debtBalanceKnown) {
            return BigDecimal.ZERO;
        }
        BigDecimal gap = safe(debtOutstandingBalance).subtract(safe(amountDue));
        return gap.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : gap;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
