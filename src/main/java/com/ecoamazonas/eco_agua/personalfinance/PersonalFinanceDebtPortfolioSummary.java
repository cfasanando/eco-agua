package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;

public record PersonalFinanceDebtPortfolioSummary(
        BigDecimal bankPenTotal,
        BigDecimal lenderPenTotal,
        BigDecimal directPenTotal,
        BigDecimal commitmentPenTotal,
        BigDecimal otherPenTotal,
        BigDecimal knownPenTotal,
        BigDecimal knownUsdTotal,
        long knownBalanceCount,
        long undefinedBalanceCount,
        long settlementOpportunityCount
) {
    public boolean hasUsdBalance() {
        return knownUsdTotal != null && knownUsdTotal.compareTo(BigDecimal.ZERO) > 0;
    }
}
