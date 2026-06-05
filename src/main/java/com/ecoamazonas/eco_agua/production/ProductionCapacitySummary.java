package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionCapacitySummary {

    private final int totalProducts;
    private final int productsWithRecipe;
    private final int productsWithoutRecipe;
    private final int producibleProducts;
    private final int blockedProducts;
    private final BigDecimal totalMaximumProducibleQuantity;
    private final BigDecimal averageEstimatedUnitCost;

    public ProductionCapacitySummary(
            int totalProducts,
            int productsWithRecipe,
            int productsWithoutRecipe,
            int producibleProducts,
            int blockedProducts,
            BigDecimal totalMaximumProducibleQuantity,
            BigDecimal averageEstimatedUnitCost
    ) {
        this.totalProducts = totalProducts;
        this.productsWithRecipe = productsWithRecipe;
        this.productsWithoutRecipe = productsWithoutRecipe;
        this.producibleProducts = producibleProducts;
        this.blockedProducts = blockedProducts;
        this.totalMaximumProducibleQuantity = totalMaximumProducibleQuantity;
        this.averageEstimatedUnitCost = averageEstimatedUnitCost;
    }

    public int getTotalProducts() { return totalProducts; }
    public int getProductsWithRecipe() { return productsWithRecipe; }
    public int getProductsWithoutRecipe() { return productsWithoutRecipe; }
    public int getProducibleProducts() { return producibleProducts; }
    public int getBlockedProducts() { return blockedProducts; }
    public BigDecimal getTotalMaximumProducibleQuantity() { return totalMaximumProducibleQuantity; }
    public BigDecimal getAverageEstimatedUnitCost() { return averageEstimatedUnitCost; }
}
