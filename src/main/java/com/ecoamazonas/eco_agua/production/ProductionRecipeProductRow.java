package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionRecipeProductRow {

    private final Long productId;
    private final String productName;
    private final BigDecimal productStock;
    private final int supplyCount;
    private final BigDecimal estimatedUnitCost;

    public ProductionRecipeProductRow(
            Long productId,
            String productName,
            BigDecimal productStock,
            int supplyCount,
            BigDecimal estimatedUnitCost
    ) {
        this.productId = productId;
        this.productName = productName;
        this.productStock = productStock;
        this.supplyCount = supplyCount;
        this.estimatedUnitCost = estimatedUnitCost;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getProductStock() { return productStock; }
    public int getSupplyCount() { return supplyCount; }
    public BigDecimal getEstimatedUnitCost() { return estimatedUnitCost; }

    public boolean isConfigured() {
        return supplyCount > 0;
    }
}
