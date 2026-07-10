package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;

@Controller
@RequestMapping("/gasto-claro/alerts")
public class PersonalFinanceAlertController {

    private final PersonalFinanceAlertService alertService;

    public PersonalFinanceAlertController(PersonalFinanceAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public String alerts(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "category", required = false) PersonalFinanceAlertCategory category,
            @RequestParam(name = "scope", required = false) PersonalFinanceAlertScope scope,
            Model model
    ) {
        YearMonth current = YearMonth.now();
        int selectedYear = year == null ? current.getYear() : Math.max(2000, Math.min(2100, year));
        int selectedMonth = month == null ? current.getMonthValue() : Math.max(1, Math.min(12, month));
        PersonalFinanceAlertCategory selectedCategory = category == null
                ? PersonalFinanceAlertCategory.ALL
                : category;
        PersonalFinanceAlertScope selectedScope = scope == null
                ? PersonalFinanceAlertScope.ALL
                : scope;
        PersonalFinanceAlertCenter center = alertService.center(
                YearMonth.of(selectedYear, selectedMonth),
                selectedCategory,
                selectedScope
        );

        model.addAttribute("activePage", "gasto_claro_alerts");
        model.addAttribute("center", center);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("selectedScope", selectedScope);
        model.addAttribute("categories", PersonalFinanceAlertCategory.values());
        model.addAttribute("scopes", PersonalFinanceAlertScope.values());
        return "personal_finance/alerts";
    }
}
