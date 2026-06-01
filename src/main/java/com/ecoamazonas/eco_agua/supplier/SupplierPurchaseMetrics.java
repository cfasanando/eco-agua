package com.ecoamazonas.eco_agua.supplier;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SupplierPurchaseMetrics {

    private final Long supplierId;
    private final int purchaseCount;
    private final int stockPurchaseCount;
    private final int productCount;
    private final BigDecimal totalAmount;
    private final LocalDate lastPurchaseDate;
    private final String productSummary;

    public SupplierPurchaseMetrics(
            Long supplierId,
            int purchaseCount,
            int stockPurchaseCount,
            int productCount,
            BigDecimal totalAmount,
            LocalDate lastPurchaseDate,
            String productSummary
    ) {
        this.supplierId = supplierId;
        this.purchaseCount = purchaseCount;
        this.stockPurchaseCount = stockPurchaseCount;
        this.productCount = productCount;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.lastPurchaseDate = lastPurchaseDate;
        this.productSummary = productSummary != null && !productSummary.isBlank() ? productSummary : "-";
    }

    public static SupplierPurchaseMetrics empty(Long supplierId) {
        return new SupplierPurchaseMetrics(
                supplierId,
                0,
                0,
                0,
                BigDecimal.ZERO,
                null,
                "-"
        );
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public int getStockPurchaseCount() {
        return stockPurchaseCount;
    }

    public int getProductCount() {
        return productCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDate getLastPurchaseDate() {
        return lastPurchaseDate;
    }

    public String getProductSummary() {
        return productSummary;
    }

    public boolean hasPurchases() {
        return purchaseCount > 0;
    }
}
