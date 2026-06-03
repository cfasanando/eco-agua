package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.accounting.AccountingAutomationEvent;
import com.ecoamazonas.eco_agua.accounting.service.AccountingAutoJournalEntryService;
import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.category.CategoryRepository;
import com.ecoamazonas.eco_agua.category.CategoryType;
import com.ecoamazonas.eco_agua.category.CostBehavior;
import com.ecoamazonas.eco_agua.category.PersonnelMode;
import com.ecoamazonas.eco_agua.inventory.InventoryMovementRepository;
import com.ecoamazonas.eco_agua.inventory.InventoryMovementType;
import com.ecoamazonas.eco_agua.inventory.InventoryService;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import com.ecoamazonas.eco_agua.supplier.Supplier;
import com.ecoamazonas.eco_agua.supplier.SupplierRepository;
import com.ecoamazonas.eco_agua.supply.Supply;
import com.ecoamazonas.eco_agua.supply.SupplyRepository;
import com.ecoamazonas.eco_agua.user.Employee;
import com.ecoamazonas.eco_agua.user.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

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
    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryService inventoryService;
    private final AccountingAutoJournalEntryService accountingAutoJournalEntryService;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            ExpensePaymentRepository paymentRepository,
            CategoryRepository categoryRepository,
            SupplierRepository supplierRepository,
            SupplyRepository supplyRepository,
            EmployeeRepository employeeRepository,
            ProductRepository productRepository,
            InventoryMovementRepository movementRepository,
            InventoryService inventoryService,
            AccountingAutoJournalEntryService accountingAutoJournalEntryService
    ) {
        this.expenseRepository = expenseRepository;
        this.paymentRepository = paymentRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.supplyRepository = supplyRepository;
        this.employeeRepository = employeeRepository;
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.inventoryService = inventoryService;
        this.accountingAutoJournalEntryService = accountingAutoJournalEntryService;
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

    private boolean isExpenseCategory(Category category) {
        return category != null && category.getType() != null && category.getType().isExpenseType();
    }

    private boolean isFixedStructuralCategory(Category category) {
        return isExpenseCategory(category)
                && category.getCostBehavior() == CostBehavior.FIXED_STRUCTURAL
                && category.isIncludeInBreakEven();
    }

    private boolean isOperationalVariableCategory(Category category) {
        return isExpenseCategory(category)
                && category.getCostBehavior() == CostBehavior.VARIABLE_OPERATIONAL
                && category.isIncludeInOperationalReading();
    }

    private boolean isOperationalVariablePersonnelCategory(Category category) {
        return isOperationalVariableCategory(category)
                && category.getPersonnelMode() != null
                && category.getPersonnelMode().isPersonnelCategory();
    }

    private boolean isOperationalVariableNonPersonnelCategory(Category category) {
        return isOperationalVariableCategory(category)
                && (category.getPersonnelMode() == null || !category.getPersonnelMode().isPersonnelCategory());
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private ExpenseInputContext resolveExpenseInputContext(Category category) {
        if (category != null && category.getPersonnelMode() != null && category.getPersonnelMode().isPersonnelCategory()) {
            return ExpenseInputContext.PERSONNEL;
        }

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
        return registerSimpleExpense(
                expenseDate,
                categoryId,
                supplierId,
                manualSupplierName,
                supplyId,
                employeeId,
                employeePaymentType,
                observation,
                voucherNumber,
                amount,
                false,
                null,
                null
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
            BigDecimal amount,
            boolean updateProductStock,
            Long stockProductId,
            BigDecimal stockQuantity
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

        Product stockProduct = null;
        BigDecimal normalizedStockQuantity = normalizeStockQuantity(updateProductStock, stockProductId, stockQuantity);
        if (updateProductStock) {
            stockProduct = productRepository.findById(stockProductId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found."));

            if (!stockProduct.isActive()) {
                throw new IllegalArgumentException("Product must be active to update stock.");
            }
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

        Expense savedExpense = expenseRepository.save(expense);

        if (updateProductStock) {
            registerProductStockFromExpense(savedExpense, stockProduct, normalizedStockQuantity);
        }

        accountingAutoJournalEntryService.generateForExpense(savedExpense, updateProductStock);

        return savedExpense;
    }

    private BigDecimal normalizeStockQuantity(boolean updateProductStock, Long stockProductId, BigDecimal stockQuantity) {
        if (!updateProductStock) {
            return BigDecimal.ZERO;
        }

        if (stockProductId == null) {
            throw new IllegalArgumentException("Product is required when stock update is enabled.");
        }

        if (stockQuantity == null || stockQuantity.signum() <= 0) {
            throw new IllegalArgumentException("Stock quantity must be greater than zero.");
        }

        return stockQuantity;
    }

    private void registerProductStockFromExpense(Expense expense, Product product, BigDecimal stockQuantity) {
        String observation = buildProductStockObservation(expense, product);

        inventoryService.registerProductMovement(
                product.getId(),
                stockQuantity,
                BigDecimal.ZERO,
                InventoryMovementType.PURCHASE,
                "PURCHASE",
                expense.getId(),
                observation,
                expense.getExpenseDate()
        );
    }

    private String buildProductStockObservation(Expense expense, Product product) {
        List<String> parts = new ArrayList<>();
        parts.add("Compra de mercadería registrada desde egreso #" + expense.getId());

        if (product != null && trimToNull(product.getName()) != null) {
            parts.add("Producto: " + product.getName());
        }
        if (expense.getSupplier() != null && trimToNull(expense.getSupplier().getName()) != null) {
            parts.add("Proveedor: " + expense.getSupplier().getName());
        }
        if (trimToNull(expense.getVoucherNumber()) != null) {
            parts.add("Comprobante: " + expense.getVoucherNumber());
        }
        if (trimToNull(expense.getObservation()) != null) {
            parts.add(expense.getObservation());
        }

        String result = String.join(". ", parts);
        return result.length() > 255 ? result.substring(0, 255) : result;
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

        if (expense.getId() != null && movementRepository.existsByReferenceModuleAndReferenceId("PURCHASE", expense.getId())) {
            throw new IllegalArgumentException("Expenses linked to product stock cannot be edited or deleted from this screen.");
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

        Expense savedExpense = expenseRepository.save(expense);
        accountingAutoJournalEntryService.regenerateForExpense(savedExpense, false);
        return savedExpense;
    }

    @Transactional
    public void deleteSimpleExpense(Long expenseId) {
        if (expenseId == null) {
            throw new IllegalArgumentException("Expense id is required.");
        }

        Expense expense = findById(expenseId);
        ensureExpenseCanBeModified(expense);
        accountingAutoJournalEntryService.cancelDraftEntries(AccountingAutomationEvent.EXPENSE_PAID, expense.getId());
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

        Expense savedExpense = expenseRepository.save(expense);
        accountingAutoJournalEntryService.regenerateForExpense(savedExpense, false);
        return savedExpense;
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
        ExpensePayment savedPayment = paymentRepository.save(payment);
        accountingAutoJournalEntryService.generateForSupplierPayment(savedPayment);
        return savedPayment;
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
        List<ExpenseStatus> statuses = List.of(ExpenseStatus.OPEN, ExpenseStatus.PARTIAL);
        return expenseRepository.findOpenDebtsDetailedByPeriod(start, end, statuses);
    }

    public AccountsPayableSummary buildAccountsPayableSummary(List<Expense> debts, LocalDate today) {
        AccountsPayableSummary summary = new AccountsPayableSummary();
        Set<Long> supplierIds = new java.util.HashSet<>();
        boolean hasUnknownSupplier = false;

        for (Expense debt : debts) {
            BigDecimal pendingAmount = normalizeBalance(debt);
            BigDecimal paidAmount = normalizeAmount(debt.getPaidAmount());

            summary.setTotalPendingAmount(summary.getTotalPendingAmount().add(pendingAmount));
            summary.setPartialPaidAmount(summary.getPartialPaidAmount().add(paidAmount));
            summary.setOpenDebtCount(summary.getOpenDebtCount() + 1);

            if (debt.getSupplier() != null && debt.getSupplier().getId() != null) {
                supplierIds.add(debt.getSupplier().getId());
            } else {
                hasUnknownSupplier = true;
            }

            if (isOverdue(debt.getDueDate(), today)) {
                summary.setOverdueDebtCount(summary.getOverdueDebtCount() + 1);
                summary.setOverduePendingAmount(summary.getOverduePendingAmount().add(pendingAmount));
            }

            if (isDueToday(debt.getDueDate(), today)) {
                summary.setDueTodayDebtCount(summary.getDueTodayDebtCount() + 1);
                summary.setDueTodayPendingAmount(summary.getDueTodayPendingAmount().add(pendingAmount));
            }

            if (debt.getStatus() == ExpenseStatus.PARTIAL || paidAmount.signum() > 0) {
                summary.setPartialDebtCount(summary.getPartialDebtCount() + 1);
            }
        }

        summary.setSupplierCount(supplierIds.size() + (hasUnknownSupplier ? 1 : 0));
        return summary;
    }

    public List<AccountsPayableSupplierSummary> buildSupplierDebtSummary(List<Expense> debts, LocalDate today) {
        Map<String, AccountsPayableSupplierSummary> grouped = new LinkedHashMap<>();

        for (Expense debt : debts) {
            String key = debt.getSupplier() != null && debt.getSupplier().getId() != null
                    ? "SUPPLIER-" + debt.getSupplier().getId()
                    : "NO-SUPPLIER";

            AccountsPayableSupplierSummary summary = grouped.computeIfAbsent(key, ignored -> buildInitialSupplierSummary(debt));
            BigDecimal pendingAmount = normalizeBalance(debt);
            BigDecimal paidAmount = normalizeAmount(debt.getPaidAmount());

            summary.setDebtCount(summary.getDebtCount() + 1);
            summary.setPendingAmount(summary.getPendingAmount().add(pendingAmount));
            summary.setPaidAmount(summary.getPaidAmount().add(paidAmount));

            LocalDate dueDate = debt.getDueDate();
            if (dueDate != null && (summary.getNearestDueDate() == null || dueDate.isBefore(summary.getNearestDueDate()))) {
                summary.setNearestDueDate(dueDate);
            }

            if (isOverdue(dueDate, today)) {
                summary.setOverdueDebtCount(summary.getOverdueDebtCount() + 1);
            }
        }

        List<AccountsPayableSupplierSummary> summaries = new ArrayList<>(grouped.values());
        for (AccountsPayableSupplierSummary summary : summaries) {
            summary.setPriorityLabel(resolvePriorityLabel(summary.getNearestDueDate(), today, summary.getOverdueDebtCount() > 0));
            summary.setPriorityBadgeClass(resolvePriorityBadgeClass(summary.getNearestDueDate(), today, summary.getOverdueDebtCount() > 0));
            summary.setWhatsappUrl(buildSupplierWhatsappUrl(summary.getPhone(), summary.getSupplierName(), summary.getPendingAmount()));
        }

        summaries.sort(Comparator
                .comparing(AccountsPayableSupplierSummary::getOverdueDebtCount).reversed()
                .thenComparing(AccountsPayableSupplierSummary::getPendingAmount, Comparator.reverseOrder())
                .thenComparing(AccountsPayableSupplierSummary::getSupplierName, Comparator.nullsLast(String::compareToIgnoreCase)));

        return summaries;
    }

    public List<AccountsPayableRow> buildAccountsPayableRows(List<Expense> debts, LocalDate today) {
        return debts.stream()
                .map(debt -> buildAccountsPayableRow(debt, today))
                .sorted(Comparator
                        .comparing(AccountsPayableRow::getDueDate, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(AccountsPayableRow::getExpenseDate, Comparator.nullsLast(LocalDate::compareTo))
                        .thenComparing(AccountsPayableRow::getExpenseId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private AccountsPayableSupplierSummary buildInitialSupplierSummary(Expense debt) {
        AccountsPayableSupplierSummary summary = new AccountsPayableSupplierSummary();
        Supplier supplier = debt.getSupplier();

        if (supplier != null) {
            summary.setSupplierId(supplier.getId());
            summary.setSupplierName(trimToNull(supplier.getName()) != null ? supplier.getName() : "Proveedor sin nombre");
            summary.setPhone(resolveSupplierPhone(supplier));
        } else {
            summary.setSupplierName("Sin proveedor asignado");
        }

        return summary;
    }

    private AccountsPayableRow buildAccountsPayableRow(Expense debt, LocalDate today) {
        AccountsPayableRow row = new AccountsPayableRow();
        Supplier supplier = debt.getSupplier();

        row.setExpenseId(debt.getId());
        row.setSupplierId(supplier != null ? supplier.getId() : null);
        row.setSupplierName(supplier != null && trimToNull(supplier.getName()) != null ? supplier.getName() : "Sin proveedor asignado");
        row.setPhone(supplier != null ? resolveSupplierPhone(supplier) : null);
        row.setCategoryName(debt.getCategory() != null ? debt.getCategory().getName() : "-");
        row.setDescription(trimToNull(debt.getObservation()) != null ? debt.getObservation() : "Sin observación");
        row.setVoucherNumber(trimToNull(debt.getVoucherNumber()) != null ? debt.getVoucherNumber() : "-");
        row.setExpenseDate(debt.getExpenseDate());
        row.setDueDate(debt.getDueDate());
        row.setStatusLabel(resolveStatusLabel(debt.getStatus()));
        row.setPriorityLabel(resolvePriorityLabel(debt.getDueDate(), today, false));
        row.setPriorityBadgeClass(resolvePriorityBadgeClass(debt.getDueDate(), today, false));
        row.setTotalAmount(normalizeAmount(debt.getAmount()));
        row.setPaidAmount(normalizeAmount(debt.getPaidAmount()));
        row.setPendingAmount(normalizeBalance(debt));
        row.setWhatsappUrl(buildSupplierWhatsappUrl(row.getPhone(), row.getSupplierName(), row.getPendingAmount()));

        return row;
    }

    private BigDecimal normalizeBalance(Expense expense) {
        if (expense == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal balance = expense.getBalance();
        if (balance == null || balance.signum() < 0) {
            return BigDecimal.ZERO;
        }

        return balance;
    }

    private boolean isOverdue(LocalDate dueDate, LocalDate today) {
        return dueDate != null && today != null && dueDate.isBefore(today);
    }

    private boolean isDueToday(LocalDate dueDate, LocalDate today) {
        return dueDate != null && today != null && dueDate.isEqual(today);
    }

    private String resolvePriorityLabel(LocalDate dueDate, LocalDate today, boolean hasOverdueDebts) {
        if (hasOverdueDebts || isOverdue(dueDate, today)) {
            return "Vencido";
        }
        if (isDueToday(dueDate, today)) {
            return "Vence hoy";
        }
        if (dueDate == null) {
            return "Sin fecha";
        }
        if (today != null && !dueDate.isAfter(today.plusDays(3))) {
            return "Próximo";
        }
        return "Pendiente";
    }

    private String resolvePriorityBadgeClass(LocalDate dueDate, LocalDate today, boolean hasOverdueDebts) {
        if (hasOverdueDebts || isOverdue(dueDate, today)) {
            return "bg-danger";
        }
        if (isDueToday(dueDate, today)) {
            return "bg-warning text-dark";
        }
        if (dueDate == null) {
            return "bg-secondary";
        }
        if (today != null && !dueDate.isAfter(today.plusDays(3))) {
            return "bg-info text-dark";
        }
        return "bg-success";
    }

    private String resolveStatusLabel(ExpenseStatus status) {
        if (status == null) {
            return "Pendiente";
        }

        return switch (status) {
            case OPEN -> "Pendiente";
            case PARTIAL -> "Pago parcial";
            case PAID -> "Pagado";
            case CANCELED -> "Cancelado";
        };
    }

    private String resolveSupplierPhone(Supplier supplier) {
        if (supplier == null) {
            return null;
        }

        String contactPhone = trimToNull(supplier.getContactPhone());
        if (contactPhone != null) {
            return contactPhone;
        }

        return trimToNull(supplier.getPhone());
    }

    private String buildSupplierWhatsappUrl(String phone, String supplierName, BigDecimal pendingAmount) {
        String normalizedPhone = normalizePhoneForWhatsapp(phone);
        if (normalizedPhone == null) {
            return null;
        }

        String cleanSupplierName = trimToNull(supplierName) != null ? supplierName : "proveedor";
        String amountText = normalizeAmount(pendingAmount).toPlainString();
        String message = "Hola, queremos coordinar el pago pendiente de S/ "
                + amountText
                + " registrado para "
                + cleanSupplierName
                + ".";

        return "https://wa.me/" + normalizedPhone + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    private String normalizePhoneForWhatsapp(String phone) {
        String value = trimToNull(phone);
        if (value == null) {
            return null;
        }

        String digits = value.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() == 9) {
            return "51" + digits;
        }

        if (digits.length() >= 10) {
            return digits;
        }

        return null;
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
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
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
        List<Expense> expenses = findNormalizedRangeExpenses(start, end);

        return expenses.stream()
                .filter(expense -> isFixedStructuralCategory(expense.getCategory()))
                .map(expense -> normalizeAmount(expense.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthlyFixedCosts(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return getTotalFixedCosts(start, end);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getFixedCostsByCategory(LocalDate start, LocalDate end) {
        List<Expense> expenses = findNormalizedRangeExpenses(start, end);

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            Category category = expense.getCategory();
            if (!isFixedStructuralCategory(category)) {
                continue;
            }

            String key = category.getName();
            result.merge(key, normalizeAmount(expense.getAmount()), BigDecimal::add);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public BigDecimal getOperationalVariableTotal(LocalDate start, LocalDate end) {
        return getOperationalVariableByCategory(start, end).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal getOperationalVariablePersonnelTotal(LocalDate start, LocalDate end) {
        List<Expense> expenses = findNormalizedRangeExpenses(start, end);

        return expenses.stream()
                .filter(expense -> isOperationalVariablePersonnelCategory(expense.getCategory()))
                .map(expense -> normalizeAmount(expense.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal getOperationalVariableNonPersonnelTotal(LocalDate start, LocalDate end) {
        List<Expense> expenses = findNormalizedRangeExpenses(start, end);

        return expenses.stream()
                .filter(expense -> isOperationalVariableNonPersonnelCategory(expense.getCategory()))
                .map(expense -> normalizeAmount(expense.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getOperationalVariableByCategory(LocalDate start, LocalDate end) {
        List<Expense> expenses = findNormalizedRangeExpenses(start, end);

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            Category category = expense.getCategory();
            if (!isOperationalVariableCategory(category)) {
                continue;
            }

            String key = category.getName();
            result.merge(key, normalizeAmount(expense.getAmount()), BigDecimal::add);
        }

        return result;
    }

    private List<Expense> findNormalizedRangeExpenses(LocalDate start, LocalDate end) {
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

        return findByDateRange(from, to);
    }

}