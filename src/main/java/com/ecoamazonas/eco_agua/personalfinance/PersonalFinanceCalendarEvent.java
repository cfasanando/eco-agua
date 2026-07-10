package com.ecoamazonas.eco_agua.personalfinance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalFinanceCalendarEvent(
        String key,
        PersonalFinanceAlertCategory category,
        LocalDate date,
        String title,
        String detail,
        BigDecimal amount,
        PersonalFinanceCurrency currency,
        String statusLabel,
        String actionUrl,
        String cssClass,
        String icon
) {
    public String currencySymbol() {
        if (amount == null || currency == null) {
            return "";
        }
        return currency == PersonalFinanceCurrency.PEN ? "S/" : "US$";
    }
}
