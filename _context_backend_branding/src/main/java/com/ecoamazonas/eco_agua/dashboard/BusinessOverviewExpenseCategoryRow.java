package com.ecoamazonas.eco_agua.dashboard;

import java.math.BigDecimal;

public class BusinessOverviewExpenseCategoryRow {

    private final String categoryName;
    private final BigDecimal amount;
    private final BigDecimal sharePercent;

    public BusinessOverviewExpenseCategoryRow(String categoryName, BigDecimal amount, BigDecimal sharePercent) {
        this.categoryName = categoryName;
        this.amount = amount;
        this.sharePercent = sharePercent;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getSharePercent() {
        return sharePercent;
    }
}
