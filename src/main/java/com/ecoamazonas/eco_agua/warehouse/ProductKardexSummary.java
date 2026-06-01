package com.ecoamazonas.eco_agua.warehouse;

import java.math.BigDecimal;

public class ProductKardexSummary {

    private final BigDecimal openingBalance;
    private final BigDecimal totalIn;
    private final BigDecimal totalOut;
    private final BigDecimal endingBalance;
    private final BigDecimal currentStock;
    private final int movementCount;

    public ProductKardexSummary(
            BigDecimal openingBalance,
            BigDecimal totalIn,
            BigDecimal totalOut,
            BigDecimal endingBalance,
            BigDecimal currentStock,
            int movementCount
    ) {
        this.openingBalance = openingBalance != null ? openingBalance : BigDecimal.ZERO;
        this.totalIn = totalIn != null ? totalIn : BigDecimal.ZERO;
        this.totalOut = totalOut != null ? totalOut : BigDecimal.ZERO;
        this.endingBalance = endingBalance != null ? endingBalance : BigDecimal.ZERO;
        this.currentStock = currentStock != null ? currentStock : BigDecimal.ZERO;
        this.movementCount = movementCount;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getTotalIn() {
        return totalIn;
    }

    public BigDecimal getTotalOut() {
        return totalOut;
    }

    public BigDecimal getEndingBalance() {
        return endingBalance;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public int getMovementCount() {
        return movementCount;
    }

    public BigDecimal getStockDifference() {
        return currentStock.subtract(endingBalance);
    }

    public boolean isBalancedWithCurrentStock() {
        return getStockDifference().compareTo(BigDecimal.ZERO) == 0;
    }
}
