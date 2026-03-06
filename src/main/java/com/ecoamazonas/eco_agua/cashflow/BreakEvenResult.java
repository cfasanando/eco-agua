package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.product.Product;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Result of a break-even analysis for one product and one period.
 */
public class BreakEvenResult {

    private final Product product;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    private final BigDecimal fixedCosts;              // Total fixed costs for the period
    private final BigDecimal variableUnitCost;        // CVU
    private final BigDecimal sellingPrice;            // Current product price

    private final BigDecimal contributionMargin;      // sellingPrice - variableUnitCost
    private final BigDecimal breakEvenUnitsExact;     // Fixed / contributionMargin (with decimals)
    private final BigDecimal breakEvenUnitsRounded;   // Minimum integer units (ceil)

    private final BigDecimal unitsSold;               // Units actually sold in period
    private final BreakEvenStatus status;             // BEFORE / AT / AFTER

    private final BigDecimal safetyMarginUnits;       // unitsSold - breakEvenUnitsRounded
    private final BigDecimal safetyMarginPercent;     // safetyMarginUnits / breakEvenUnitsRounded (%)

    public BreakEvenResult(
            Product product,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal fixedCosts,
            BigDecimal variableUnitCost,
            BigDecimal sellingPrice,
            BigDecimal contributionMargin,
            BigDecimal breakEvenUnitsExact,
            BigDecimal breakEvenUnitsRounded,
            BigDecimal unitsSold,
            BreakEvenStatus status,
            BigDecimal safetyMarginUnits,
            BigDecimal safetyMarginPercent
    ) {
        this.product = product;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.fixedCosts = fixedCosts;
        this.variableUnitCost = variableUnitCost;
        this.sellingPrice = sellingPrice;
        this.contributionMargin = contributionMargin;
        this.breakEvenUnitsExact = breakEvenUnitsExact;
        this.breakEvenUnitsRounded = breakEvenUnitsRounded;
        this.unitsSold = unitsSold;
        this.status = status;
        this.safetyMarginUnits = safetyMarginUnits;
        this.safetyMarginPercent = safetyMarginPercent;
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

    public BigDecimal getVariableUnitCost() {
        return variableUnitCost;
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
}
