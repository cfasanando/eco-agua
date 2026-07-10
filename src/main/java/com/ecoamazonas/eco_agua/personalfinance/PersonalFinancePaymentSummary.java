package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;

public record PersonalFinancePaymentSummary(
        BigDecimal totalPaid,
        BigDecimal principalPaid,
        BigDecimal interestPaid,
        BigDecimal chargesPaid,
        BigDecimal reversedTotal,
        long activeCount,
        long reversedCount
) {
}
