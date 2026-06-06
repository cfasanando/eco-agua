package com.ecoamazonas.eco_agua.product.cost;

import java.math.BigDecimal;

public class ProductProfitabilityRow {

    private final Long productId;
    private final String productName;
    private final BigDecimal salePrice;
    private final BigDecimal unitCost;
    private final BigDecimal unitMargin;
    private final BigDecimal marginPercent;
    private final BigDecimal quantitySold;
    private final BigDecimal revenue;
    private final BigDecimal totalCost;
    private final BigDecimal grossProfit;
    private final boolean hasRecipeCost;
    private final boolean lowMargin;
    private final boolean loss;

    public ProductProfitabilityRow(
            Long productId,
            String productName,
            BigDecimal salePrice,
            BigDecimal unitCost,
            BigDecimal unitMargin,
            BigDecimal marginPercent,
            BigDecimal quantitySold,
            BigDecimal revenue,
            BigDecimal totalCost,
            BigDecimal grossProfit,
            boolean hasRecipeCost,
            boolean lowMargin,
            boolean loss
    ) {
        this.productId = productId;
        this.productName = productName;
        this.salePrice = salePrice;
        this.unitCost = unitCost;
        this.unitMargin = unitMargin;
        this.marginPercent = marginPercent;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
        this.totalCost = totalCost;
        this.grossProfit = grossProfit;
        this.hasRecipeCost = hasRecipeCost;
        this.lowMargin = lowMargin;
        this.loss = loss;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getUnitMargin() {
        return unitMargin;
    }

    public BigDecimal getMarginPercent() {
        return marginPercent;
    }

    public BigDecimal getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public boolean isHasRecipeCost() {
        return hasRecipeCost;
    }

    public boolean isLowMargin() {
        return lowMargin;
    }

    public boolean isLoss() {
        return loss;
    }
}
