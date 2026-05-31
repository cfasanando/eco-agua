package com.ecoamazonas.eco_agua.dashboard;

import java.math.BigDecimal;

public class BusinessOverviewStockRow {

    private final Long productId;
    private final String productName;
    private final BigDecimal stock;
    private final String stockLabel;
    private final String badgeClass;

    public BusinessOverviewStockRow(
            Long productId,
            String productName,
            BigDecimal stock,
            String stockLabel,
            String badgeClass
    ) {
        this.productId = productId;
        this.productName = productName;
        this.stock = stock;
        this.stockLabel = stockLabel;
        this.badgeClass = badgeClass;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getStock() {
        return stock;
    }

    public String getStockLabel() {
        return stockLabel;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
