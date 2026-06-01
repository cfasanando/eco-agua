package com.ecoamazonas.eco_agua.warehouse;

public class ProductStockSummary {

    private final int activeProducts;
    private final int outOfStockProducts;
    private final int lowStockProducts;
    private final int enoughStockProducts;

    public ProductStockSummary(int activeProducts, int outOfStockProducts, int lowStockProducts, int enoughStockProducts) {
        this.activeProducts = activeProducts;
        this.outOfStockProducts = outOfStockProducts;
        this.lowStockProducts = lowStockProducts;
        this.enoughStockProducts = enoughStockProducts;
    }

    public int getActiveProducts() {
        return activeProducts;
    }

    public int getOutOfStockProducts() {
        return outOfStockProducts;
    }

    public int getLowStockProducts() {
        return lowStockProducts;
    }

    public int getEnoughStockProducts() {
        return enoughStockProducts;
    }
}
