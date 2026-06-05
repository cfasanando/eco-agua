package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionMaterialRequirementRow {

    private final Long productId;
    private final String productName;
    private final Long supplyId;
    private final String supplyName;
    private final String unit;
    private final BigDecimal plannedQuantity;
    private final BigDecimal quantityPerUnit;
    private final BigDecimal requiredQuantity;
    private final BigDecimal availableQuantity;
    private final BigDecimal shortageQuantity;
    private final BigDecimal unitCost;
    private final BigDecimal requiredCost;
    private final BigDecimal shortageCost;
    private final boolean enoughStock;

    public ProductionMaterialRequirementRow(
            Long productId,
            String productName,
            Long supplyId,
            String supplyName,
            String unit,
            BigDecimal plannedQuantity,
            BigDecimal quantityPerUnit,
            BigDecimal requiredQuantity,
            BigDecimal availableQuantity,
            BigDecimal shortageQuantity,
            BigDecimal unitCost,
            BigDecimal requiredCost,
            BigDecimal shortageCost,
            boolean enoughStock
    ) {
        this.productId = productId;
        this.productName = productName;
        this.supplyId = supplyId;
        this.supplyName = supplyName;
        this.unit = unit;
        this.plannedQuantity = plannedQuantity;
        this.quantityPerUnit = quantityPerUnit;
        this.requiredQuantity = requiredQuantity;
        this.availableQuantity = availableQuantity;
        this.shortageQuantity = shortageQuantity;
        this.unitCost = unitCost;
        this.requiredCost = requiredCost;
        this.shortageCost = shortageCost;
        this.enoughStock = enoughStock;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Long getSupplyId() { return supplyId; }
    public String getSupplyName() { return supplyName; }
    public String getUnit() { return unit; }
    public BigDecimal getPlannedQuantity() { return plannedQuantity; }
    public BigDecimal getQuantityPerUnit() { return quantityPerUnit; }
    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public BigDecimal getShortageQuantity() { return shortageQuantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getRequiredCost() { return requiredCost; }
    public BigDecimal getShortageCost() { return shortageCost; }
    public boolean isEnoughStock() { return enoughStock; }
}
