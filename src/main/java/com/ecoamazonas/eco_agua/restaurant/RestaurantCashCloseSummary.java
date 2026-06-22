package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantCashCloseSummary(
        RestaurantCashSessionRow session,
        BigDecimal cashSales,
        BigDecimal manualIncome,
        BigDecimal manualExpense,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal difference
) {
    public BigDecimal safeCashSales() { return safe(cashSales); }
    public BigDecimal safeManualIncome() { return safe(manualIncome); }
    public BigDecimal safeManualExpense() { return safe(manualExpense); }
    public BigDecimal safeExpectedCash() { return safe(expectedCash); }
    public BigDecimal safeCountedCash() { return safe(countedCash); }
    public BigDecimal safeDifference() { return safe(difference); }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
