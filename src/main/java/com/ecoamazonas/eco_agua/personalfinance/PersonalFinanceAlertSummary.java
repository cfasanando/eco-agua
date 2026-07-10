package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;

public record PersonalFinanceAlertSummary(
        long overduePayments,
        long dueToday,
        long upcomingSevenDays,
        long partialPayments,
        long pendingIncomes,
        long negotiationFollowUps,
        BigDecimal pendingPen,
        BigDecimal pendingUsd
) {
    public long urgentCount() {
        return overduePayments + dueToday + negotiationFollowUps;
    }
}
