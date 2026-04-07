package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionRecipeLine {

    private final Long supplyId;
    private final String supplyName;
    private final String unit;
    private final BigDecimal recipeQuantityPerUnit;
    private final BigDecimal calculatedQuantity;
    private final BigDecimal unitCost;
    private final BigDecimal lineTotal;

    public ProductionRecipeLine(
            Long supplyId,
            String supplyName,
            String unit,
            BigDecimal recipeQuantityPerUnit,
            BigDecimal calculatedQuantity,
            BigDecimal unitCost,
            BigDecimal lineTotal
    ) {
        this.supplyId = supplyId;
        this.supplyName = supplyName;
        this.unit = unit;
        this.recipeQuantityPerUnit = recipeQuantityPerUnit;
        this.calculatedQuantity = calculatedQuantity;
        this.unitCost = unitCost;
        this.lineTotal = lineTotal;
    }

    public Long getSupplyId() { return supplyId; }
    public String getSupplyName() { return supplyName; }
    public String getUnit() { return unit; }
    public BigDecimal getRecipeQuantityPerUnit() { return recipeQuantityPerUnit; }
    public BigDecimal getCalculatedQuantity() { return calculatedQuantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
