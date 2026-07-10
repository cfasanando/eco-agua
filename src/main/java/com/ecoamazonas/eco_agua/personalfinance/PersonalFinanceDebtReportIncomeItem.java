package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinanceDebtReportIncomeItem(
        String name,
        String typeLabel,
        BigDecimal amount,
        BigDecimal firstMonthAmount,
        BigDecimal projectedPeriodAmount,
        PersonalFinanceCurrency currency,
        String frequencyLabel,
        Integer expectedDay,
        LocalDate startDate,
        LocalDate endDate
) {
}
