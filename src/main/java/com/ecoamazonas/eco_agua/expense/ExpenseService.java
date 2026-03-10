package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.category.CategoryType;
import com.ecoamazonas.eco_agua.product.cost.PeriodExpenseLine;
import com.ecoamazonas.eco_agua.supplier.Supplier;
import com.ecoamazonas.eco_agua.supplier.SupplierRepository;
import com.ecoamazonas.eco_agua.supply.Supply;
import com.ecoamazonas.eco_agua.supply.SupplyRepository;
import com.ecoamazonas.eco_agua.user.Employee;
import com.ecoamazonas.eco_agua.user.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private static final List<Long> FIXED_COST_CATEGORY_IDS = List.of(
            17L, // Luz local
            18L, // Agua local
            19L, // Alquiler local
            20L, // Contador
            21L, // Sunat
            22L, // Boletas y facturas
            28L, // Cochera
            29L, // Mantenimiento y aceite furgón
            31L, // Detergente
            32L, // Escobilla
            34L  // Declaración IE
    );

    private enum ExpenseInputContext {
        SUPPLIER,
        SUPPLY,
        PERSONNEL
    }

    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentRepository paymentRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final SupplyRepository supplyRepository;
    private final EmployeeRepository employeeRepository;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            ExpensePaymentRepository paymentRepository,
            CategoryRepository categoryRepository,
            SupplierRepository supplierRepository,
            SupplyRepository supplyRepository,
            EmployeeRepository employeeRepository
    ) {
        this.expenseRepository = expenseRepository;
        this.paymentRepository = paymentRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.supplyRepository = supplyRepository;
        this.employeeRepository = employeeRepository;
    }

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

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }

        return false;
    }

    private ExpenseInputContext resolveExpenseInputContext(Category category) {
        String normalizedCategoryName = normalizeText(category != null ? category.getName() : null);

        if (normalizedCategoryName.contains("insumo")) {
            return ExpenseInputContext.SUPPLY;
        }

        if (containsAny(normalizedCategoryName, "personal", "repartidor", "llenador", "lavador")) {
            return ExpenseInputContext.PERSONNEL;
        }

        return ExpenseInputContext.SUPPLIER;
    }

    private String buildEmployeeDisplayName(Employee employee) {
        if (employee == null) {
            return null;
        }

        String firstName = trimToNull(employee.getFirstName());
        String lastName = trimToNull(employee.getLastName());

        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }

        return firstName + " " + lastName;
    }

    private String mapEmployeePaymentTypeLabel(String employeePaymentType) {
        String value = trimToNull(employeePaymentType);
        if (value == null) {
            return null;
        }

        return switch (value.toUpperCase()) {
            case "SALARY" -> "Salario";
            case "ADVANCE" -> "Adelanto";
            case "DISCOUNT" -> "Descuento";
            default -> value;
        };
    }

    private String buildObservationWithContext(
            String observation,
            Supplier supplier,
            String manualSupplierName,
            Supply supply,
            Employee employee,
            String employeePaymentType
    ) {
        List<String> prefixes = new ArrayList<>();
        String cleanObservation = trimToNull(observation);
        String cleanManualSupplier = trimToNull(manualSupplierName);
        String employeeName = buildEmployeeDisplayName(employee);
        String paymentTypeLabel = mapEmployeePaymentTypeLabel(employeePaymentType);

        if (supplier != null) {
            prefixes.add("Proveedor: " + supplier.getName());
        }
        if (cleanManualSupplier != null) {
            prefixes.add("Referencia: " + cleanManualSupplier);
        }
        if (supply != null) {
            prefixes.add("Insumo: " + supply.getName());
        }
        if (employeeName != null) {
            prefixes.add("Personal: " + employeeName);
        }
        if (paymentTypeLabel != null) {
            prefixes.add("Tipo pago: " + paymentTypeLabel);
        }

        if (prefixes.isEmpty()) {
            return cleanObservation;
        }

        String prefix = String.join(". ", prefixes);
        return cleanObservation == null ? prefix : prefix + ". " + cleanObservation;
    }

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
                null,
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
        return registerSimpleExpense(
                expenseDate,
                categoryId,
                supplierId,
                manualSupplierName,
                null,
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
            Long supplyId,
            Long employeeId,
            String employeePaymentType,
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

        Supply supply = null;
        if (supplyId != null) {
            supply = supplyRepository.findById(supplyId)
                    .orElseThrow(() -> new IllegalArgumentException("Supply not found."));
        }

        Employee employee = null;
        if (employeeId != null) {
            employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
        }

        ExpenseInputContext context = resolveExpenseInputContext(category);

        if (context == ExpenseInputContext.SUPPLY && supply == null) {
            throw new IllegalArgumentException("Supply is required for this category.");
        }

        if (context == ExpenseInputContext.PERSONNEL) {
            if (employee == null) {
                throw new IllegalArgumentException("Employee is required for this category.");
            }

            if (trimToNull(employeePaymentType) == null) {
                throw new IllegalArgumentException("Payment type is required for this category.");
            }
        }

        Expense expense = new Expense();
        expense.setCategory(category);
        expense.setSupplier(supplier);
        expense.setObservation(
                buildObservationWithContext(
                        observation,
                        supplier,
                        manualSupplierName,
                        supply,
                        employee,
                        employeePaymentType
                )
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

    @Transactional(readOnly = true)
    public Expense findById(Long expenseId) {
        Expense expense = expenseRepository.findDetailedById(expenseId);
        if (expense == null) {
            throw new IllegalArgumentException("Expense not found.");
        }

        return expense;
    }

    private void ensureExpenseCanBeModified(Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense not found.");
        }

        if (expense.isDebt()) {
            throw new IllegalArgumentException("Debt expenses cannot be edited or deleted from this screen.");
        }

        if (expense.getPayments() != null && !expense.getPayments().isEmpty()) {
            throw new IllegalArgumentException("Expenses with registered payments cannot be edited or deleted.");
        }
    }

    @Transactional
    public Expense updateSimpleExpense(
            Long expenseId,
            Long categoryId,
            String observation,
            BigDecimal amount,
            LocalDate expenseDate
    ) {
        if (expenseId == null) {
            throw new IllegalArgumentException("Expense id is required.");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        Expense expense = findById(expenseId);
        ensureExpenseCanBeModified(expense);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));

        expense.setCategory(category);
        expense.setObservation(trimToNull(observation));
        expense.setAmount(amount);
        expense.setExpenseDate(expenseDate != null ? expenseDate : expense.getExpenseDate());
        expense.setPaymentType(ExpensePaymentType.CASH);
        expense.setDebt(false);
        expense.setPaidAmount(amount);
        expense.setStatus(ExpenseStatus.PAID);

        fillTaxInfoWithoutVat(expense);

        return expenseRepository.save(expense);
    }

    @Transactional
    public void deleteSimpleExpense(Long expenseId) {
        if (expenseId == null) {
            throw new IllegalArgumentException("Expense id is required.");
        }

        Expense expense = findById(expenseId);
        ensureExpenseCanBeModified(expense);
        expenseRepository.delete(expense);
    }

    @Transactional
    public Expense createSimpleExpense(Long categoryId, BigDecimal amount, String observation, LocalDate expenseDate) {
        return registerSimpleExpense(
                expenseDate,
                categoryId,
                null,
                null,
                null,
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
        return expenseRepository.findByDebtTrueAndExpenseDateBetweenOrderByExpenseDateAsc(start, end);
    }

    @Transactional(readOnly = true)
    public List<Category> findExpenseCategories() {
        return categoryRepository.findByTypeInAndActiveTrueOrderByNameAsc(CategoryType.expenseTypes());
    }

    @Transactional(readOnly = true)
    public List<Supplier> findActiveSuppliers() {
        return supplierRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Supply> findActiveSupplies() {
        return supplyRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Employee> findActiveEmployees() {
        return employeeRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Expense> findCashflowExpenses(LocalDate start, LocalDate end) {
        List<Expense> all = findByDateRange(start, end);
        return all.stream()
                .filter(expense -> !expense.isDebt())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalFixedCosts(LocalDate start, LocalDate end) {
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

        BigDecimal total = expenseRepository.sumAmountByCategoryIdsAndPeriod(
                FIXED_COST_CATEGORY_IDS,
                from,
                to
        );

        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthlyFixedCosts(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return getTotalFixedCosts(start, end);
    }

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

        List<PeriodExpenseLine> rows = expenseRepository.sumAmountByCategoryIdsGrouped(
                FIXED_COST_CATEGORY_IDS,
                from,
                to
        );

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (PeriodExpenseLine row : rows) {
            result.put(
                    row.getCategoryName(),
                    row.getTotalAmount() != null ? row.getTotalAmount() : BigDecimal.ZERO
            );
        }

        return result;
    }
}
