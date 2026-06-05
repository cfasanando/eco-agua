package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionPlanningRequirementRow {

    private final Long supplyId;
    private final String supplyName;
    private final String unit;
    private final BigDecimal quantityPerUnit;
    private final BigDecimal requiredQuantity;
    private final BigDecimal availableQuantity;
    private final BigDecimal shortageQuantity;
    private final BigDecimal unitCost;
    private final BigDecimal requiredCost;
    private final boolean enoughStock;

    public ProductionPlanningRequirementRow(
            Long supplyId,
            String supplyName,
            String unit,
            BigDecimal quantityPerUnit,
            BigDecimal requiredQuantity,
            BigDecimal availableQuantity,
            BigDecimal shortageQuantity,
            BigDecimal unitCost,
            BigDecimal requiredCost,
            boolean enoughStock
    ) {
        this.supplyId = supplyId;
        this.supplyName = supplyName;
        this.unit = unit;
        this.quantityPerUnit = quantityPerUnit;
        this.requiredQuantity = requiredQuantity;
        this.availableQuantity = availableQuantity;
        this.shortageQuantity = shortageQuantity;
        this.unitCost = unitCost;
        this.requiredCost = requiredCost;
        this.enoughStock = enoughStock;
    }

    public Long getSupplyId() {
        return supplyId;
    }

    public String getSupplyName() {
        return supplyName;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getQuantityPerUnit() {
        return quantityPerUnit;
    }

    public BigDecimal getRequiredQuantity() {
        return requiredQuantity;
    }

    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }

    public BigDecimal getShortageQuantity() {
        return shortageQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getRequiredCost() {
        return requiredCost;
    }

    public boolean isEnoughStock() {
        return enoughStock;
    }
}
