package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.category.CategoryType;
import com.ecoamazonas.eco_agua.supplier.Supplier;
import com.ecoamazonas.eco_agua.supplier.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentRepository paymentRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            ExpensePaymentRepository paymentRepository,
            CategoryRepository categoryRepository,
            SupplierRepository supplierRepository
    ) {
        this.expenseRepository = expenseRepository;
        this.paymentRepository = paymentRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Simple default: full amount is treated as non-taxed cost.
     * Later you can create another method to split base + IGV if needed.
     */
    private void fillTaxInfoWithoutVat(Expense expense) {
        BigDecimal amount = expense.getAmount() != null ? expense.getAmount() : BigDecimal.ZERO;
        expense.setTaxBase(amount);
        expense.setTaxIgv(BigDecimal.ZERO);
        expense.setTaxRate(BigDecimal.ZERO);
    }
    
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildObservationWithSupplierReference(
            String observation,
            Supplier supplier,
            String manualSupplierName
    ) {
        String cleanObservation = trimToNull(observation);

        if (supplier != null) {
            String prefix = "Proveedor: " + supplier.getName();
            return cleanObservation == null ? prefix : prefix + ". " + cleanObservation;
        }

        String cleanManualSupplier = trimToNull(manualSupplierName);
        if (cleanManualSupplier != null) {
            String prefix = "Referencia: " + cleanManualSupplier;
            return cleanObservation == null ? prefix : prefix + ". " + cleanObservation;
        }

        return cleanObservation;
    }

    // -------------------------------------------------------------------------
    // Registration methods
    // -------------------------------------------------------------------------

    @Transactional
    public Expense registerSimpleExpense(
            LocalDate expenseDate,
            Long categoryId,
            String observation,
            String voucherNumber,
            BigDecimal amount
    ) {
        return registerSimpleExpense(
                expenseDate,
                categoryId,
                null,
                null,
                observation,
                voucherNumber,
                amount
        );
    }

    @Transactional
    public Expense registerSimpleExpense(
            LocalDate expenseDate,
            Long categoryId,
            Long supplierId,
            String manualSupplierName,
            String observation,
            String voucherNumber,
            BigDecimal amount
    ) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        Supplier supplier = null;
        if (supplierId != null) {
            supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found."));
        }

        Expense expense = new Expense();
        expense.setCategory(category);
        expense.setSupplier(supplier);
        expense.setObservation(
                buildObservationWithSupplierReference(observation, supplier, manualSupplierName)
        );
        expense.setVoucherNumber(trimToNull(voucherNumber));
        expense.setAmount(amount);
        expense.setExpenseDate(expenseDate != null ? expenseDate : LocalDate.now());
        expense.setPaymentType(ExpensePaymentType.CASH);
        expense.setDebt(false);
        expense.setPaidAmount(amount);
        expense.setStatus(ExpenseStatus.PAID);

        fillTaxInfoWithoutVat(expense);

        return expenseRepository.save(expense);
    }
    
    @Transactional
    public Expense createSimpleExpense(Long categoryId,
                                       BigDecimal amount,
                                       String observation,
                                       LocalDate expenseDate) {
        return registerSimpleExpense(
                expenseDate,
                categoryId,
                null,
                null,
                observation,
                null,
                amount
        );
    }

    @Transactional
    public Expense registerDebtExpense(
            LocalDate expenseDate,
            Long categoryId,
            Long supplierId,
            String observation,
            String voucherNumber,
            BigDecimal amount,
            LocalDate dueDate,
            ExpensePaymentType paymentType
    ) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        Supplier supplier = null;
        if (supplierId != null) {
            supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found."));
        }

        Expense expense = new Expense();
        expense.setCategory(category);
        expense.setSupplier(supplier);
        expense.setObservation(observation);
        expense.setVoucherNumber(voucherNumber);
        expense.setAmount(amount);
        expense.setExpenseDate(expenseDate != null ? expenseDate : LocalDate.now());
        expense.setPaymentType(paymentType != null ? paymentType : ExpensePaymentType.CREDIT);
        expense.setDebt(true);
        expense.setPaidAmount(BigDecimal.ZERO);
        expense.setDueDate(dueDate);
        expense.setStatus(ExpenseStatus.OPEN);

        // New tax fields default
        fillTaxInfoWithoutVat(expense);

        return expenseRepository.save(expense);
    }

    @Transactional
    public ExpensePayment registerPayment(
            Long expenseId,
            LocalDate paymentDate,
            BigDecimal amount,
            String observation
    ) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found."));

        if (!expense.isDebt()) {
            throw new IllegalArgumentException("This expense is not marked as debt.");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        BigDecimal currentBalance = expense.getBalance();
        if (amount.compareTo(currentBalance) > 0) {
            throw new IllegalArgumentException("Payment amount cannot be greater than current balance.");
        }

        ExpensePayment payment = new ExpensePayment();
        payment.setPaymentDate(paymentDate != null ? paymentDate : LocalDate.now());
        payment.setAmount(amount);
        payment.setObservation(observation);
        payment.setExpense(expense);

        expense.addPayment(payment);
        expense.setPaidAmount(expense.getPaidAmount().add(amount));

        BigDecimal newBalance = expense.getBalance();
        if (newBalance.signum() == 0) {
            expense.setStatus(ExpenseStatus.PAID);
        } else {
            expense.setStatus(ExpenseStatus.PARTIAL);
        }

        expenseRepository.save(expense);
        return paymentRepository.save(payment);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Expense> findByDateRange(LocalDate start, LocalDate end) {
        return expenseRepository.findByExpenseDateBetweenOrderByExpenseDateAsc(start, end);
    }

    @Transactional(readOnly = true)
    public List<Expense> findDailyExpenses(LocalDate date) {
        return expenseRepository.findByExpenseDateAndDebtFalseOrderByExpenseDateAsc(date);
    }

    @Transactional(readOnly = true)
    public List<Expense> findOpenDebts(LocalDate start, LocalDate end) {
        Set<ExpenseStatus> statuses = EnumSet.of(ExpenseStatus.OPEN, ExpenseStatus.PARTIAL);
        // NOTE: currently using only date range, not statuses
        return expenseRepository.findByDebtTrueAndExpenseDateBetweenOrderByExpenseDateAsc(start, end);
    }

    @Transactional(readOnly = true)
    public List<Category> findExpenseCategories() {
        return categoryRepository.findByTypeAndActiveTrueOrderByNameAsc(CategoryType.EXPENSES);
    }

    // >>> Existing helper for cashflow (only real cash expenses, no open debts)
    @Transactional(readOnly = true)
    public List<Expense> findCashflowExpenses(LocalDate start, LocalDate end) {
        List<Expense> all = findByDateRange(start, end);
        return all.stream()
                .filter(expense -> !expense.isDebt())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Fixed costs helpers (to mirror the Excel "costos fijos mensuales" table)
    // -------------------------------------------------------------------------

    /**
     * Returns total fixed costs for a given period,
     * summing all expenses whose category type is EXPENSES.
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalFixedCosts(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required.");
        }

        LocalDate from = start;
        LocalDate to = end;
        if (to.isBefore(from)) {
            // Swap to keep a valid range
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        return expenseRepository.sumAmountByCategoryTypeAndPeriod(
                CategoryType.EXPENSES,
                from,
                to
        );
    }

    /**
     * Convenience method: returns total fixed costs for a full month (year + month).
     * Example: getMonthlyFixedCosts(2025, 11) -> November 2025.
     */
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyFixedCosts(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return getTotalFixedCosts(start, end);
    }

    /**
     * Returns a breakdown of fixed costs by category name for a given period.
     * Map key = category name, value = total amount for that category.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getFixedCostsByCategory(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required.");
        }

        LocalDate from = start;
        LocalDate to = end;
        if (to.isBefore(from)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        List<Object[]> rows = expenseRepository.sumAmountByCategoryNameForTypeAndPeriod(
                CategoryType.EXPENSES,
                from,
                to
        );

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String categoryName = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            result.put(categoryName, total);
        }

        return result;
    }
}
