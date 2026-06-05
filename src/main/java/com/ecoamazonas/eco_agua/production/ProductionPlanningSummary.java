package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionPlanningSummary {

    private final Long productId;
    private final String productName;
    private final BigDecimal plannedQuantity;
    private final int requirementCount;
    private final int linesWithEnoughStock;
    private final int linesWithShortage;
    private final BigDecimal totalEstimatedCost;
    private final boolean canProduce;

    public ProductionPlanningSummary(
            Long productId,
            String productName,
            BigDecimal plannedQuantity,
            int requirementCount,
            int linesWithEnoughStock,
            int linesWithShortage,
            BigDecimal totalEstimatedCost,
            boolean canProduce
    ) {
        this.productId = productId;
        this.productName = productName;
        this.plannedQuantity = plannedQuantity;
        this.requirementCount = requirementCount;
        this.linesWithEnoughStock = linesWithEnoughStock;
        this.linesWithShortage = linesWithShortage;
        this.totalEstimatedCost = totalEstimatedCost;
        this.canProduce = canProduce;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getPlannedQuantity() {
        return plannedQuantity;
    }

    public int getRequirementCount() {
        return requirementCount;
    }

    public int getLinesWithEnoughStock() {
        return linesWithEnoughStock;
    }

    public int getLinesWithShortage() {
        return linesWithShortage;
    }

    public BigDecimal getTotalEstimatedCost() {
        return totalEstimatedCost;
    }

    public boolean isCanProduce() {
        return canProduce;
    }

    public boolean hasProduct() {
        return productId != null;
    }

    public boolean hasRecipe() {
        return requirementCount > 0;
    }
}
