package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionMaterialRequirementSummary {

    private final Long productId;
    private final String productName;
    private final BigDecimal plannedQuantity;
    private final int requirementCount;
    private final int linesWithEnoughStock;
    private final int linesWithShortage;
    private final BigDecimal totalRequiredQuantity;
    private final BigDecimal totalShortageQuantity;
    private final BigDecimal totalEstimatedCost;
    private final BigDecimal totalShortageCost;
    private final boolean canProduce;

    public ProductionMaterialRequirementSummary(
            Long productId,
            String productName,
            BigDecimal plannedQuantity,
            int requirementCount,
            int linesWithEnoughStock,
            int linesWithShortage,
            BigDecimal totalRequiredQuantity,
            BigDecimal totalShortageQuantity,
            BigDecimal totalEstimatedCost,
            BigDecimal totalShortageCost,
            boolean canProduce
    ) {
        this.productId = productId;
        this.productName = productName;
        this.plannedQuantity = plannedQuantity;
        this.requirementCount = requirementCount;
        this.linesWithEnoughStock = linesWithEnoughStock;
        this.linesWithShortage = linesWithShortage;
        this.totalRequiredQuantity = totalRequiredQuantity;
        this.totalShortageQuantity = totalShortageQuantity;
        this.totalEstimatedCost = totalEstimatedCost;
        this.totalShortageCost = totalShortageCost;
        this.canProduce = canProduce;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getPlannedQuantity() { return plannedQuantity; }
    public int getRequirementCount() { return requirementCount; }
    public int getLinesWithEnoughStock() { return linesWithEnoughStock; }
    public int getLinesWithShortage() { return linesWithShortage; }
    public BigDecimal getTotalRequiredQuantity() { return totalRequiredQuantity; }
    public BigDecimal getTotalShortageQuantity() { return totalShortageQuantity; }
    public BigDecimal getTotalEstimatedCost() { return totalEstimatedCost; }
    public BigDecimal getTotalShortageCost() { return totalShortageCost; }
    public boolean isCanProduce() { return canProduce; }

    public boolean hasProduct() { return productId != null; }
    public boolean hasRecipe() { return requirementCount > 0; }
}
