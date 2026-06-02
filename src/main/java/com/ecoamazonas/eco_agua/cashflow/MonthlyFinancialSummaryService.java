package com.ecoamazonas.eco_agua.cashflow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
public class MonthlyFinancialSummaryService {

    private static final Locale SPANISH_LOCALE = Locale.forLanguageTag("es-PE");

    private final CashflowService cashflowService;

    public MonthlyFinancialSummaryService(CashflowService cashflowService) {
        this.cashflowService = cashflowService;
    }

    @Transactional(readOnly = true)
    public MonthlyFinancialSummary build(Integer year, Integer month) {
        YearMonth selectedMonth = normalizeMonth(year, month);
        LocalDate startDate = selectedMonth.atDay(1);
        LocalDate endDate = selectedMonth.atEndOfMonth();

        List<CashflowItem> dailyItems = cashflowService.buildCashflow(startDate, endDate);
        CashflowSummary cashflowSummary = cashflowService.buildSummary(dailyItems);

        YearMonth previous = selectedMonth.minusMonths(1);
        YearMonth next = selectedMonth.plusMonths(1);

        MonthlyFinancialSummary summary = new MonthlyFinancialSummary();
        summary.setYear(selectedMonth.getYear());
        summary.setMonth(selectedMonth.getMonthValue());
        summary.setPreviousYear(previous.getYear());
        summary.setPreviousMonth(previous.getMonthValue());
        summary.setNextYear(next.getYear());
        summary.setNextMonth(next.getMonthValue());
        summary.setMonthLabel(buildMonthLabel(selectedMonth));
        summary.setStartDate(startDate);
        summary.setEndDate(endDate);
        summary.setDailyItems(dailyItems);
        summary.setCashflowSummary(cashflowSummary);
        return summary;
    }

    private YearMonth normalizeMonth(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int normalizedYear = year != null ? year : today.getYear();
        int normalizedMonth = month != null ? month : today.getMonthValue();

        if (normalizedMonth < 1) {
            normalizedMonth = 1;
        }
        if (normalizedMonth > 12) {
            normalizedMonth = 12;
        }

        return YearMonth.of(normalizedYear, normalizedMonth);
    }

    private String buildMonthLabel(YearMonth yearMonth) {
        String monthName = yearMonth.getMonth().getDisplayName(TextStyle.FULL, SPANISH_LOCALE);
        if (monthName == null || monthName.isBlank()) {
            monthName = String.valueOf(yearMonth.getMonthValue());
        }
        String normalized = monthName.substring(0, 1).toUpperCase(SPANISH_LOCALE) + monthName.substring(1);
        return normalized + " " + yearMonth.getYear();
    }
}
