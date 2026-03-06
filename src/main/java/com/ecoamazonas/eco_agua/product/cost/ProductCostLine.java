package com.ecoamazonas.eco_agua.product.cost;

import java.math.BigDecimal;

public class ProductCostLine {

    private String groupLabel;
    private String supplyName;
    private String unit;
    private BigDecimal quantityUsed;
    private BigDecimal unitCost;
    private BigDecimal totalCost;

    // --- Builder-style constructor for convenience ---

    public ProductCostLine(
            String groupLabel,
            String supplyName,
            String unit,
            BigDecimal quantityUsed,
            BigDecimal unitCost,
            BigDecimal totalCost
    ) {
        this.groupLabel = groupLabel;
        this.supplyName = supplyName;
        this.unit = unit;
        this.quantityUsed = quantityUsed;
        this.unitCost = unitCost;
        this.totalCost = totalCost;
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public String getSupplyName() {
        return supplyName;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getQuantityUsed() {
        return quantityUsed;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }
}
