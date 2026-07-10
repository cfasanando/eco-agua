package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;

public record PersonalFinanceDebtSimulationOptions(
        YearMonth startMonth,
        PersonalFinanceCurrency currency,
        PersonalFinanceDebtStrategy strategy,
        BigDecimal lumpSum,
        BigDecimal monthlyExtra,
        int horizonMonths,
        List<Long> targetDebtIds
) {
    public PersonalFinanceDebtSimulationOptions {
        startMonth = startMonth == null ? YearMonth.now() : startMonth;
        currency = currency == null ? PersonalFinanceCurrency.PEN : currency;
        strategy = strategy == null ? PersonalFinanceDebtStrategy.HIGHEST_INTEREST : strategy;
        lumpSum = money(lumpSum);
        monthlyExtra = money(monthlyExtra);
        horizonMonths = Math.max(1, Math.min(horizonMonths <= 0 ? 120 : horizonMonths, 360));
        targetDebtIds = targetDebtIds == null
                ? List.of()
                : List.copyOf(new LinkedHashSet<>(targetDebtIds.stream().filter(java.util.Objects::nonNull).toList()));
    }

    public boolean customSelection() {
        return strategy == PersonalFinanceDebtStrategy.CUSTOM_SELECTION;
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
