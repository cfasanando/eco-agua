package com.ecoamazonas.eco_agua.product.cost;

import java.math.BigDecimal;

public class ProductPriceSimulatorResult {

    private final Long productId;
    private final String productName;
    private final BigDecimal currentPrice;
    private final BigDecimal unitCost;
    private final BigDecimal simulatedPrice;
    private final BigDecimal estimatedQuantity;
    private final BigDecimal currentUnitMargin;
    private final BigDecimal currentMarginPercent;
    private final BigDecimal simulatedUnitMargin;
    private final BigDecimal simulatedMarginPercent;
    private final BigDecimal currentRevenue;
    private final BigDecimal simulatedRevenue;
    private final BigDecimal priceDifference;
    private final BigDecimal totalVariableCost;
    private final BigDecimal currentGrossProfit;
    private final BigDecimal simulatedGrossProfit;
    private final BigDecimal revenueDifference;
    private final BigDecimal profitDifference;
    private final BigDecimal minimumRecommendedPrice;
    private final boolean hasRecipeCost;
    private final boolean belowCost;
    private final boolean currentBelowCost;

    public ProductPriceSimulatorResult(
            Long productId,
            String productName,
            BigDecimal currentPrice,
            BigDecimal unitCost,
            BigDecimal simulatedPrice,
            BigDecimal estimatedQuantity,
            BigDecimal currentUnitMargin,
            BigDecimal currentMarginPercent,
            BigDecimal simulatedUnitMargin,
            BigDecimal simulatedMarginPercent,
            BigDecimal currentRevenue,
            BigDecimal simulatedRevenue,
            BigDecimal priceDifference,
            BigDecimal totalVariableCost,
            BigDecimal currentGrossProfit,
            BigDecimal simulatedGrossProfit,
            BigDecimal revenueDifference,
            BigDecimal profitDifference,
            BigDecimal minimumRecommendedPrice,
            boolean hasRecipeCost,
            boolean belowCost,
            boolean currentBelowCost
    ) {
        this.productId = productId;
        this.productName = productName;
        this.currentPrice = currentPrice;
        this.unitCost = unitCost;
        this.simulatedPrice = simulatedPrice;
        this.estimatedQuantity = estimatedQuantity;
        this.currentUnitMargin = currentUnitMargin;
        this.currentMarginPercent = currentMarginPercent;
        this.simulatedUnitMargin = simulatedUnitMargin;
        this.simulatedMarginPercent = simulatedMarginPercent;
        this.currentRevenue = currentRevenue;
        this.simulatedRevenue = simulatedRevenue;
        this.priceDifference = priceDifference;
        this.totalVariableCost = totalVariableCost;
        this.currentGrossProfit = currentGrossProfit;
        this.simulatedGrossProfit = simulatedGrossProfit;
        this.revenueDifference = revenueDifference;
        this.profitDifference = profitDifference;
        this.minimumRecommendedPrice = minimumRecommendedPrice;
        this.hasRecipeCost = hasRecipeCost;
        this.belowCost = belowCost;
        this.currentBelowCost = currentBelowCost;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getSimulatedPrice() {
        return simulatedPrice;
    }

    public BigDecimal getEstimatedQuantity() {
        return estimatedQuantity;
    }

    public BigDecimal getCurrentUnitMargin() {
        return currentUnitMargin;
    }

    public BigDecimal getCurrentMarginPercent() {
        return currentMarginPercent;
    }

    public BigDecimal getSimulatedUnitMargin() {
        return simulatedUnitMargin;
    }

    public BigDecimal getSimulatedMarginPercent() {
        return simulatedMarginPercent;
    }

    public BigDecimal getCurrentRevenue() {
        return currentRevenue;
    }

    public BigDecimal getSimulatedRevenue() {
        return simulatedRevenue;
    }

    public BigDecimal getPriceDifference() {
        return priceDifference;
    }

    public BigDecimal getTotalVariableCost() {
        return totalVariableCost;
    }

    public BigDecimal getCurrentGrossProfit() {
        return currentGrossProfit;
    }

    public BigDecimal getSimulatedGrossProfit() {
        return simulatedGrossProfit;
    }

    public BigDecimal getRevenueDifference() {
        return revenueDifference;
    }

    public BigDecimal getProfitDifference() {
        return profitDifference;
    }

    public BigDecimal getMinimumRecommendedPrice() {
        return minimumRecommendedPrice;
    }

    public boolean isHasRecipeCost() {
        return hasRecipeCost;
    }

    public boolean isBelowCost() {
        return belowCost;
    }

    public boolean isCurrentBelowCost() {
        return currentBelowCost;
    }
}
