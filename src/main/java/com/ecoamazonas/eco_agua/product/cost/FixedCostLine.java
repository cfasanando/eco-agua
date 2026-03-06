package com.ecoamazonas.eco_agua.product.cost;

import java.math.BigDecimal;

public class FixedCostLine {

    private final String categoryName;
    private final BigDecimal amount;

    public FixedCostLine(String categoryName, BigDecimal amount) {
        this.categoryName = categoryName;
        this.amount = amount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
