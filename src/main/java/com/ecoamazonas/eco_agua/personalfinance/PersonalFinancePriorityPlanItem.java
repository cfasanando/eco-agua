package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinancePriorityPlanItem(
        int position,
        Long obligationId,
        String title,
        String sourceLabel,
        PersonalFinanceObligationSourceType sourceType,
        PersonalFinanceObligationGroup group,
        PersonalFinanceCurrency currency,
        BigDecimal pendingAmount,
        LocalDate dueDate,
        PersonalFinanceObligationStatus obligationStatus,
        PersonalFinancePriority priority,
        BigDecimal recommendedAmount,
        BigDecimal unfundedAmount,
        BigDecimal cashAfter,
        PersonalFinanceAllocationStatus allocationStatus,
        String reason,
        boolean essential,
        boolean generated
) {
    public boolean isCovered() {
        return allocationStatus == PersonalFinanceAllocationStatus.COVERED;
    }
}
