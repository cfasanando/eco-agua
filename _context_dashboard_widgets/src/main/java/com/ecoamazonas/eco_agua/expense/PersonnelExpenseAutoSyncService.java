package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.user.Employee;
import com.ecoamazonas.eco_agua.user.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PersonnelExpenseAutoSyncService {

    private static final Pattern EMPLOYEE_NAME_PATTERN =
            Pattern.compile("(?:^|\\.\\s)Personal:\\s([^.]+?)(?:\\.\\s|$)");
    private static final Pattern PAYMENT_TYPE_PATTERN =
            Pattern.compile("(?:^|\\.\\s)Tipo pago:\\s([^.]+?)(?:\\.\\s|$)", Pattern.CASE_INSENSITIVE);

    private final ExpenseRepository expenseRepository;
    private final EmployeeRepository employeeRepository;
    private final PersonnelExpenseCalculatorService personnelExpenseCalculatorService;

    public PersonnelExpenseAutoSyncService(
            ExpenseRepository expenseRepository,
            EmployeeRepository employeeRepository,
            PersonnelExpenseCalculatorService personnelExpenseCalculatorService
    ) {
        this.expenseRepository = expenseRepository;
        this.employeeRepository = employeeRepository;
        this.personnelExpenseCalculatorService = personnelExpenseCalculatorService;
    }

    public void syncSalaryExpensesForDate(LocalDate expenseDate) {
        LocalDate effectiveDate = (expenseDate != null ? expenseDate : LocalDate.now());

        List<Expense> expenses = expenseRepository.findByExpenseDateAndDebtFalseOrderByExpenseDateAsc(effectiveDate);
        if (expenses.isEmpty()) {
            return;
        }

        Map<String, Employee> employeesByNormalizedName = buildEmployeeLookup();
        if (employeesByNormalizedName.isEmpty()) {
            return;
        }

        for (Expense expense : expenses) {
            if (!isAutoSalaryExpense(expense)) {
                continue;
            }

            String employeeName = extractEmployeeName(expense.getObservation());
            if (employeeName == null) {
                continue;
            }

            Employee employee = employeesByNormalizedName.get(normalizeText(employeeName));
            if (employee == null || employee.getJobPosition() == null) {
                continue;
            }

            PersonnelExpenseCalculation calculation =
                    personnelExpenseCalculatorService.calculateSalaryExpense(employee, effectiveDate);

            if (!calculation.isAutoFilled()) {
                continue;
            }

            BigDecimal suggestedAmount = normalizeMoney(calculation.getSuggestedAmount());
            BigDecimal currentAmount = normalizeMoney(expense.getAmount());

            if (currentAmount.compareTo(suggestedAmount) == 0) {
                continue;
            }

            expense.setAmount(suggestedAmount);
            expense.setPaidAmount(suggestedAmount);
            expense.setDebt(false);
            expense.setStatus(ExpenseStatus.PAID);
            expense.setTaxBase(suggestedAmount);
            expense.setTaxIgv(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            expense.setTaxRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

            if (expense.getPaymentType() == null) {
                expense.setPaymentType(ExpensePaymentType.CASH);
            }

            expenseRepository.save(expense);
        }
    }

    private Map<String, Employee> buildEmployeeLookup() {
        List<Employee> employees = employeeRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc();
        Map<String, Employee> lookup = new LinkedHashMap<>();

        for (Employee employee : employees) {
            String key = normalizeText(buildEmployeeDisplayName(employee));
            if (key.isBlank() || lookup.containsKey(key)) {
                continue;
            }

            lookup.put(key, employee);
        }

        return lookup;
    }

    private boolean isAutoSalaryExpense(Expense expense) {
        if (expense == null || expense.isDebt()) {
            return false;
        }

        String paymentTypeLabel = extractPaymentTypeLabel(expense.getObservation());
        return paymentTypeLabel != null && "salario".equals(normalizeText(paymentTypeLabel));
    }

    private String extractEmployeeName(String observation) {
        if (observation == null || observation.isBlank()) {
            return null;
        }

        Matcher matcher = EMPLOYEE_NAME_PATTERN.matcher(observation);
        if (!matcher.find()) {
            return null;
        }

        String value = matcher.group(1);
        return value != null ? value.trim() : null;
    }

    private String extractPaymentTypeLabel(String observation) {
        if (observation == null || observation.isBlank()) {
            return null;
        }

        Matcher matcher = PAYMENT_TYPE_PATTERN.matcher(observation);
        if (!matcher.find()) {
            return null;
        }

        String value = matcher.group(1);
        return value != null ? value.trim() : null;
    }

    private String buildEmployeeDisplayName(Employee employee) {
        if (employee == null) {
            return "";
        }

        String firstName = employee.getFirstName() != null ? employee.getFirstName().trim() : "";
        String lastName = employee.getLastName() != null ? employee.getLastName().trim() : "";

        return (firstName + " " + lastName).trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}