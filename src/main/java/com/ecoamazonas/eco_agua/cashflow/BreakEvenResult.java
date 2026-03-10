package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.product.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Result of a break-even analysis for one product and one period.
 */
public class BreakEvenResult {

    private final Product product;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    private final BigDecimal fixedCosts;
    private final BigDecimal baseIndustrialUnitCost;
    private final BigDecimal fuelVariableUnitCost;
    private final BigDecimal deliveryCommissionPercent;
    private final BigDecimal deliveryCommissionUnitCost;
    private final BigDecimal operationalVariableUnitCost;
    private final BigDecimal variableUnitCost;
    private final BigDecimal totalUnitsSold;
    private final BigDecimal totalRevenueInPeriod;
    private final BigDecimal actualAverageSellingPrice;

    private final boolean usesProfilePricing;
    private final String primaryScenarioLabel;

    private final BigDecimal sellingPrice;
    private final BigDecimal contributionMargin;
    private final BigDecimal breakEvenUnitsExact;
    private final BigDecimal breakEvenUnitsRounded;
    private final BigDecimal unitsSold;
    private final BreakEvenStatus status;
    private final BigDecimal safetyMarginUnits;
    private final BigDecimal safetyMarginPercent;

    private final List<BreakEvenScenario> scenarios;
    private final List<BreakEvenFixedCostLine> fixedCostLines;

    public BreakEvenResult(
            Product product,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal fixedCosts,
            BigDecimal baseIndustrialUnitCost,
            BigDecimal fuelVariableUnitCost,
            BigDecimal deliveryCommissionPercent,
            BigDecimal deliveryCommissionUnitCost,
            BigDecimal operationalVariableUnitCost,
            BigDecimal variableUnitCost,
            BigDecimal totalUnitsSold,
            BigDecimal totalRevenueInPeriod,
            BigDecimal actualAverageSellingPrice,
            boolean usesProfilePricing,
            String primaryScenarioLabel,
            BigDecimal sellingPrice,
            BigDecimal contributionMargin,
            BigDecimal breakEvenUnitsExact,
            BigDecimal breakEvenUnitsRounded,
            BigDecimal unitsSold,
            BreakEvenStatus status,
            BigDecimal safetyMarginUnits,
            BigDecimal safetyMarginPercent,
            List<BreakEvenScenario> scenarios,
            List<BreakEvenFixedCostLine> fixedCostLines
    ) {
        this.product = product;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.fixedCosts = fixedCosts;
        this.baseIndustrialUnitCost = baseIndustrialUnitCost;
        this.fuelVariableUnitCost = fuelVariableUnitCost;
        this.deliveryCommissionPercent = deliveryCommissionPercent;
        this.deliveryCommissionUnitCost = deliveryCommissionUnitCost;
        this.operationalVariableUnitCost = operationalVariableUnitCost;
        this.variableUnitCost = variableUnitCost;
        this.totalUnitsSold = totalUnitsSold;
        this.totalRevenueInPeriod = totalRevenueInPeriod;
        this.actualAverageSellingPrice = actualAverageSellingPrice;
        this.usesProfilePricing = usesProfilePricing;
        this.primaryScenarioLabel = primaryScenarioLabel;
        this.sellingPrice = sellingPrice;
        this.contributionMargin = contributionMargin;
        this.breakEvenUnitsExact = breakEvenUnitsExact;
        this.breakEvenUnitsRounded = breakEvenUnitsRounded;
        this.unitsSold = unitsSold;
        this.status = status;
        this.safetyMarginUnits = safetyMarginUnits;
        this.safetyMarginPercent = safetyMarginPercent;
        this.scenarios = scenarios;
        this.fixedCostLines = fixedCostLines;
    }

    public Product getProduct() {
        return product;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public BigDecimal getFixedCosts() {
        return fixedCosts;
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

    public BigDecimal getVariableUnitCost() {
        return variableUnitCost;
    }

    public BigDecimal getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public BigDecimal getTotalRevenueInPeriod() {
        return totalRevenueInPeriod;
    }

    public BigDecimal getActualAverageSellingPrice() {
        return actualAverageSellingPrice;
    }

    public boolean isUsesProfilePricing() {
        return usesProfilePricing;
    }

    public String getPrimaryScenarioLabel() {
        return primaryScenarioLabel;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
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

    public BreakEvenStatus getStatus() {
        return status;
    }

    public BigDecimal getSafetyMarginUnits() {
        return safetyMarginUnits;
    }

    public BigDecimal getSafetyMarginPercent() {
        return safetyMarginPercent;
    }

    public List<BreakEvenScenario> getScenarios() {
        return scenarios;
    }

    public List<BreakEvenFixedCostLine> getFixedCostLines() {
        return fixedCostLines;
    }
}
