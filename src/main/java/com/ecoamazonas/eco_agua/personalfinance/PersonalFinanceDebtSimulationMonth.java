package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.YearMonth;

public record PersonalFinanceDebtSimulationMonth(
        int monthNumber,
        YearMonth month,
        BigDecimal openingBalance,
        BigDecimal interestAdded,
        BigDecimal scheduledPaid,
        BigDecimal extraPaid,
        BigDecimal unusedBudget,
        BigDecimal endingBalance,
        int activeDebtCount,
        int paidOffCount,
        BigDecimal monthlyPressureFreed
) {
    public boolean milestone() {
        return monthNumber <= 12 || monthNumber % 12 == 0 || paidOffCount > 0 || activeDebtCount == 0;
    }
}
