package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinanceAlertItem(
        String key,
        PersonalFinanceAlertCategory category,
        PersonalFinanceAlertSeverity severity,
        LocalDate date,
        String title,
        String detail,
        BigDecimal amount,
        PersonalFinanceCurrency currency,
        String statusLabel,
        String actionUrl,
        String actionLabel,
        boolean overdue,
        boolean today,
        boolean partial,
        long daysFromToday
) {
    public String currencySymbol() {
        if (amount == null || currency == null) {
            return "";
        }
        return currency == PersonalFinanceCurrency.PEN ? "S/" : "US$";
    }

    public String cardClass() {
        return "alert-card-" + severity.getBootstrapClass();
    }
}
