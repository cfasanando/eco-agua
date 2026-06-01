package com.ecoamazonas.eco_agua.warehouse;

import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseRepository;
import com.ecoamazonas.eco_agua.inventory.InventoryMovement;
import com.ecoamazonas.eco_agua.inventory.InventoryMovementRepository;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductService;
import com.ecoamazonas.eco_agua.supplier.Supplier;
import com.ecoamazonas.eco_agua.supplier.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PurchaseHistoryService {

    private final InventoryMovementRepository movementRepository;
    private final ExpenseRepository expenseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductService productService;

    public PurchaseHistoryService(
            InventoryMovementRepository movementRepository,
            ExpenseRepository expenseRepository,
            SupplierRepository supplierRepository,
            ProductService productService
    ) {
        this.movementRepository = movementRepository;
        this.expenseRepository = expenseRepository;
        this.supplierRepository = supplierRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public PurchaseHistorySnapshot buildHistory(
            LocalDate startDate,
            LocalDate endDate,
            Long supplierId,
            Long productId
    ) {
        DateRange range = normalizeRange(startDate, endDate);
        LocalDateTime startDateTime = range.start().atStartOfDay();
        LocalDateTime endDateTime = range.end().atTime(LocalTime.MAX);

        List<InventoryMovement> movements = movementRepository.findProductPurchaseMovements(
                startDateTime,
                endDateTime,
                productId
        );

        Map<Long, Expense> expensesById = loadExpensesById(movements);

        List<PurchaseHistoryRow> rows = movements.stream()
                .map(movement -> new PurchaseHistoryRow(movement, resolveExpense(movement, expensesById)))
                .filter(row -> supplierId == null || supplierId.equals(row.getSupplierId()))
                .toList();

        return new PurchaseHistorySnapshot(buildSummary(rows), rows);
    }

    @Transactional(readOnly = true)
    public List<Supplier> findActiveSuppliers() {
        return supplierRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productService.findAllActiveForOrder();
    }

    private Map<Long, Expense> loadExpensesById(List<InventoryMovement> movements) {
        List<Long> expenseIds = movements.stream()
                .filter(movement -> movement != null
                        && "PURCHASE".equalsIgnoreCase(movement.getReferenceModule())
                        && movement.getReferenceId() != null)
                .map(InventoryMovement::getReferenceId)
                .distinct()
                .toList();

        if (expenseIds.isEmpty()) {
            return Map.of();
        }

        return expenseRepository.findAllDetailedByIdIn(expenseIds).stream()
                .collect(Collectors.toMap(Expense::getId, expense -> expense));
    }

    private Expense resolveExpense(InventoryMovement movement, Map<Long, Expense> expensesById) {
        if (movement == null || movement.getReferenceId() == null) {
            return null;
        }

        return expensesById.get(movement.getReferenceId());
    }

    private PurchaseHistorySummary buildSummary(List<PurchaseHistoryRow> rows) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        Set<Long> supplierIds = new HashSet<>();
        Set<Long> productIds = new HashSet<>();
        int linkedExpenseCount = 0;

        for (PurchaseHistoryRow row : rows) {
            totalQuantity = totalQuantity.add(row.getQuantity());
            totalAmount = totalAmount.add(row.getAmount());

            if (row.isLinkedToExpense()) {
                linkedExpenseCount++;
            }
            if (row.getSupplierId() != null) {
                supplierIds.add(row.getSupplierId());
            }
            if (row.getProductId() != null) {
                productIds.add(row.getProductId());
            }
        }

        return new PurchaseHistorySummary(
                rows.size(),
                linkedExpenseCount,
                supplierIds.size(),
                productIds.size(),
                totalQuantity,
                totalAmount
        );
    }

    private DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate start = startDate != null ? startDate : today.withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : today;

        if (end.isBefore(start)) {
            return new DateRange(end, start);
        }

        return new DateRange(start, end);
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
