package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PersonalFinanceReservedFundMovementView(
        Long id,
        String fundTitle,
        PersonalFinanceReservedFundMovementType movementType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        PersonalFinanceCurrency currency,
        String notes,
        LocalDateTime createdAt
) {
    public String currencySymbol() {
        return currency == PersonalFinanceCurrency.USD ? "US$" : "S/";
    }
}
