package com.ecoamazonas.eco_agua.cashflow;

import java.math.BigDecimal;

public class BreakEvenScenario {

    private final String label;
    private final String subtitle;
    private final BigDecimal sellingPrice;
    private final BigDecimal realizedAveragePrice;
    private final BigDecimal baseIndustrialUnitCost;
    private final BigDecimal fuelVariableUnitCost;
    private final BigDecimal deliveryCommissionPercent;
    private final BigDecimal deliveryCommissionUnitCost;
    private final BigDecimal operationalVariableUnitCost;
    private final BigDecimal totalVariableUnitCost;
    private final BigDecimal contributionMargin;
    private final BigDecimal breakEvenUnitsExact;
    private final BigDecimal breakEvenUnitsRounded;
    private final BigDecimal unitsSold;
    private final BigDecimal revenueInPeriod;
    private final BreakEvenStatus status;
    private final BigDecimal safetyMarginUnits;
    private final BigDecimal safetyMarginPercent;
    private final boolean primary;

    public BreakEvenScenario(
            String label,
            String subtitle,
            BigDecimal sellingPrice,
            BigDecimal realizedAveragePrice,
            BigDecimal baseIndustrialUnitCost,
            BigDecimal fuelVariableUnitCost,
            BigDecimal deliveryCommissionPercent,
            BigDecimal deliveryCommissionUnitCost,
            BigDecimal operationalVariableUnitCost,
            BigDecimal totalVariableUnitCost,
            BigDecimal contributionMargin,
            BigDecimal breakEvenUnitsExact,
            BigDecimal breakEvenUnitsRounded,
            BigDecimal unitsSold,
            BigDecimal revenueInPeriod,
            BreakEvenStatus status,
            BigDecimal safetyMarginUnits,
            BigDecimal safetyMarginPercent,
            boolean primary
    ) {
        this.label = label;
        this.subtitle = subtitle;
        this.sellingPrice = sellingPrice;
        this.realizedAveragePrice = realizedAveragePrice;
        this.baseIndustrialUnitCost = baseIndustrialUnitCost;
        this.fuelVariableUnitCost = fuelVariableUnitCost;
        this.deliveryCommissionPercent = deliveryCommissionPercent;
        this.deliveryCommissionUnitCost = deliveryCommissionUnitCost;
        this.operationalVariableUnitCost = operationalVariableUnitCost;
        this.totalVariableUnitCost = totalVariableUnitCost;
        this.contributionMargin = contributionMargin;
        this.breakEvenUnitsExact = breakEvenUnitsExact;
        this.breakEvenUnitsRounded = breakEvenUnitsRounded;
        this.unitsSold = unitsSold;
        this.revenueInPeriod = revenueInPeriod;
        this.status = status;
        this.safetyMarginUnits = safetyMarginUnits;
        this.safetyMarginPercent = safetyMarginPercent;
        this.primary = primary;
    }

    public String getLabel() {
        return label;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public BigDecimal getRealizedAveragePrice() {
        return realizedAveragePrice;
    }

    public BigDecimal getBaseIndustrialUnitCost() {
        return baseIndustrialUnitCost;
    }

    public BigDecimal getFuelVariableUnitCost() {
        return fuelVariableUnitCost;
    }

    public BigDecimal getDeliveryCommissionPercent() {
        return deliveryCommissionPercent;
    }

    public BigDecimal getDeliveryCommissionUnitCost() {
        return deliveryCommissionUnitCost;
    }

    public BigDecimal getOperationalVariableUnitCost() {
        return operationalVariableUnitCost;
    }

    public BigDecimal getTotalVariableUnitCost() {
        return totalVariableUnitCost;
    }

    public BigDecimal getContributionMargin() {
        return contributionMargin;
    }

    public BigDecimal getBreakEvenUnitsExact() {
        return breakEvenUnitsExact;
    }

    public BigDecimal getBreakEvenUnitsRounded() {
        return breakEvenUnitsRounded;
    }

    public BigDecimal getUnitsSold() {
        return unitsSold;
    }

    public BigDecimal getRevenueInPeriod() {
        return revenueInPeriod;
    }

    public BreakEvenStatus getStatus() {
        return status;
    }

    public BigDecimal getSafetyMarginUnits() {
        return safetyMarginUnits;
    }

    public BigDecimal getSafetyMarginPercent() {
        return safetyMarginPercent;
    }

    public boolean isPrimary() {
        return primary;
    }
}
