package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/gasto-claro/debt-simulator")
public class PersonalFinanceDebtSimulationController {

    private final PersonalFinanceDebtSimulationService simulationService;

    public PersonalFinanceDebtSimulationController(PersonalFinanceDebtSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping
    public String simulator(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "currency", required = false) PersonalFinanceCurrency currency,
            @RequestParam(name = "strategy", required = false) PersonalFinanceDebtStrategy strategy,
            @RequestParam(name = "lumpSum", required = false) BigDecimal lumpSum,
            @RequestParam(name = "monthlyExtra", required = false) BigDecimal monthlyExtra,
            @RequestParam(name = "horizonMonths", required = false) Integer horizonMonths,
            @RequestParam(name = "targetDebtIds", required = false) List<Long> targetDebtIds,
            Model model
    ) {
        YearMonth current = YearMonth.now();
        int selectedYear = year == null ? current.getYear() : year;
        int selectedMonth = month == null ? current.getMonthValue() : Math.max(1, Math.min(month, 12));
        YearMonth startMonth = YearMonth.of(selectedYear, selectedMonth);
        PersonalFinanceDebtSimulationOptions options = new PersonalFinanceDebtSimulationOptions(
                startMonth,
                currency,
                strategy,
                lumpSum,
                monthlyExtra,
                horizonMonths == null ? 120 : horizonMonths,
                targetDebtIds
        );
        PersonalFinanceDebtSimulation simulation = simulationService.simulate(options);

        model.addAttribute("activePage", "gasto_claro_debt_simulator");
        model.addAttribute("selectedYear", startMonth.getYear());
        model.addAttribute("selectedMonth", startMonth.getMonthValue());
        model.addAttribute("selectedCurrency", options.currency());
        model.addAttribute("selectedStrategy", options.strategy());
        model.addAttribute("selectedTargetDebtIds", options.targetDebtIds());
        model.addAttribute("lumpSum", options.lumpSum());
        model.addAttribute("monthlyExtra", options.monthlyExtra());
        model.addAttribute("horizonMonths", options.horizonMonths());
        model.addAttribute("currencySymbol", options.currency() == PersonalFinanceCurrency.PEN ? "S/" : "US$");
        model.addAttribute("currencies", PersonalFinanceCurrency.values());
        model.addAttribute("strategies", PersonalFinanceDebtStrategy.values());
        model.addAttribute("simulation", simulation);
        return "personal_finance/debt_simulator";
    }
}
