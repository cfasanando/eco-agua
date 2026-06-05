package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionPurchaseRequestSummary {

    private final Long productId;
    private final String productName;
    private final BigDecimal plannedQuantity;
    private final int requirementCount;
    private final int purchaseLineCount;
    private final BigDecimal totalShortageQuantity;
    private final BigDecimal totalEstimatedCost;

    public ProductionPurchaseRequestSummary(
            Long productId,
            String productName,
            BigDecimal plannedQuantity,
            int requirementCount,
            int purchaseLineCount,
            BigDecimal totalShortageQuantity,
            BigDecimal totalEstimatedCost
    ) {
        this.productId = productId;
        this.productName = productName;
        this.plannedQuantity = plannedQuantity;
        this.requirementCount = requirementCount;
        this.purchaseLineCount = purchaseLineCount;
        this.totalShortageQuantity = totalShortageQuantity;
        this.totalEstimatedCost = totalEstimatedCost;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getPlannedQuantity() { return plannedQuantity; }
    public int getRequirementCount() { return requirementCount; }
    public int getPurchaseLineCount() { return purchaseLineCount; }
    public BigDecimal getTotalShortageQuantity() { return totalShortageQuantity; }
    public BigDecimal getTotalEstimatedCost() { return totalEstimatedCost; }

    public boolean hasProduct() { return productId != null; }
    public boolean hasRecipe() { return requirementCount > 0; }
    public boolean hasPurchaseLines() { return purchaseLineCount > 0; }
}
