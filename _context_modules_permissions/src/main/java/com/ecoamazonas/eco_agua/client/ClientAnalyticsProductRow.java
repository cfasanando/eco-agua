package com.ecoamazonas.eco_agua.client;

import java.math.BigDecimal;

public class ClientAnalyticsProductRow {

    private final String productName;
    private final BigDecimal quantity;
    private final BigDecimal revenue;
    private final BigDecimal estimatedCost;
    private final BigDecimal estimatedProfit;
    private final BigDecimal averageUnitPrice;

    public ClientAnalyticsProductRow(
            String productName,
            BigDecimal quantity,
            BigDecimal revenue,
            BigDecimal estimatedCost,
            BigDecimal estimatedProfit,
            BigDecimal averageUnitPrice
    ) {
        this.productName = productName;
        this.quantity = quantity;
        this.revenue = revenue;
        this.estimatedCost = estimatedCost;
        this.estimatedProfit = estimatedProfit;
        this.averageUnitPrice = averageUnitPrice;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public BigDecimal getEstimatedProfit() {
        return estimatedProfit;
    }

    public BigDecimal getAverageUnitPrice() {
        return averageUnitPrice;
    }
}
