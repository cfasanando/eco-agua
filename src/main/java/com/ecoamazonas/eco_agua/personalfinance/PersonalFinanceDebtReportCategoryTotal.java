package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;

public record PersonalFinanceDebtReportCategoryTotal(
        String label,
        BigDecimal penAmount,
        BigDecimal usdAmount,
        long debtCount
) {
}
