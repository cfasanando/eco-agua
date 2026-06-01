package com.ecoamazonas.eco_agua.supplier;

import java.math.BigDecimal;

public class SupplierPurchaseHistorySummary {

    private final int purchaseCount;
    private final int stockLinkedPurchaseCount;
    private final int productCount;
    private final BigDecimal totalAmount;
    private final BigDecimal totalPaidAmount;
    private final BigDecimal pendingAmount;
    private final BigDecimal totalStockQuantity;

    public SupplierPurchaseHistorySummary(
            int purchaseCount,
            int stockLinkedPurchaseCount,
            int productCount,
            BigDecimal totalAmount,
            BigDecimal totalPaidAmount,
            BigDecimal pendingAmount,
            BigDecimal totalStockQuantity
    ) {
        this.purchaseCount = purchaseCount;
        this.stockLinkedPurchaseCount = stockLinkedPurchaseCount;
        this.productCount = productCount;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.totalPaidAmount = totalPaidAmount != null ? totalPaidAmount : BigDecimal.ZERO;
        this.pendingAmount = pendingAmount != null ? pendingAmount : BigDecimal.ZERO;
        this.totalStockQuantity = totalStockQuantity != null ? totalStockQuantity : BigDecimal.ZERO;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public int getStockLinkedPurchaseCount() {
        return stockLinkedPurchaseCount;
    }

    public int getProductCount() {
        return productCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public BigDecimal getTotalStockQuantity() {
        return totalStockQuantity;
    }
}
