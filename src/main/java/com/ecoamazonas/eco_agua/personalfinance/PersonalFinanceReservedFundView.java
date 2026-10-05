package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinanceReservedFundView(
        Long id,
        String publicId,
        String title,
        PersonalFinanceReservedFundTargetType targetType,
        String targetLabel,
        BigDecimal amount,
        BigDecimal targetAmount,
        PersonalFinanceCurrency currency,
        LocalDate targetDate,
        PersonalFinanceReservedFundStatus status,
        PersonalFinanceAllocationStatus coverageStatus,
        String notes,
        Long obligationId,
        boolean applicableToPayment
) {
    public String currencySymbol() {
        return currency == PersonalFinanceCurrency.USD ? "US$" : "S/";
    }

    public int coveragePercentage() {
        if (targetAmount == null || targetAmount.signum() <= 0) {
            return amount != null && amount.signum() > 0 ? 100 : 0;
        }
        BigDecimal percentage = amount.multiply(new BigDecimal("100"))
                .divide(targetAmount, 0, java.math.RoundingMode.DOWN);
        return percentage.min(new BigDecimal("100")).max(BigDecimal.ZERO).intValue();
    }
}
