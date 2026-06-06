package com.ecoamazonas.eco_agua.product.cost;

import java.math.BigDecimal;

public class ProductProfitabilitySummary {

    private final int productCount;
    private final BigDecimal totalRevenue;
    private final BigDecimal totalCost;
    private final BigDecimal totalGrossProfit;
    private final BigDecimal averageMarginPercent;
    private final ProductProfitabilityRow mostProfitableProduct;
    private final ProductProfitabilityRow topSellingProduct;
    private final ProductProfitabilityRow topSellingLowMarginProduct;

    public ProductProfitabilitySummary(
            int productCount,
            BigDecimal totalRevenue,
            BigDecimal totalCost,
            BigDecimal totalGrossProfit,
            BigDecimal averageMarginPercent,
            ProductProfitabilityRow mostProfitableProduct,
            ProductProfitabilityRow topSellingProduct,
            ProductProfitabilityRow topSellingLowMarginProduct
    ) {
        this.productCount = productCount;
        this.totalRevenue = totalRevenue;
        this.totalCost = totalCost;
        this.totalGrossProfit = totalGrossProfit;
        this.averageMarginPercent = averageMarginPercent;
        this.mostProfitableProduct = mostProfitableProduct;
        this.topSellingProduct = topSellingProduct;
        this.topSellingLowMarginProduct = topSellingLowMarginProduct;
    }

    public int getProductCount() {
        return productCount;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public BigDecimal getTotalGrossProfit() {
        return totalGrossProfit;
    }

    public BigDecimal getAverageMarginPercent() {
        return averageMarginPercent;
    }

    public ProductProfitabilityRow getMostProfitableProduct() {
        return mostProfitableProduct;
    }

    public ProductProfitabilityRow getTopSellingProduct() {
        return topSellingProduct;
    }

    public ProductProfitabilityRow getTopSellingLowMarginProduct() {
        return topSellingLowMarginProduct;
    }
}
