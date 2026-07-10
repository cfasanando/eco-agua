package com.ecoamazonas.eco_agua.personalfinance;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;

@Controller
@RequestMapping("/gasto-claro")
public class PersonalFinanceController {

    private final PersonalFinanceService service;

    public PersonalFinanceController(PersonalFinanceService service) {
        this.service = service;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            Model model
    ) {
        YearMonth selectedMonth = selectedMonth(year, month);
        model.addAttribute("activePage", "gasto_claro_dashboard");
        model.addAttribute("selectedYear", selectedMonth.getYear());
        model.addAttribute("selectedMonth", selectedMonth.getMonthValue());
        model.addAttribute("dashboard", service.dashboard(selectedMonth));
        model.addAttribute("debts", service.debts().stream().limit(6).toList());
        model.addAttribute("incomeEvents", service.incomeEvents(selectedMonth).stream().limit(6).toList());
        return "personal_finance/dashboard";
    }


    @GetMapping("/monthly-plan")
    public String monthlyPlan(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "editId", required = false) Long editId,
            Model model
    ) {
        YearMonth selectedMonth = selectedMonth(year, month);
        PersonalFinancePaymentObligation form = editId == null
                ? new PersonalFinancePaymentObligation()
                : service.paymentObligation(editId);
        if (form.getDueDate() == null) {
            form.setDueDate(selectedMonth.atDay(Math.min(LocalDate.now().getDayOfMonth(), selectedMonth.lengthOfMonth())));
        }
        model.addAttribute("activePage", "gasto_claro_monthly_plan");
        model.addAttribute("selectedYear", selectedMonth.getYear());
        model.addAttribute("selectedMonth", selectedMonth.getMonthValue());
        model.addAttribute("plan", service.monthlyPlan(selectedMonth));
        model.addAttribute("liveSummary", service.monthlyLiveSummary(selectedMonth, PersonalFinanceCurrency.PEN));
        model.addAttribute("obligationForm", form);
        model.addAttribute("obligationGroups", PersonalFinanceObligationGroup.values());
        model.addAttribute("obligationStatuses", PersonalFinanceObligationStatus.values());
        model.addAttribute("obligationSources", PersonalFinanceObligationSourceType.values());
        model.addAttribute("priorities", PersonalFinancePriority.values());
        model.addAttribute("currencies", PersonalFinanceCurrency.values());
        return "personal_finance/monthly_plan";
    }

    @GetMapping("/monthly-priority")
    public String monthlyPriority(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "currency", required = false) PersonalFinanceCurrency currency,
            @RequestParam(name = "cashBasis", required = false) PersonalFinanceCashBasis cashBasis,
            @RequestParam(name = "availableCash", required = false) BigDecimal availableCash,
            Model model
    ) {
        YearMonth selectedMonth = selectedMonth(year, month);
        PersonalFinanceCurrency selectedCurrency = currency == null ? PersonalFinanceCurrency.PEN : currency;
        PersonalFinanceCashBasis selectedCashBasis = cashBasis == null ? PersonalFinanceCashBasis.EXPECTED : cashBasis;
        model.addAttribute("activePage", "gasto_claro_monthly_priority");
        model.addAttribute("selectedYear", selectedMonth.getYear());
        model.addAttribute("selectedMonth", selectedMonth.getMonthValue());
        model.addAttribute("selectedCurrency", selectedCurrency);
        model.addAttribute("selectedCashBasis", selectedCashBasis);
        model.addAttribute("availableCash", availableCash);
        model.addAttribute("currencies", PersonalFinanceCurrency.values());
        model.addAttribute("cashBases", PersonalFinanceCashBasis.values());
        model.addAttribute("priorityPlan", service.priorityPlan(selectedMonth, selectedCurrency, selectedCashBasis, availableCash));
        return "personal_finance/monthly_priority";
    }

    @PostMapping("/monthly-plan/obligations")
    public String saveMonthlyPlanObligation(
            @ModelAttribute("obligationForm") PersonalFinancePaymentObligation obligation,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            RedirectAttributes redirectAttributes
    ) {
        service.savePaymentObligation(obligation);
        redirectAttributes.addFlashAttribute("message", "Compromiso mensual guardado correctamente.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        YearMonth selectedMonth = selectedMonth(year, month);
        return "redirect:/gasto-claro/monthly-plan?year=" + selectedMonth.getYear() + "&month=" + selectedMonth.getMonthValue();
    }

    @PostMapping("/monthly-plan/obligations/{id}/delete")
    public String deleteMonthlyPlanObligation(
            @PathVariable("id") Long id,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            RedirectAttributes redirectAttributes
    ) {
        service.deletePaymentObligation(id);
        redirectAttributes.addFlashAttribute("message", "Compromiso mensual eliminado.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        YearMonth selectedMonth = selectedMonth(year, month);
        return "redirect:/gasto-claro/monthly-plan?year=" + selectedMonth.getYear() + "&month=" + selectedMonth.getMonthValue();
    }

    @PostMapping("/monthly-plan/generate-obligations")
    public String generateMonthlyObligations(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            RedirectAttributes redirectAttributes
    ) {
        YearMonth selectedMonth = selectedMonth(year, month);
        PersonalFinanceMonthGenerationResult result = service.generateMonthlyPlan(selectedMonth);
        redirectAttributes.addFlashAttribute("message", "Mes generado/actualizado. " + result.summary());
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/gasto-claro/monthly-plan?year=" + selectedMonth.getYear() + "&month=" + selectedMonth.getMonthValue();
    }

    @GetMapping("/debts")
    public String debts(@RequestParam(name = "editId", required = false) Long editId, Model model) {
        model.addAttribute("activePage", "gasto_claro_debts");
        model.addAttribute("debts", service.debts());
        model.addAttribute("debtForm", editId == null ? new PersonalFinanceDebt() : service.debt(editId));
        model.addAttribute("debtTypes", PersonalFinanceDebtType.values());
        model.addAttribute("debtStatuses", PersonalFinanceDebtStatus.values());
        model.addAttribute("debtHolderTypes", PersonalFinanceDebtHolderType.values());
        model.addAttribute("scheduleModes", PersonalFinanceDebtScheduleMode.values());
        model.addAttribute("collectionStatuses", PersonalFinanceCollectionStatus.values());
        model.addAttribute("negotiationStatuses", PersonalFinanceNegotiationStatus.values());
        model.addAttribute("priorities", PersonalFinancePriority.values());
        model.addAttribute("currencies", PersonalFinanceCurrency.values());
        return "personal_finance/debts";
    }

    @PostMapping("/debts")
    public String saveDebt(@ModelAttribute("debtForm") PersonalFinanceDebt debt, RedirectAttributes redirectAttributes) {
        service.saveDebt(debt);
        redirectAttributes.addFlashAttribute("message", "Deuda guardada correctamente.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/gasto-claro/debts";
    }

    @PostMapping("/debts/{id}/delete")
    public String deleteDebt(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        service.deleteDebt(id);
        redirectAttributes.addFlashAttribute("message", "Deuda eliminada del módulo personal.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/gasto-claro/debts";
    }

    @GetMapping("/debts/{id}/voluntary-payment")
    public String voluntaryPayment(
            @PathVariable("id") Long id,
            Model model
    ) {
        PersonalFinanceDebt debt = service.debt(id);
        PersonalFinanceVoluntaryPaymentForm form = new PersonalFinanceVoluntaryPaymentForm();
        form.setCurrency(debt.getCurrency());
        form.setPriority(debt.getPriority());
        model.addAttribute("activePage", "gasto_claro_debts");
        model.addAttribute("debt", debt);
        model.addAttribute("paymentForm", form);
        model.addAttribute("priorities", PersonalFinancePriority.values());
        model.addAttribute("currencies", PersonalFinanceCurrency.values());
        return "personal_finance/debt_voluntary_payment";
    }

    @PostMapping("/debts/{id}/voluntary-payment")
    public String createVoluntaryPayment(
            @PathVariable("id") Long id,
            @ModelAttribute("paymentForm") PersonalFinanceVoluntaryPaymentForm form,
            RedirectAttributes redirectAttributes
    ) {
        try {
            PersonalFinancePaymentObligation obligation = service.createVoluntaryPayment(id, form);
            redirectAttributes.addFlashAttribute("message", "Abono voluntario agregado al plan mensual.");
            redirectAttributes.addFlashAttribute("messageType", "success");
            YearMonth selectedMonth = YearMonth.from(obligation.getDueDate());
            return "redirect:/gasto-claro/monthly-plan?year=" + selectedMonth.getYear() + "&month=" + selectedMonth.getMonthValue();
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "info");
            return "redirect:/gasto-claro/debts/" + id + "/voluntary-payment";
        }
    }

    @GetMapping("/debts/{id}/schedule")
    public String debtSchedule(
            @PathVariable("id") Long id,
            @RequestParam(name = "editLineId", required = false) Long editLineId,
            Model model
    ) {
        PersonalFinanceDebt debt = service.debt(id);
        PersonalFinanceDebtScheduleLine form = editLineId == null ? new PersonalFinanceDebtScheduleLine() : service.debtScheduleLine(editLineId);
        if (form.getDueDate() == null) {
            form.setDueDate(LocalDate.now());
        }
        model.addAttribute("activePage", "gasto_claro_debts");
        model.addAttribute("debt", debt);
        model.addAttribute("scheduleLines", service.debtSchedule(id));
        model.addAttribute("lineForm", form);
        model.addAttribute("lineTypes", PersonalFinanceScheduleLineType.values());
        model.addAttribute("currentYear", YearMonth.now().getYear());
        model.addAttribute("currentMonth", YearMonth.now().getMonthValue());
        model.addAttribute("obligationStatuses", PersonalFinanceObligationStatus.values());
        model.addAttribute("currencies", PersonalFinanceCurrency.values());
        return "personal_finance/debt_schedule";
    }

    @GetMapping("/debts/{id}/bank-schedule")
    public String bankSchedule(@PathVariable("id") Long id, Model model) {
        PersonalFinanceDebt debt = service.debt(id);
        model.addAttribute("activePage", "gasto_claro_debts");
        model.addAttribute("debt", debt);
        model.addAttribute("scheduleLines", service.debtSchedule(id));
        model.addAttribute("summary", service.bankScheduleSummary(id));
        model.addAttribute("today", LocalDate.now());
        return "personal_finance/bank_schedule";
    }

    @PostMapping("/debts/{id}/bank-schedule/import")
    public String importBankSchedule(
            @PathVariable("id") Long id,
            @RequestParam(name = "scheduleText", required = false) String scheduleText,
            @RequestParam(name = "scheduleFile", required = false) MultipartFile scheduleFile,
            @RequestParam(name = "replaceExisting", defaultValue = "false") boolean replaceExisting,
            RedirectAttributes redirectAttributes
    ) {
        String content = scheduleText == null ? "" : scheduleText;
        try {
            if (scheduleFile != null && !scheduleFile.isEmpty()) {
                String fileContent = new String(scheduleFile.getBytes(), StandardCharsets.UTF_8);
                content = content.isBlank() ? fileContent : content + "\n" + fileContent;
            }
            PersonalFinanceBankScheduleImportResult result = service.importBankSchedule(id, content, replaceExisting);
            redirectAttributes.addFlashAttribute("message", result.success()
                    ? result.summary()
                    : result.summary() + " " + String.join(" | ", result.errors()));
            redirectAttributes.addFlashAttribute("messageType", result.success() ? "success" : "danger");
        } catch (IOException exception) {
            redirectAttributes.addFlashAttribute("message", "No se pudo leer el archivo seleccionado.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/gasto-claro/debts/" + id + "/bank-schedule";
    }

    @PostMapping("/debts/{id}/bank-schedule/mark-paid-through")
    public String markBankSchedulePaidThrough(
            @PathVariable("id") Long id,
            @RequestParam("throughLineNumber") Integer throughLineNumber,
            RedirectAttributes redirectAttributes
    ) {
        try {
            int updated = service.markBankSchedulePaidThrough(id, throughLineNumber);
            redirectAttributes.addFlashAttribute("message", "Cuotas marcadas como pagadas: " + updated + ".");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/gasto-claro/debts/" + id + "/bank-schedule";
    }

    @PostMapping("/debts/{debtId}/bank-schedule/lines/{lineId}/paid")
    public String setBankScheduleLinePaid(
            @PathVariable("debtId") Long debtId,
            @PathVariable("lineId") Long lineId,
            @RequestParam(name = "paid", defaultValue = "true") boolean paid,
            RedirectAttributes redirectAttributes
    ) {
        try {
            service.setBankScheduleLinePaid(debtId, lineId, paid);
            redirectAttributes.addFlashAttribute("message", paid ? "Cuota marcada como pagada." : "Cuota reabierta como pendiente.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/gasto-claro/debts/" + debtId + "/bank-schedule";
    }

    @PostMapping("/debts/{id}/schedule/generate")
    public String generateDebtSchedule(
            @PathVariable("id") Long id,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "months", required = false) Integer months,
            RedirectAttributes redirectAttributes
    ) {
        YearMonth selectedMonth = selectedMonth(year, month);
        try {
            PersonalFinanceDebtScheduleGenerationResult result = service.generateDebtSchedule(id, selectedMonth, months);
            redirectAttributes.addFlashAttribute("message", result.summary());
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("message", exception.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/gasto-claro/debts/" + id + "/schedule";
    }

    @PostMapping("/debts/{id}/schedule-lines")
    public String saveDebtScheduleLine(
            @PathVariable("id") Long id,
            @ModelAttribute("lineForm") PersonalFinanceDebtScheduleLine line,
            RedirectAttributes redirectAttributes
    ) {
        service.saveDebtScheduleLine(id, line);
        redirectAttributes.addFlashAttribute("message", "Fila de cronograma guardada correctamente.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/gasto-claro/debts/" + id + "/schedule";
    }

    @PostMapping("/debts/{debtId}/schedule-lines/{lineId}/delete")
    public String deleteDebtScheduleLine(
            @PathVariable("debtId") Long debtId,
            @PathVariable("lineId") Long lineId,
            RedirectAttributes redirectAttributes
    ) {
        service.deleteDebtScheduleLine(lineId);
        redirectAttributes.addFlashAttribute("message", "Fila de cronograma eliminada.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/gasto-claro/debts/" + debtId + "/schedule";
    }

    @GetMapping("/fixed-expenses")
    public String fixedExpenses(@RequestParam(name = "editId", required = false) Long editId, Model model) {
        model.addAttribute("activePage", "gasto_claro_fixed_expenses");
        model.addAttribute("expenses", service.fixedExpenses());
        model.addAttribute("expenseForm", editId == null ? new PersonalFinanceFixedExpense() : service.fixedExpense(editId));
        model.addAttribute("categories", PersonalFinanceExpenseCategory.values());
        model.addAttribute("frequencies", PersonalFinanceFrequency.values());
        model.addAttribute("currencies", PersonalFinanceCurrency.values());
        return "personal_finance/fixed_expenses";
    }

    @PostMapping("/fixed-expenses")
    public String saveFixedExpense(@ModelAttribute("expenseForm") PersonalFinanceFixedExpense expense, RedirectAttributes redirectAttributes) {
        service.saveFixedExpense(expense);
        redirectAttributes.addFlashAttribute("message", "Gasto fijo guardado correctamente.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/gasto-claro/fixed-expenses";
    }

    @PostMapping("/fixed-expenses/{id}/delete")
    public String deleteFixedExpense(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        service.deleteFixedExpense(id);
        redirectAttributes.addFlashAttribute("message", "Gasto fijo eliminado del módulo personal.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/gasto-claro/fixed-expenses";
    }

    @GetMapping("/income-sources")
    public String incomeSources(@RequestParam(name = "editId", required = false) Long editId, Model model) {
        model.addAttribute("activePage", "gasto_claro_income_sources");
        model.addAttribute("sources", service.incomeSources());
        model.addAttribute("sourceForm", editId == null ? new PersonalFinanceIncomeSource() : service.incomeSource(editId));
        model.addAttribute("incomeTypes", PersonalFinanceIncomeType.values());
        model.addAttribute("frequencies", PersonalFinanceFrequency.values());
        model.addAttribute("currencies", PersonalFinanceCurrency.values());
        return "personal_finance/income_sources";
    }

    @PostMapping("/income-sources")
    public String saveIncomeSource(@ModelAttribute("sourceForm") PersonalFinanceIncomeSource source, RedirectAttributes redirectAttributes) {
        service.saveIncomeSource(source);
        redirectAttributes.addFlashAttribute("message", "Fuente de ingreso guardada correctamente.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/gasto-claro/income-sources";
    }

    @PostMapping("/income-sources/{id}/delete")
    public String deleteIncomeSource(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        service.deleteIncomeSource(id);
        redirectAttributes.addFlashAttribute("message", "Fuente de ingreso eliminada del módulo personal.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/gasto-claro/income-sources";
    }

    @GetMapping("/income-events")
    public String incomeEvents(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "editId", required = false) Long editId,
            Model model
    ) {
        YearMonth selectedMonth = selectedMonth(year, month);
        model.addAttribute("activePage", "gasto_claro_income_events");
        model.addAttribute("selectedYear", selectedMonth.getYear());
        model.addAttribute("selectedMonth", selectedMonth.getMonthValue());
        PersonalFinanceIncomeEvent form = editId == null ? new PersonalFinanceIncomeEvent() : service.incomeEvent(editId);
        if (form.getExpectedDate() == null) {
            form.setExpectedDate(LocalDate.now());
        }
        model.addAttribute("eventForm", form);
        model.addAttribute("events", service.incomeEvents(selectedMonth));
        model.addAttribute("sources", service.activeIncomeSources());
        model.addAttribute("incomeStatuses", PersonalFinanceIncomeStatus.values());
        model.addAttribute("currencies", PersonalFinanceCurrency.values());
        return "personal_finance/income_events";
    }

    @PostMapping("/income-events")
    public String saveIncomeEvent(
            @ModelAttribute("eventForm") PersonalFinanceIncomeEvent event,
            @RequestParam(name = "sourceId", required = false) Long sourceId,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            RedirectAttributes redirectAttributes
    ) {
        service.saveIncomeEvent(event, sourceId);
        redirectAttributes.addFlashAttribute("message", "Ingreso programado guardado correctamente.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        YearMonth selectedMonth = selectedMonth(year, month);
        return "redirect:/gasto-claro/income-events?year=" + selectedMonth.getYear() + "&month=" + selectedMonth.getMonthValue();
    }

    @PostMapping("/income-events/{id}/delete")
    public String deleteIncomeEvent(
            @PathVariable("id") Long id,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            RedirectAttributes redirectAttributes
    ) {
        service.deleteIncomeEvent(id);
        redirectAttributes.addFlashAttribute("message", "Ingreso programado eliminado del módulo personal.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        YearMonth selectedMonth = selectedMonth(year, month);
        return "redirect:/gasto-claro/income-events?year=" + selectedMonth.getYear() + "&month=" + selectedMonth.getMonthValue();
    }

    private YearMonth selectedMonth(Integer year, Integer month) {
        YearMonth now = YearMonth.now();
        int safeYear = year == null ? now.getYear() : year;
        int safeMonth = month == null ? now.getMonthValue() : Math.max(1, Math.min(12, month));
        return YearMonth.of(safeYear, safeMonth);
    }
}
