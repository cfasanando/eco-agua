package com.ecoamazonas.eco_agua.warehouse;

public class ProductReorderSuggestionSummary {

    private final int activeProducts;
    private final int configuredMinimumProducts;
    private final int outOfStockProducts;
    private final int belowMinimumProducts;
    private final int atLimitProducts;
    private final int suggestedProducts;

    public ProductReorderSuggestionSummary(
            int activeProducts,
            int configuredMinimumProducts,
            int outOfStockProducts,
            int belowMinimumProducts,
            int atLimitProducts,
            int suggestedProducts
    ) {
        this.activeProducts = activeProducts;
        this.configuredMinimumProducts = configuredMinimumProducts;
        this.outOfStockProducts = outOfStockProducts;
        this.belowMinimumProducts = belowMinimumProducts;
        this.atLimitProducts = atLimitProducts;
        this.suggestedProducts = suggestedProducts;
    }

    public int getActiveProducts() {
        return activeProducts;
    }

    public int getConfiguredMinimumProducts() {
        return configuredMinimumProducts;
    }

    public int getOutOfStockProducts() {
        return outOfStockProducts;
    }

    public int getBelowMinimumProducts() {
        return belowMinimumProducts;
    }

    public int getAtLimitProducts() {
        return atLimitProducts;
    }

    public int getSuggestedProducts() {
        return suggestedProducts;
    }
}
