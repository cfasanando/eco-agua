package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionCapacityProductRow {

    private final Long productId;
    private final String productName;
    private final BigDecimal productStock;
    private final boolean hasRecipe;
    private final int supplyCount;
    private final int blockedSupplyCount;
    private final String limitingSupplyName;
    private final String limitingSupplyUnit;
    private final BigDecimal limitingQuantityPerUnit;
    private final BigDecimal limitingAvailableQuantity;
    private final BigDecimal maximumProducibleQuantity;
    private final BigDecimal estimatedUnitCost;

    public ProductionCapacityProductRow(
            Long productId,
            String productName,
            BigDecimal productStock,
            boolean hasRecipe,
            int supplyCount,
            int blockedSupplyCount,
            String limitingSupplyName,
            String limitingSupplyUnit,
            BigDecimal limitingQuantityPerUnit,
            BigDecimal limitingAvailableQuantity,
            BigDecimal maximumProducibleQuantity,
            BigDecimal estimatedUnitCost
    ) {
        this.productId = productId;
        this.productName = productName;
        this.productStock = productStock;
        this.hasRecipe = hasRecipe;
        this.supplyCount = supplyCount;
        this.blockedSupplyCount = blockedSupplyCount;
        this.limitingSupplyName = limitingSupplyName;
        this.limitingSupplyUnit = limitingSupplyUnit;
        this.limitingQuantityPerUnit = limitingQuantityPerUnit;
        this.limitingAvailableQuantity = limitingAvailableQuantity;
        this.maximumProducibleQuantity = maximumProducibleQuantity;
        this.estimatedUnitCost = estimatedUnitCost;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getProductStock() { return productStock; }
    public boolean isHasRecipe() { return hasRecipe; }
    public int getSupplyCount() { return supplyCount; }
    public int getBlockedSupplyCount() { return blockedSupplyCount; }
    public String getLimitingSupplyName() { return limitingSupplyName; }
    public String getLimitingSupplyUnit() { return limitingSupplyUnit; }
    public BigDecimal getLimitingQuantityPerUnit() { return limitingQuantityPerUnit; }
    public BigDecimal getLimitingAvailableQuantity() { return limitingAvailableQuantity; }
    public BigDecimal getMaximumProducibleQuantity() { return maximumProducibleQuantity; }
    public BigDecimal getEstimatedUnitCost() { return estimatedUnitCost; }

    public boolean isCanProduce() {
        return hasRecipe && maximumProducibleQuantity != null && maximumProducibleQuantity.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isBlockedByStock() {
        return hasRecipe && !isCanProduce();
    }

    public String getStatusLabel() {
        if (!hasRecipe) {
            return "Sin receta";
        }
        if (isCanProduce()) {
            return "Puede producir";
        }
        return "Sin stock suficiente";
    }
}
