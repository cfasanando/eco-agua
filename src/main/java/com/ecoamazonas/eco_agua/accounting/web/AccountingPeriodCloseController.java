package com.ecoamazonas.eco_agua.accounting.web;

import com.ecoamazonas.eco_agua.accounting.AccountingPeriodCloseSnapshot;
import com.ecoamazonas.eco_agua.accounting.service.AccountingPeriodCloseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Controller
public class AccountingPeriodCloseController {

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountingPeriodCloseService periodCloseService;

    public AccountingPeriodCloseController(AccountingPeriodCloseService periodCloseService) {
        this.periodCloseService = periodCloseService;
    }

    @GetMapping("/accounting/period-close")
    public String index(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            Model model
    ) {
        AccountingPeriodCloseSnapshot snapshot = periodCloseService.build(year, month);
        YearMonth current = YearMonth.now();

        model.addAttribute("activePage", "accounting_period_close");
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("selectedYear", snapshot.getYear());
        model.addAttribute("selectedMonth", snapshot.getMonth());
        model.addAttribute("yearOptions", IntStream.rangeClosed(current.getYear() - 2, current.getYear() + 1).boxed().toList());
        model.addAttribute("monthOptions", monthOptions());
        model.addAttribute("periodLabel", buildPeriodLabel(snapshot.getStartDate(), snapshot.getEndDate()));

        return "accounting/period_close";
    }

    @PostMapping("/accounting/period-close/close")
    public String closePeriod(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam(name = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes
    ) {
        try {
            periodCloseService.closePeriod(year, month, notes);
            redirectAttributes.addFlashAttribute("message", "Período cerrado correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "warning");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo cerrar el período.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return redirectToPeriod(year, month);
    }

    @PostMapping("/accounting/period-close/reopen")
    public String reopenPeriod(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam(name = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes
    ) {
        try {
            periodCloseService.reopenPeriod(year, month, notes);
            redirectAttributes.addFlashAttribute("message", "Período reabierto correctamente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("message", ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "warning");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "No se pudo reabrir el período.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return redirectToPeriod(year, month);
    }

    private String redirectToPeriod(Integer year, Integer month) {
        return "redirect:/accounting/period-close?year=" + year + "&month=" + month;
    }

    private String buildPeriodLabel(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "sin período definido";
        }
        return startDate.format(LABEL_FORMATTER) + " al " + endDate.format(LABEL_FORMATTER);
    }

    private List<Map<String, Object>> monthOptions() {
        return List.of(
                Map.of("value", 1, "label", "Enero"),
                Map.of("value", 2, "label", "Febrero"),
                Map.of("value", 3, "label", "Marzo"),
                Map.of("value", 4, "label", "Abril"),
                Map.of("value", 5, "label", "Mayo"),
                Map.of("value", 6, "label", "Junio"),
                Map.of("value", 7, "label", "Julio"),
                Map.of("value", 8, "label", "Agosto"),
                Map.of("value", 9, "label", "Septiembre"),
                Map.of("value", 10, "label", "Octubre"),
                Map.of("value", 11, "label", "Noviembre"),
                Map.of("value", 12, "label", "Diciembre")
        );
    }
}
