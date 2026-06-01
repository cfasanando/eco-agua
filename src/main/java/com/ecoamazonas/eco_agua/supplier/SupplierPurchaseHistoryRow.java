package com.ecoamazonas.eco_agua.supplier;

import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseStatus;
import com.ecoamazonas.eco_agua.inventory.InventoryMovement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SupplierPurchaseHistoryRow {

    private final Expense expense;
    private final List<InventoryMovement> movements;

    public SupplierPurchaseHistoryRow(Expense expense, List<InventoryMovement> movements) {
        this.expense = expense;
        this.movements = movements != null ? movements : List.of();
    }

    public Long getExpenseId() {
        return expense != null ? expense.getId() : null;
    }

    public LocalDate getPurchaseDate() {
        return expense != null ? expense.getExpenseDate() : null;
    }

    public String getCategoryName() {
        if (expense == null || expense.getCategory() == null) {
            return "Sin categoría";
        }
        return expense.getCategory().getName();
    }

    public BigDecimal getAmount() {
        return expense != null && expense.getAmount() != null ? expense.getAmount() : BigDecimal.ZERO;
    }

    public BigDecimal getPaidAmount() {
        return expense != null && expense.getPaidAmount() != null ? expense.getPaidAmount() : BigDecimal.ZERO;
    }

    public BigDecimal getBalance() {
        return expense != null ? expense.getBalance() : BigDecimal.ZERO;
    }

    public String getPaymentStatusLabel() {
        if (expense == null || expense.getStatus() == null) {
            return "Sin estado";
        }
        if (ExpenseStatus.PAID.equals(expense.getStatus())) {
            return "Pagado";
        }
        if (ExpenseStatus.PARTIAL.equals(expense.getStatus())) {
            return "Pago parcial";
        }
        if (ExpenseStatus.CANCELED.equals(expense.getStatus())) {
            return "Anulado";
        }
        return expense.isDebt() ? "Pendiente" : "Abierto";
    }

    public String getPaymentStatusCssClass() {
        if (expense == null || expense.getStatus() == null) {
            return "bg-secondary-subtle text-secondary";
        }
        if (ExpenseStatus.PAID.equals(expense.getStatus())) {
            return "bg-success-subtle text-success";
        }
        if (ExpenseStatus.PARTIAL.equals(expense.getStatus())) {
            return "bg-warning-subtle text-warning";
        }
        if (ExpenseStatus.CANCELED.equals(expense.getStatus())) {
            return "bg-secondary-subtle text-secondary";
        }
        return expense.isDebt() ? "bg-danger-subtle text-danger" : "bg-info-subtle text-info";
    }

    public String getVoucherNumber() {
        if (expense == null || expense.getVoucherNumber() == null || expense.getVoucherNumber().isBlank()) {
            return "-";
        }
        return expense.getVoucherNumber();
    }

    public String getDocumentLabel() {
        if (expense == null || expense.getDocType() == null || expense.getDocType().isBlank()) {
            return "-";
        }
        String series = expense.getDocSeries() != null && !expense.getDocSeries().isBlank() ? expense.getDocSeries() : "";
        String number = expense.getDocNumber() != null && !expense.getDocNumber().isBlank() ? expense.getDocNumber() : "";
        String suffix = (series + " " + number).trim();
        return suffix.isBlank() ? expense.getDocType() : expense.getDocType() + " " + suffix;
    }

    public String getObservation() {
        if (expense == null || expense.getObservation() == null || expense.getObservation().isBlank()) {
            return "-";
        }
        return expense.getObservation();
    }

    public String getProductSummary() {
        if (movements.isEmpty()) {
            return "Sin entrada de stock";
        }

        return movements.stream()
                .map(InventoryMovement::getProduct)
                .filter(Objects::nonNull)
                .map(product -> product.getName() != null ? product.getName() : "Producto sin nombre")
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public BigDecimal getStockQuantity() {
        return movements.stream()
                .map(InventoryMovement::getQuantityIn)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isLinkedToStock() {
        return !movements.isEmpty();
    }

    public Long getFirstProductId() {
        return movements.stream()
                .map(InventoryMovement::getProduct)
                .filter(Objects::nonNull)
                .map(product -> product.getId())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
