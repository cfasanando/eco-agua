package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.order.OrderService;
import com.ecoamazonas.eco_agua.user.Employee;
import com.ecoamazonas.eco_agua.user.EmployeeRepository;
import com.ecoamazonas.eco_agua.user.JobPosition;
import com.ecoamazonas.eco_agua.user.PaymentMode;
import com.ecoamazonas.eco_agua.user.SalaryPeriod;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final OrderService orderService;
    private final EmployeeRepository employeeRepository;

    public ExpenseController(
            ExpenseService expenseService,
            OrderService orderService,
            EmployeeRepository employeeRepository
    ) {
        this.expenseService = expenseService;
        this.orderService = orderService;
        this.employeeRepository = employeeRepository;
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
        model.addAttribute("categories", expenseService.findExpenseCategories());
        model.addAttribute("suppliers", expenseService.findActiveSuppliers());
        model.addAttribute("supplies", expenseService.findActiveSupplies());
        model.addAttribute("employees", expenseService.findActiveEmployees());

        return "expenses/expenses_by_date";
    }

    /**
     * Simple expense registration.
     * Used both by the "Expenses by date" page and the "Daily expenses" widget.
     *
     * redirect = "BY_DATE" -> go back to /expenses/by-date
     * redirect = "HOME" -> go back to /home
     */
    @PostMapping("/save-simple")
    public String saveSimpleExpense(
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(name = "supplierId", required = false) Long supplierId,
            @RequestParam(name = "manualSupplierName", required = false) String manualSupplierName,
            @RequestParam(name = "supplyId", required = false) Long supplyId,
            @RequestParam(name = "employeeId", required = false) Long employeeId,
            @RequestParam(name = "employeePaymentType", required = false) String employeePaymentType,
            @RequestParam(name = "observation", required = false) String observation,
            @RequestParam(name = "voucherNumber", required = false) String voucherNumber,
            @RequestParam(name = "expenseDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate,
            @RequestParam(name = "redirect", defaultValue = "BY_DATE") String redirect,
            RedirectAttributes redirectAttributes
    ) {
        LocalDate effectiveDate = (expenseDate != null ? expenseDate : LocalDate.now());

        try {
            expenseService.registerSimpleExpense(
                    effectiveDate,
                    categoryId,
                    supplierId,
                    manualSupplierName,
                    supplyId,
                    employeeId,
                    employeePaymentType,
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

    @GetMapping("/personnel-preview")
    @ResponseBody
    public Map<String, Object> previewPersonnelExpense(
            @RequestParam("employeeId") Long employeeId,
            @RequestParam(name = "paymentType", required = false) String paymentType,
            @RequestParam(name = "expenseDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate
    ) {
        LocalDate effectiveDate = (expenseDate != null ? expenseDate : LocalDate.now());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", employeeId);
        payload.put("expenseDate", effectiveDate);
        payload.put("autoFilled", false);
        payload.put("suggestedAmount", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payload.put("totalSales", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payload.put("fixedComponent", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payload.put("commissionComponent", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payload.put("commissionRate", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        if (employeeId == null) {
            payload.put("message", "Select an employee first.");
            return payload;
        }

        String normalizedPaymentType = paymentType != null ? paymentType.trim().toUpperCase() : "";
        if (!"SALARY".equals(normalizedPaymentType)) {
            payload.put("message", "Automatic calculation is only applied for salary payments.");
            return payload;
        }

        Employee employee = employeeRepository.findByIdWithJobPosition(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        JobPosition position = employee.getJobPosition();
        if (position == null) {
            payload.put("employeeName", buildEmployeeDisplayName(employee));
            payload.put("message", "The selected employee has no job position configured.");
            return payload;
        }

        BigDecimal totalSales = normalizeMoney(orderService.getPaidSalesTotalForDate(effectiveDate));
        BigDecimal fixedComponent = resolveDailyFixedAmount(position);
        BigDecimal commissionRate = normalizeMoney(position.getCommissionRate());
        BigDecimal commissionComponent = totalSales
                .multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        PaymentMode paymentMode = position.getPaymentMode() != null
                ? position.getPaymentMode()
                : PaymentMode.FIXED;

        BigDecimal suggestedAmount;
        String explanation;

        switch (paymentMode) {
            case COMMISSION:
                suggestedAmount = commissionComponent;
                explanation = "Commission-only mode. Amount calculated as " +
                        commissionRate + "% of paid sales for the selected date.";
                break;

            case MIXED:
                suggestedAmount = fixedComponent.add(commissionComponent);
                explanation = "Mixed mode. Amount calculated as fixed daily amount plus " +
                        commissionRate + "% of paid sales for the selected date.";
                break;

            case FIXED:
            default:
                suggestedAmount = fixedComponent;
                explanation = "Fixed mode. Amount calculated from the job position salary settings.";
                break;
        }

        suggestedAmount = normalizeMoney(suggestedAmount);

        payload.put("autoFilled", true);
        payload.put("employeeName", buildEmployeeDisplayName(employee));
        payload.put("jobPositionName", position.getName());
        payload.put("paymentMode", paymentMode.name());
        payload.put("paymentModeLabel", position.getPaymentModeLabel());
        payload.put("salaryPeriod", position.getSalaryPeriod() != null ? position.getSalaryPeriod().name() : null);
        payload.put("salaryPeriodLabel", position.getSalaryPeriod() != null ? position.getSalaryPeriod().getLabel() : null);
        payload.put("totalSales", totalSales);
        payload.put("fixedComponent", fixedComponent);
        payload.put("commissionComponent", commissionComponent);
        payload.put("commissionRate", commissionRate);
        payload.put("suggestedAmount", suggestedAmount);
        payload.put("message", explanation);

        return payload;
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

        BigDecimal totalFixedCosts = expenseService.getTotalFixedCosts(start, end);
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

    private BigDecimal resolveDailyFixedAmount(JobPosition position) {
        if (position == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal reference = normalizeMoney(
                position.getSalaryAmount() != null && position.getSalaryAmount().signum() > 0
                        ? position.getSalaryAmount()
                        : position.getBaseSalary()
        );

        SalaryPeriod period = position.getSalaryPeriod() != null
                ? position.getSalaryPeriod()
                : SalaryPeriod.DAILY;

        BigDecimal dailyAmount = switch (period) {
            case DAILY -> reference;
            case WEEKLY -> reference.divide(new BigDecimal("7"), 2, RoundingMode.HALF_UP);
            case BIWEEKLY -> reference.divide(new BigDecimal("15"), 2, RoundingMode.HALF_UP);
            case MONTHLY -> reference.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
            case HOURLY -> reference;
        };

        return normalizeMoney(dailyAmount);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildEmployeeDisplayName(Employee employee) {
        if (employee == null) {
            return "";
        }

        String firstName = employee.getFirstName() != null ? employee.getFirstName().trim() : "";
        String lastName = employee.getLastName() != null ? employee.getLastName().trim() : "";

        return (firstName + " " + lastName).trim();
    }
}