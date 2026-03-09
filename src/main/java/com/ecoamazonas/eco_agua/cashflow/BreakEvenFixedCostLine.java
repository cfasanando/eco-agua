package com.ecoamazonas.eco_agua.cashflow;

import java.math.BigDecimal;

public class BreakEvenFixedCostLine {

    private final String categoryName;
    private final BigDecimal totalAmount;

    public BreakEvenFixedCostLine(String categoryName, BigDecimal totalAmount) {
        this.categoryName = categoryName;
        this.totalAmount = totalAmount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
