package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record PersonalFinanceDebtReportMonth(
        YearMonth month,
        BigDecimal expectedIncome,
        BigDecimal basicExpenses,
        BigDecimal debtPayments,
        BigDecimal projectedBalance,
        List<PersonalFinanceDebtReportScheduleItem> payments
) {
    public boolean deficit() {
        return projectedBalance != null && projectedBalance.signum() < 0;
    }
}
