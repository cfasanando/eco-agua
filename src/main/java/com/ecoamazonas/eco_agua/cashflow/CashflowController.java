package com.ecoamazonas.eco_agua.cashflow;

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

    private final CashflowService cashflowService;

    private static final DateTimeFormatter LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CashflowController(CashflowService cashflowService) {
        this.cashflowService = cashflowService;
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
        System.out.println(">>> [CashflowController] Handling /cashflow");

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
            periodLabel = "del " + start.format(LABEL_FORMATTER)
                    + " al " + end.format(LABEL_FORMATTER);
        }

        List<CashflowItem> items;
        BigDecimal totalIncomes;
        BigDecimal totalExpenses;
        BigDecimal netResult;

        try {
            items = cashflowService.buildCashflow(start, end);
            totalIncomes = cashflowService.calculateTotalIncomes(items);
            totalExpenses = cashflowService.calculateTotalExpenses(items);
            netResult = cashflowService.calculateNetResult(items);

            model.addAttribute("errorMessage", null);
        } catch (Exception ex) {
            System.out.println("[CashflowController] Error building cashflow: " + ex.getMessage());
            ex.printStackTrace();

            items = List.of();
            totalIncomes = BigDecimal.ZERO;
            totalExpenses = BigDecimal.ZERO;
            netResult = BigDecimal.ZERO;
            model.addAttribute("errorMessage",
                    "Error al calcular el flujo de caja: " + ex.getMessage());
        }

        model.addAttribute("activePage", "cashflow");
        model.addAttribute("mode", normalizedMode);
        model.addAttribute("date", base);
        model.addAttribute("fromDate", start);
        model.addAttribute("toDate", end);
        model.addAttribute("periodLabel", periodLabel);

        model.addAttribute("cashflowItems", items);
        model.addAttribute("totalIncomes", totalIncomes);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("netResult", netResult);

        return "cashflow/cashflow";
    }
}
