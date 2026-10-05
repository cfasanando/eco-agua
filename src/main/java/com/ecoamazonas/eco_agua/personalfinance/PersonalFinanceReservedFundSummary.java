package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.YearMonth;

public record PersonalFinanceReservedFundSummary(
        YearMonth month,
        PersonalFinanceCurrency currency,
        BigDecimal receivedIncome,
        BigDecimal paidTotal,
        BigDecimal availableCash,
        BigDecimal reservedTotal,
        BigDecimal freeCash,
        BigDecimal overReserved,
        long activeFunds,
        long coveredFunds,
        long partialFunds,
        long uncoveredUpcomingObligations
) {
    public String currencySymbol() {
        return currency == PersonalFinanceCurrency.USD ? "US$" : "S/";
    }
}
