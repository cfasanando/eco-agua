package com.ecoamazonas.eco_agua.warehouse;

import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.inventory.InventoryMovement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PurchaseHistoryRow {

    private final InventoryMovement movement;
    private final Expense expense;

    public PurchaseHistoryRow(InventoryMovement movement, Expense expense) {
        this.movement = movement;
        this.expense = expense;
    }

    public Long getMovementId() {
        return movement != null ? movement.getId() : null;
    }

    public Long getExpenseId() {
        return expense != null ? expense.getId() : null;
    }

    public Long getProductId() {
        return movement != null && movement.getProduct() != null ? movement.getProduct().getId() : null;
    }

    public String getProductName() {
        return movement != null && movement.getProduct() != null ? movement.getProduct().getName() : "-";
    }

    public String getCategoryName() {
        if (movement == null || movement.getProduct() == null || movement.getProduct().getCategory() == null) {
            return "Sin categoría";
        }

        return movement.getProduct().getCategory().getName();
    }

    public String getSupplierName() {
        if (expense == null || expense.getSupplier() == null) {
            return "Sin proveedor";
        }

        return expense.getSupplier().getName();
    }

    public Long getSupplierId() {
        return expense != null && expense.getSupplier() != null ? expense.getSupplier().getId() : null;
    }

    public LocalDate getPurchaseDate() {
        if (expense != null && expense.getExpenseDate() != null) {
            return expense.getExpenseDate();
        }

        LocalDateTime movementDate = getMovementDate();
        return movementDate != null ? movementDate.toLocalDate() : null;
    }

    public LocalDateTime getMovementDate() {
        return movement != null ? movement.getMovementDate() : null;
    }

    public BigDecimal getQuantity() {
        return movement != null && movement.getQuantityIn() != null ? movement.getQuantityIn() : BigDecimal.ZERO;
    }

    public BigDecimal getAmount() {
        return expense != null && expense.getAmount() != null ? expense.getAmount() : BigDecimal.ZERO;
    }

    public String getVoucherNumber() {
        if (expense == null || expense.getVoucherNumber() == null || expense.getVoucherNumber().isBlank()) {
            return "-";
        }

        return expense.getVoucherNumber();
    }

    public String getObservation() {
        if (expense != null && expense.getObservation() != null && !expense.getObservation().isBlank()) {
            return expense.getObservation();
        }
        if (movement != null && movement.getObservation() != null && !movement.getObservation().isBlank()) {
            return movement.getObservation();
        }

        return "-";
    }

    public boolean isLinkedToExpense() {
        return expense != null;
    }
}
