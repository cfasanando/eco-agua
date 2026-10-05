package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.YearMonth;

@Controller
@RequestMapping("/gasto-claro/reserved-funds")
public class PersonalFinanceReservedFundController {

    private final PersonalFinanceReservedFundService reservedFundService;

    public PersonalFinanceReservedFundController(PersonalFinanceReservedFundService reservedFundService) {
        this.reservedFundService = reservedFundService;
    }

    @GetMapping
    public String index(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "currency", defaultValue = "PEN") PersonalFinanceCurrency currency,
            Model model
    ) {
        YearMonth selectedMonth = selectedMonth(year, month);
        PersonalFinanceReservedFundDashboard dashboard = reservedFundService.dashboard(selectedMonth, currency);
        model.addAttribute("activePage", "gasto_claro_reserved_funds");
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("reservedFundForm", dashboard.form());
        model.addAttribute("selectedYear", selectedMonth.getYear());
        model.addAttribute("selectedMonth", selectedMonth.getMonthValue());
        model.addAttribute("selectedCurrency", currency);
        model.addAttribute("currencies", PersonalFinanceCurrency.values());
        model.addAttribute("targetTypes", PersonalFinanceReservedFundTargetType.values());
        return "personal_finance/reserved_funds";
    }

    @PostMapping
    public String create(
            @ModelAttribute PersonalFinanceReservedFundForm form,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "viewCurrency", defaultValue = "PEN") PersonalFinanceCurrency viewCurrency,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reservedFundService.create(form);
            success(redirectAttributes, "Reserva creada correctamente.");
        } catch (IllegalArgumentException exception) {
            error(redirectAttributes, exception.getMessage());
        }
        return redirect(year, month, viewCurrency);
    }

    @PostMapping("/{id}/adjust")
    public String adjust(
            @PathVariable Long id,
            @RequestParam BigDecimal adjustment,
            @RequestParam(name = "notes", required = false) String notes,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "currency", defaultValue = "PEN") PersonalFinanceCurrency currency,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reservedFundService.adjust(id, adjustment, notes);
            success(redirectAttributes, "La reserva fue actualizada.");
        } catch (IllegalArgumentException exception) {
            error(redirectAttributes, exception.getMessage());
        }
        return redirect(year, month, currency);
    }

    @PostMapping("/{id}/release")
    public String release(
            @PathVariable Long id,
            @RequestParam(name = "notes", required = false) String notes,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "currency", defaultValue = "PEN") PersonalFinanceCurrency currency,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reservedFundService.release(id, notes);
            success(redirectAttributes, "La reserva fue liberada y vuelve a considerarse dinero libre.");
        } catch (IllegalArgumentException exception) {
            error(redirectAttributes, exception.getMessage());
        }
        return redirect(year, month, currency);
    }

    @PostMapping("/transfer")
    public String transfer(
            @RequestParam Long fromId,
            @RequestParam Long toId,
            @RequestParam BigDecimal amount,
            @RequestParam(name = "notes", required = false) String notes,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "currency", defaultValue = "PEN") PersonalFinanceCurrency currency,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reservedFundService.transfer(fromId, toId, amount, notes);
            success(redirectAttributes, "Transferencia entre reservas registrada.");
        } catch (IllegalArgumentException exception) {
            error(redirectAttributes, exception.getMessage());
        }
        return redirect(year, month, currency);
    }

    private YearMonth selectedMonth(Integer year, Integer month) {
        YearMonth current = YearMonth.now();
        return YearMonth.of(
                year == null ? current.getYear() : year,
                month == null ? current.getMonthValue() : Math.max(1, Math.min(12, month))
        );
    }

    private String redirect(Integer year, Integer month, PersonalFinanceCurrency currency) {
        YearMonth selected = selectedMonth(year, month);
        return "redirect:/gasto-claro/reserved-funds?year=" + selected.getYear()
                + "&month=" + selected.getMonthValue()
                + "&currency=" + (currency == null ? PersonalFinanceCurrency.PEN : currency);
    }

    private void success(RedirectAttributes attributes, String message) {
        attributes.addFlashAttribute("message", message);
        attributes.addFlashAttribute("messageType", "success");
    }

    private void error(RedirectAttributes attributes, String message) {
        attributes.addFlashAttribute("message", message);
        attributes.addFlashAttribute("messageType", "danger");
    }
}
