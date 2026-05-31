package com.ecoamazonas.eco_agua.product.cost;

import java.math.BigDecimal;

public class PeriodExpenseLine {

    private final String categoryName;
    private final BigDecimal totalAmount;
    private final BigDecimal unitCost;

    public PeriodExpenseLine(String categoryName, BigDecimal totalAmount, BigDecimal unitCost) {
        this.categoryName = categoryName;
        this.totalAmount = totalAmount;
        this.unitCost = unitCost;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }
}
