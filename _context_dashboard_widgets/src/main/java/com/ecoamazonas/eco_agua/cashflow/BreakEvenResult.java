package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.product.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Result of a break-even analysis for one product and one period.
 */
public class BreakEvenResult {

    private final Product product;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    private final BigDecimal fixedCosts;
    private final BigDecimal baseIndustrialUnitCost;
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
    private final BigDecimal structuralGapUnits;
    private final BigDecimal structuralGapSales;

    private final BigDecimal operationalVariableTotal;
    private final BigDecimal operationalVariablePersonnelTotal;
    private final BigDecimal operationalVariableNonPersonnelTotal;
    private final BigDecimal operationalVariableUnitCost;
    private final BigDecimal operationalContributionMargin;
    private final BigDecimal operationalBreakEvenUnitsExact;
    private final BigDecimal operationalBreakEvenUnitsRounded;
    private final BigDecimal operationalGapUnits;
    private final BigDecimal operationalGapSales;
    private final BigDecimal operationalContributionTotalBeforeFixed;
    private final BigDecimal operationalResultAfterFixed;
    private final boolean operationalReadingAvailable;
    private final Map<String, BigDecimal> operationalVariableByCategory;

    private final List<BreakEvenScenario> scenarios;
    private final List<BreakEvenFixedCostLine> fixedCostLines;

    public BreakEvenResult(
            Product product,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal fixedCosts,
            BigDecimal baseIndustrialUnitCost,
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
            BigDecimal structuralGapUnits,
            BigDecimal structuralGapSales,
            BigDecimal operationalVariableTotal,
            BigDecimal operationalVariablePersonnelTotal,
            BigDecimal operationalVariableNonPersonnelTotal,
            BigDecimal operationalVariableUnitCost,
            BigDecimal operationalContributionMargin,
            BigDecimal operationalBreakEvenUnitsExact,
            BigDecimal operationalBreakEvenUnitsRounded,
            BigDecimal operationalGapUnits,
            BigDecimal operationalGapSales,
            BigDecimal operationalContributionTotalBeforeFixed,
            BigDecimal operationalResultAfterFixed,
            boolean operationalReadingAvailable,
            Map<String, BigDecimal> operationalVariableByCategory,
            List<BreakEvenScenario> scenarios,
            List<BreakEvenFixedCostLine> fixedCostLines
    ) {
        this.product = product;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.fixedCosts = fixedCosts;
        this.baseIndustrialUnitCost = baseIndustrialUnitCost;
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
        this.structuralGapUnits = structuralGapUnits;
        this.structuralGapSales = structuralGapSales;
        this.operationalVariableTotal = operationalVariableTotal;
        this.operationalVariablePersonnelTotal = operationalVariablePersonnelTotal;
        this.operationalVariableNonPersonnelTotal = operationalVariableNonPersonnelTotal;
        this.operationalVariableUnitCost = operationalVariableUnitCost;
        this.operationalContributionMargin = operationalContributionMargin;
        this.operationalBreakEvenUnitsExact = operationalBreakEvenUnitsExact;
        this.operationalBreakEvenUnitsRounded = operationalBreakEvenUnitsRounded;
        this.operationalGapUnits = operationalGapUnits;
        this.operationalGapSales = operationalGapSales;
        this.operationalContributionTotalBeforeFixed = operationalContributionTotalBeforeFixed;
        this.operationalResultAfterFixed = operationalResultAfterFixed;
        this.operationalReadingAvailable = operationalReadingAvailable;
        this.operationalVariableByCategory = operationalVariableByCategory;
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

    public BigDecimal getStructuralGapUnits() {
        return structuralGapUnits;
    }

    public BigDecimal getStructuralGapSales() {
        return structuralGapSales;
    }

    public BigDecimal getOperationalVariableTotal() {
        return operationalVariableTotal;
    }

    public BigDecimal getOperationalVariablePersonnelTotal() {
        return operationalVariablePersonnelTotal;
    }

    public BigDecimal getOperationalVariableNonPersonnelTotal() {
        return operationalVariableNonPersonnelTotal;
    }

    public BigDecimal getOperationalVariableUnitCost() {
        return operationalVariableUnitCost;
    }

    public BigDecimal getOperationalContributionMargin() {
        return operationalContributionMargin;
    }

    public BigDecimal getOperationalBreakEvenUnitsExact() {
        return operationalBreakEvenUnitsExact;
    }

    public BigDecimal getOperationalBreakEvenUnitsRounded() {
        return operationalBreakEvenUnitsRounded;
    }

    public BigDecimal getOperationalGapUnits() {
        return operationalGapUnits;
    }

    public BigDecimal getOperationalGapSales() {
        return operationalGapSales;
    }

    public BigDecimal getOperationalContributionTotalBeforeFixed() {
        return operationalContributionTotalBeforeFixed;
    }

    public BigDecimal getOperationalResultAfterFixed() {
        return operationalResultAfterFixed;
    }

    public boolean isOperationalReadingAvailable() {
        return operationalReadingAvailable;
    }

    public Map<String, BigDecimal> getOperationalVariableByCategory() {
        return operationalVariableByCategory;
    }

    public List<BreakEvenScenario> getScenarios() {
        return scenarios;
    }

    public List<BreakEvenFixedCostLine> getFixedCostLines() {
        return fixedCostLines;
    }
}
