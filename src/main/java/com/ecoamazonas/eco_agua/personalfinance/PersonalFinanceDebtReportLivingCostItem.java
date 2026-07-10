package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinanceDebtReportLivingCostItem(
        String name,
        String categoryLabel,
        BigDecimal amount,
        BigDecimal firstMonthAmount,
        BigDecimal projectedPeriodAmount,
        PersonalFinanceCurrency currency,
        String frequencyLabel,
        Integer dueDay,
        LocalDate startDate,
        LocalDate endDate,
        boolean mandatory
) {
}
