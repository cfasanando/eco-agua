package com.ecoamazonas.eco_agua.warehouse;

import java.math.BigDecimal;

public class PurchaseHistorySummary {

    private final int movementCount;
    private final int linkedExpenseCount;
    private final int supplierCount;
    private final int productCount;
    private final BigDecimal totalQuantity;
    private final BigDecimal totalAmount;

    public PurchaseHistorySummary(
            int movementCount,
            int linkedExpenseCount,
            int supplierCount,
            int productCount,
            BigDecimal totalQuantity,
            BigDecimal totalAmount
    ) {
        this.movementCount = movementCount;
        this.linkedExpenseCount = linkedExpenseCount;
        this.supplierCount = supplierCount;
        this.productCount = productCount;
        this.totalQuantity = totalQuantity != null ? totalQuantity : BigDecimal.ZERO;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public int getMovementCount() {
        return movementCount;
    }

    public int getLinkedExpenseCount() {
        return linkedExpenseCount;
    }

    public int getSupplierCount() {
        return supplierCount;
    }

    public int getProductCount() {
        return productCount;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
