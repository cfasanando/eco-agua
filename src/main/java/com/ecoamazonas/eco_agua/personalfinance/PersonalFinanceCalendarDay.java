package com.ecoamazonas.eco_agua.personalfinance;

import java.time.LocalDate;
import java.util.List;

public record PersonalFinanceCalendarDay(
        LocalDate date,
        boolean inSelectedMonth,
        boolean today,
        List<PersonalFinanceCalendarEvent> events
) {
    public int hiddenEventCount() {
        return Math.max(events.size() - 3, 0);
    }
}
