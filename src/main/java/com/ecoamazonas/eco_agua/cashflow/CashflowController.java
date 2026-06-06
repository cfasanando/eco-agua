package com.ecoamazonas.eco_agua.cashflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class CashflowController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CashflowController.class);

    private final CashflowService cashflowService;
    private final MonthlyFinancialSummaryService monthlyFinancialSummaryService;

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CashflowController(
            CashflowService cashflowService,
            MonthlyFinancialSummaryService monthlyFinancialSummaryService
    ) {
        this.cashflowService = cashflowService;
        this.monthlyFinancialSummaryService = monthlyFinancialSummaryService;
    }

    @GetMapping("/cashflow")
    public String cashflow(
            @RequestParam(name = "mode", defaultValue = "BY_DAY") String mode,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        LocalDate base = (date != null ? date : today);

        LocalDate start;
        LocalDate end;
        String normalizedMode = (mode != null ? mode.toUpperCase() : "BY_DAY");

        switch (normalizedMode) {
            case "BY_PERIOD":
                start = (from != null ? from : base);
                end = (to != null ? to : base);
                break;
            case "BY_DAY":
            default:
                start = base;
                end = base;
                normalizedMode = "BY_DAY";
                break;
        }

        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        String periodLabel;
        if (start.equals(end)) {
            periodLabel = "del " + start.format(LABEL_FORMATTER);
        } else {
            periodLabel = "del " + start.format(LABEL_FORMATTER) + " al " + end.format(LABEL_FORMATTER);
        }

        List<CashflowItem> items;
        CashflowSummary summary;
        BigDecimal totalIncomes;
        BigDecimal totalExpenses;
        BigDecimal netResult;

        try {
            items = cashflowService.buildCashflow(start, end);
            summary = cashflowService.buildSummary(items);
            totalIncomes = summary.getCollectedIncome();
            totalExpenses = summary.getPaidExpenses();
            netResult = summary.getNetCashResult();
            model.addAttribute("errorMessage", null);
        } catch (Exception ex) {
            LOGGER.error("Cashflow calculation failed for period {} to {}", start, end, ex);

            items = List.of();
            summary = new CashflowSummary();
            totalIncomes = BigDecimal.ZERO;
            totalExpenses = BigDecimal.ZERO;
            netResult = BigDecimal.ZERO;

            model.addAttribute("errorMessage", "Error al calcular el flujo de caja: " + ex.getMessage());
        }

        model.addAttribute("activePage", "cashflow");
        model.addAttribute("mode", normalizedMode);
        model.addAttribute("date", base);
        model.addAttribute("fromDate", start);
        model.addAttribute("toDate", end);
        model.addAttribute("periodLabel", periodLabel);
        model.addAttribute("cashflowItems", items);
        model.addAttribute("summary", summary);
        model.addAttribute("totalIncomes", totalIncomes);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("netResult", netResult);

        return "cashflow/cashflow";
    }

    @GetMapping("/cashflow/monthly-summary")
    public String monthlyFinancialSummary(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            Model model
    ) {
        MonthlyFinancialSummary monthlySummary;

        try {
            monthlySummary = monthlyFinancialSummaryService.build(year, month);
            model.addAttribute("errorMessage", null);
        } catch (Exception ex) {
            LOGGER.error("Monthly financial summary calculation failed for year {} and month {}", year, month, ex);
            monthlySummary = monthlyFinancialSummaryService.build(LocalDate.now().getYear(), LocalDate.now().getMonthValue());
            model.addAttribute("errorMessage", "Error al calcular el resumen financiero mensual: " + ex.getMessage());
        }

        model.addAttribute("activePage", "cashflow_monthly_summary");
        model.addAttribute("monthlySummary", monthlySummary);
        model.addAttribute("selectedYear", monthlySummary.getYear());
        model.addAttribute("selectedMonth", monthlySummary.getMonth());

        return "cashflow/monthly_summary";
    }

    @GetMapping("/cashflow/day-detail")
    public String cashflowDayDetail(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        CashflowDayDetail detail = cashflowService.getDayDetail(date);
        model.addAttribute("dayDetail", detail);

        return "cashflow/day_detail_modal :: dayDetailModalContent";
    }
}