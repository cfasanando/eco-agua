package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.category.CategoryType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final CategoryRepository categoryRepository;

    public ExpenseController(
            ExpenseService expenseService,
            CategoryRepository categoryRepository
    ) {
        this.expenseService = expenseService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/by-date")
    public String expensesByDate(
            @RequestParam(name = "filter", defaultValue = "DAY") String filter,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        LocalDate base = (date != null ? date : today);

        LocalDate start;
        LocalDate end;

        switch (filter.toUpperCase()) {
            case "PERIOD":
                start = (from != null ? from : base);
                end = (to != null ? to : base);
                break;
            case "DAY":
            default:
                start = base;
                end = base;
                break;
        }

        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        List<Expense> expenses = expenseService.findByDateRange(start, end);

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("activePage", "expenses_by_date");
        model.addAttribute("filter", filter);
        model.addAttribute("date", base);
        model.addAttribute("fromDate", start);
        model.addAttribute("toDate", end);
        model.addAttribute("expenses", expenses);
        model.addAttribute("totalAmount", total);
        model.addAttribute("categories",
                categoryRepository.findByTypeAndActiveTrueOrderByNameAsc(CategoryType.EXPENSES));

        return "expenses/expenses_by_date";
    }

    /**
     * Simple expense registration.
     * Used both by the "Expenses by date" page and the "Daily expenses" widget.
     *
     * redirect = "BY_DATE" -> go back to /expenses/by-date
     * redirect = "HOME"    -> go back to /home
     */
    @PostMapping("/save-simple")
    public String saveSimpleExpense(
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(name = "observation", required = false) String observation,
            @RequestParam(name = "voucherNumber", required = false) String voucherNumber,
            @RequestParam(name = "expenseDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate,
            @RequestParam(name = "redirect", defaultValue = "BY_DATE") String redirect,
            RedirectAttributes redirectAttributes
    ) {
        // Normalize date: if null, use today
        LocalDate effectiveDate = (expenseDate != null ? expenseDate : LocalDate.now());

        try {
            expenseService.registerSimpleExpense(
                    effectiveDate,
                    categoryId,
                    observation,
                    voucherNumber,
                    amount
            );
            redirectAttributes.addFlashAttribute("message", "Expense saved successfully.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error while saving expense: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        if ("HOME".equalsIgnoreCase(redirect)) {
            return "redirect:/home";
        }

        return "redirect:/expenses/by-date";
    }

    @GetMapping("/debts")
    public String debts(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        LocalDate start = (from != null ? from : today);
        LocalDate end = (to != null ? to : today);

        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        List<Expense> debts = expenseService.findOpenDebts(start, end);

        model.addAttribute("activePage", "expenses_debts");
        model.addAttribute("fromDate", start);
        model.addAttribute("toDate", end);
        model.addAttribute("debts", debts);

        // 👇 aquí estaba el problema
        return "expenses/expenses_debts";
    }


    @PostMapping("/{id}/payments")
    public String registerPayment(
            @PathVariable("id") Long expenseId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(name = "paymentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
            @RequestParam(name = "observation", required = false) String observation,
            RedirectAttributes redirectAttributes
    ) {
        try {
            expenseService.registerPayment(expenseId, paymentDate, amount, observation);
            redirectAttributes.addFlashAttribute("message", "Payment registered successfully.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error while saving payment: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/expenses/debts";
    }
    
    @GetMapping("/fixed-costs")
    public String fixedCostsSummary(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        int y = (year != null ? year : today.getYear());
        int m = (month != null ? month : today.getMonthValue());

        LocalDate start = LocalDate.of(y, m, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // Total fixed costs for the selected month
        BigDecimal totalFixedCosts = expenseService.getTotalFixedCosts(start, end);

        // Breakdown by category
        var breakdown = expenseService.getFixedCostsByCategory(start, end);

        model.addAttribute("activePage", "expenses_fixed_costs");
        model.addAttribute("year", y);
        model.addAttribute("month", m);
        model.addAttribute("fromDate", start);
        model.addAttribute("toDate", end);
        model.addAttribute("totalFixedCosts", totalFixedCosts);
        model.addAttribute("breakdown", breakdown);

        return "expenses/expenses_fixed_costs";
    }

}
