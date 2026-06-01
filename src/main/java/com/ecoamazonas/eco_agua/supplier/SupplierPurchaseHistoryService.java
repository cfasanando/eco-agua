package com.ecoamazonas.eco_agua.supplier;

import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseRepository;
import com.ecoamazonas.eco_agua.inventory.InventoryMovement;
import com.ecoamazonas.eco_agua.inventory.InventoryMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SupplierPurchaseHistoryService {

    private final ExpenseRepository expenseRepository;
    private final InventoryMovementRepository movementRepository;
    private final SupplierRepository supplierRepository;

    public SupplierPurchaseHistoryService(
            ExpenseRepository expenseRepository,
            InventoryMovementRepository movementRepository,
            SupplierRepository supplierRepository
    ) {
        this.expenseRepository = expenseRepository;
        this.movementRepository = movementRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierListRow> buildSupplierRows(List<Supplier> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            return List.of();
        }

        List<Long> supplierIds = suppliers.stream()
                .map(Supplier::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, SupplierPurchaseMetrics> metricsBySupplier = buildMetricsBySupplierId(supplierIds);

        return suppliers.stream()
                .map(supplier -> new SupplierListRow(
                        supplier,
                        metricsBySupplier.getOrDefault(
                                supplier.getId(),
                                SupplierPurchaseMetrics.empty(supplier.getId())
                        )
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierPurchaseHistorySnapshot buildSupplierHistory(
            Long supplierId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        DateRange range = normalizeRange(startDate, endDate);
        List<Expense> expenses = expenseRepository.findSupplierExpensesBySupplierAndPeriod(
                supplierId,
                range.start(),
                range.end()
        );

        Map<Long, List<InventoryMovement>> movementsByExpenseId = loadMovementsByExpenseId(
                expenses.stream().map(Expense::getId).filter(Objects::nonNull).toList()
        );

        List<SupplierPurchaseHistoryRow> rows = expenses.stream()
                .map(expense -> new SupplierPurchaseHistoryRow(
                        expense,
                        movementsByExpenseId.getOrDefault(expense.getId(), List.of())
                ))
                .toList();

        return new SupplierPurchaseHistorySnapshot(buildSummary(rows), rows);
    }

    @Transactional(readOnly = true)
    public Supplier findSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with id " + supplierId));
    }

    public DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate start = startDate != null ? startDate : today.withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : today;

        if (end.isBefore(start)) {
            return new DateRange(end, start);
        }

        return new DateRange(start, end);
    }

    private Map<Long, SupplierPurchaseMetrics> buildMetricsBySupplierId(List<Long> supplierIds) {
        if (supplierIds == null || supplierIds.isEmpty()) {
            return Map.of();
        }

        List<Expense> expenses = expenseRepository.findSupplierExpensesBySupplierIds(supplierIds);
        Map<Long, List<InventoryMovement>> movementsByExpenseId = loadMovementsByExpenseId(
                expenses.stream().map(Expense::getId).filter(Objects::nonNull).toList()
        );

        Map<Long, SupplierAccumulator> accumulators = new LinkedHashMap<>();
        for (Long supplierId : supplierIds) {
            accumulators.put(supplierId, new SupplierAccumulator(supplierId));
        }

        for (Expense expense : expenses) {
            if (expense.getSupplier() == null || expense.getSupplier().getId() == null) {
                continue;
            }

            SupplierAccumulator accumulator = accumulators.computeIfAbsent(
                    expense.getSupplier().getId(),
                    SupplierAccumulator::new
            );
            List<InventoryMovement> movements = movementsByExpenseId.getOrDefault(expense.getId(), List.of());
            accumulator.addExpense(expense, movements);
        }

        return accumulators.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toMetrics()
                ));
    }

    private Map<Long, List<InventoryMovement>> loadMovementsByExpenseId(List<Long> expenseIds) {
        if (expenseIds == null || expenseIds.isEmpty()) {
            return Map.of();
        }

        List<InventoryMovement> movements = movementRepository.findProductPurchaseMovementsByExpenseIds(expenseIds);
        Map<Long, List<InventoryMovement>> movementsByExpenseId = new HashMap<>();

        for (InventoryMovement movement : movements) {
            if (movement.getReferenceId() == null) {
                continue;
            }
            movementsByExpenseId.computeIfAbsent(movement.getReferenceId(), key -> new ArrayList<>()).add(movement);
        }

        return movementsByExpenseId;
    }

    private SupplierPurchaseHistorySummary buildSummary(List<SupplierPurchaseHistoryRow> rows) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPaidAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        BigDecimal totalStockQuantity = BigDecimal.ZERO;
        Set<String> productNames = new HashSet<>();
        int stockLinkedPurchaseCount = 0;

        for (SupplierPurchaseHistoryRow row : rows) {
            totalAmount = totalAmount.add(row.getAmount());
            totalPaidAmount = totalPaidAmount.add(row.getPaidAmount());
            pendingAmount = pendingAmount.add(row.getBalance());
            totalStockQuantity = totalStockQuantity.add(row.getStockQuantity());

            if (row.isLinkedToStock()) {
                stockLinkedPurchaseCount++;
                if (!"Sin entrada de stock".equals(row.getProductSummary())) {
                    productNames.add(row.getProductSummary());
                }
            }
        }

        return new SupplierPurchaseHistorySummary(
                rows.size(),
                stockLinkedPurchaseCount,
                productNames.size(),
                totalAmount,
                totalPaidAmount,
                pendingAmount,
                totalStockQuantity
        );
    }

    public record DateRange(LocalDate start, LocalDate end) {
    }

    private static class SupplierAccumulator {

        private final Long supplierId;
        private int purchaseCount;
        private int stockPurchaseCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private LocalDate lastPurchaseDate;
        private final Set<String> productNames = new HashSet<>();

        private SupplierAccumulator(Long supplierId) {
            this.supplierId = supplierId;
        }

        private void addExpense(Expense expense, List<InventoryMovement> movements) {
            purchaseCount++;
            if (expense.getAmount() != null) {
                totalAmount = totalAmount.add(expense.getAmount());
            }
            if (expense.getExpenseDate() != null
                    && (lastPurchaseDate == null || expense.getExpenseDate().isAfter(lastPurchaseDate))) {
                lastPurchaseDate = expense.getExpenseDate();
            }

            if (movements != null && !movements.isEmpty()) {
                stockPurchaseCount++;
                for (InventoryMovement movement : movements) {
                    if (movement.getProduct() != null && movement.getProduct().getName() != null) {
                        productNames.add(movement.getProduct().getName());
                    }
                }
            }
        }

        private SupplierPurchaseMetrics toMetrics() {
            String productSummary = productNames.stream()
                    .sorted()
                    .limit(3)
                    .collect(Collectors.joining(", "));

            if (productNames.size() > 3) {
                productSummary = productSummary + " y " + (productNames.size() - 3) + " más";
            }

            return new SupplierPurchaseMetrics(
                    supplierId,
                    purchaseCount,
                    stockPurchaseCount,
                    productNames.size(),
                    totalAmount,
                    lastPurchaseDate,
                    productSummary
            );
        }
    }
}
