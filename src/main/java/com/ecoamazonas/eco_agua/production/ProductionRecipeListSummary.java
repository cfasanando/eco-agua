package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionRecipeListSummary {

    private final int totalProducts;
    private final int configuredRecipes;
    private final int pendingRecipes;
    private final BigDecimal averageEstimatedUnitCost;

    public ProductionRecipeListSummary(
            int totalProducts,
            int configuredRecipes,
            int pendingRecipes,
            BigDecimal averageEstimatedUnitCost
    ) {
        this.totalProducts = totalProducts;
        this.configuredRecipes = configuredRecipes;
        this.pendingRecipes = pendingRecipes;
        this.averageEstimatedUnitCost = averageEstimatedUnitCost;
    }

    public int getTotalProducts() { return totalProducts; }
    public int getConfiguredRecipes() { return configuredRecipes; }
    public int getPendingRecipes() { return pendingRecipes; }
    public BigDecimal getAverageEstimatedUnitCost() { return averageEstimatedUnitCost; }
}
