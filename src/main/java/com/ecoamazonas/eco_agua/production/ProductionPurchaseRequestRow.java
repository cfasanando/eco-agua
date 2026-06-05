package com.ecoamazonas.eco_agua.production;

import java.math.BigDecimal;

public class ProductionPurchaseRequestRow {

    private final Long supplyId;
    private final String supplyName;
    private final String unit;
    private final BigDecimal requiredQuantity;
    private final BigDecimal availableQuantity;
    private final BigDecimal shortageQuantity;
    private final BigDecimal unitCost;
    private final BigDecimal estimatedCost;
    private final String suggestedSupplierName;

    public ProductionPurchaseRequestRow(
            Long supplyId,
            String supplyName,
            String unit,
            BigDecimal requiredQuantity,
            BigDecimal availableQuantity,
            BigDecimal shortageQuantity,
            BigDecimal unitCost,
            BigDecimal estimatedCost,
            String suggestedSupplierName
    ) {
        this.supplyId = supplyId;
        this.supplyName = supplyName;
        this.unit = unit;
        this.requiredQuantity = requiredQuantity;
        this.availableQuantity = availableQuantity;
        this.shortageQuantity = shortageQuantity;
        this.unitCost = unitCost;
        this.estimatedCost = estimatedCost;
        this.suggestedSupplierName = suggestedSupplierName;
    }

    public Long getSupplyId() { return supplyId; }
    public String getSupplyName() { return supplyName; }
    public String getUnit() { return unit; }
    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public BigDecimal getShortageQuantity() { return shortageQuantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public String getSuggestedSupplierName() { return suggestedSupplierName; }

    public boolean hasShortage() {
        return shortageQuantity != null && shortageQuantity.compareTo(BigDecimal.ZERO) > 0;
    }
}
