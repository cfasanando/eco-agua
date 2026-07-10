package com.ecoamazonas.eco_agua.personalfinance;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record PersonalFinanceAlertCenter(
        YearMonth selectedMonth,
        YearMonth previousMonth,
        YearMonth nextMonth,
        String monthLabel,
        LocalDate today,
        PersonalFinanceAlertSummary summary,
        List<PersonalFinanceAlertItem> alerts,
        List<PersonalFinanceCalendarDay> calendarDays,
        int totalCalendarEvents
) {
}
