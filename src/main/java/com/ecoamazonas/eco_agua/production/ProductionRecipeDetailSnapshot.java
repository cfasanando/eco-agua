package com.ecoamazonas.eco_agua.production;

import com.ecoamazonas.eco_agua.supply.Supply;

import java.math.BigDecimal;
import java.util.List;

public class ProductionRecipeDetailSnapshot {

    private final Long productId;
    private final String productName;
    private final BigDecimal productStock;
    private final BigDecimal estimatedUnitCost;
    private final List<ProductionRecipeSupplyRow> recipeRows;
    private final List<Supply> availableSupplies;

    public ProductionRecipeDetailSnapshot(
            Long productId,
            String productName,
            BigDecimal productStock,
            BigDecimal estimatedUnitCost,
            List<ProductionRecipeSupplyRow> recipeRows,
            List<Supply> availableSupplies
    ) {
        this.productId = productId;
        this.productName = productName;
        this.productStock = productStock;
        this.estimatedUnitCost = estimatedUnitCost;
        this.recipeRows = recipeRows;
        this.availableSupplies = availableSupplies;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getProductStock() { return productStock; }
    public BigDecimal getEstimatedUnitCost() { return estimatedUnitCost; }
    public List<ProductionRecipeSupplyRow> getRecipeRows() { return recipeRows; }
    public List<Supply> getAvailableSupplies() { return availableSupplies; }

    public boolean isConfigured() {
        return recipeRows != null && !recipeRows.isEmpty();
    }
}
