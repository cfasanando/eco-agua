package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionRecipeSupplyRow {

    private final Long supplyId;
    private final String supplyName;
    private final String unit;
    private final BigDecimal quantityPerUnit;
    private final BigDecimal unitCost;
    private final BigDecimal lineTotal;
    private final BigDecimal availableStock;

    public ProductionRecipeSupplyRow(
            Long supplyId,
            String supplyName,
            String unit,
            BigDecimal quantityPerUnit,
            BigDecimal unitCost,
            BigDecimal lineTotal,
            BigDecimal availableStock
    ) {
        this.supplyId = supplyId;
        this.supplyName = supplyName;
        this.unit = unit;
        this.quantityPerUnit = quantityPerUnit;
        this.unitCost = unitCost;
        this.lineTotal = lineTotal;
        this.availableStock = availableStock;
    }

    public Long getSupplyId() { return supplyId; }
    public String getSupplyName() { return supplyName; }
    public String getUnit() { return unit; }
    public BigDecimal getQuantityPerUnit() { return quantityPerUnit; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public BigDecimal getAvailableStock() { return availableStock; }
}
