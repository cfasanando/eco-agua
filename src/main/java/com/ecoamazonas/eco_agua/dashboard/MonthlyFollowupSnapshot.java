package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.cashflow.BreakEvenResult;
import com.ecoamazonas.eco_agua.product.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class MonthlyFollowupSnapshot {

    private final Product product;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final int daysInMonth;
    private final int elapsedDays;
    private final int remainingDays;
    private final boolean currentMonth;
    private final BigDecimal baseFixedMonth;
    private final BigDecimal breakEvenUnits;
    private final BigDecimal soldUnits;
    private final BigDecimal missingUnits;
    private final BigDecimal soldProgressPercent;
    private final BigDecimal expectedUnitsByToday;
    private final BigDecimal paceGapUnits;
    private final BigDecimal averageSellingPrice;
    private final BigDecimal totalRevenue;
    private final BigDecimal contributionMarginUnit;
    private final BigDecimal targetUnitsPerRemainingDay;
    private final BigDecimal targetSalesPerRemainingDay;
    private final String paceLabel;
    private final String paceBadgeClass;
    private final String paceSummary;
    private final String mainRecommendation;
    private final BigDecimal operationalVariableTotal;
    private final BigDecimal operationalVariablePersonnelTotal;
    private final BigDecimal operationalVariableNonPersonnelTotal;
    private final BigDecimal operationalVariableUnitCost;
    private final BigDecimal operationalContributionMargin;
    private final BigDecimal operationalBreakEvenUnits;
    private final BigDecimal operationalGapUnits;
    private final BigDecimal operationalGapSales;
    private final BigDecimal operationalContributionBeforeFixed;
    private final BigDecimal operationalResultAfterFixed;
    private final boolean operationalReadingAvailable;
    private final Map<String, BigDecimal> operationalVariableByCategory;
    private final BreakEvenResult breakEvenResult;

    public MonthlyFollowupSnapshot(
            Product product,
            LocalDate periodStart,
            LocalDate periodEnd,
            int daysInMonth,
            int elapsedDays,
            int remainingDays,
            boolean currentMonth,
            BigDecimal baseFixedMonth,
            BigDecimal breakEvenUnits,
            BigDecimal soldUnits,
            BigDecimal missingUnits,
            BigDecimal soldProgressPercent,
            BigDecimal expectedUnitsByToday,
            BigDecimal paceGapUnits,
            BigDecimal averageSellingPrice,
            BigDecimal totalRevenue,
            BigDecimal contributionMarginUnit,
            BigDecimal targetUnitsPerRemainingDay,
            BigDecimal targetSalesPerRemainingDay,
            String paceLabel,
            String paceBadgeClass,
            String paceSummary,
            String mainRecommendation,
            BigDecimal operationalVariableTotal,
            BigDecimal operationalVariablePersonnelTotal,
            BigDecimal operationalVariableNonPersonnelTotal,
            BigDecimal operationalVariableUnitCost,
            BigDecimal operationalContributionMargin,
            BigDecimal operationalBreakEvenUnits,
            BigDecimal operationalGapUnits,
            BigDecimal operationalGapSales,
            BigDecimal operationalContributionBeforeFixed,
            BigDecimal operationalResultAfterFixed,
            boolean operationalReadingAvailable,
            Map<String, BigDecimal> operationalVariableByCategory,
            BreakEvenResult breakEvenResult
    ) {
        this.product = product;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.daysInMonth = daysInMonth;
        this.elapsedDays = elapsedDays;
        this.remainingDays = remainingDays;
        this.currentMonth = currentMonth;
        this.baseFixedMonth = baseFixedMonth;
        this.breakEvenUnits = breakEvenUnits;
        this.soldUnits = soldUnits;
        this.missingUnits = missingUnits;
        this.soldProgressPercent = soldProgressPercent;
        this.expectedUnitsByToday = expectedUnitsByToday;
        this.paceGapUnits = paceGapUnits;
        this.averageSellingPrice = averageSellingPrice;
        this.totalRevenue = totalRevenue;
        this.contributionMarginUnit = contributionMarginUnit;
        this.targetUnitsPerRemainingDay = targetUnitsPerRemainingDay;
        this.targetSalesPerRemainingDay = targetSalesPerRemainingDay;
        this.paceLabel = paceLabel;
        this.paceBadgeClass = paceBadgeClass;
        this.paceSummary = paceSummary;
        this.mainRecommendation = mainRecommendation;
        this.operationalVariableTotal = operationalVariableTotal;
        this.operationalVariablePersonnelTotal = operationalVariablePersonnelTotal;
        this.operationalVariableNonPersonnelTotal = operationalVariableNonPersonnelTotal;
        this.operationalVariableUnitCost = operationalVariableUnitCost;
        this.operationalContributionMargin = operationalContributionMargin;
        this.operationalBreakEvenUnits = operationalBreakEvenUnits;
        this.operationalGapUnits = operationalGapUnits;
        this.operationalGapSales = operationalGapSales;
        this.operationalContributionBeforeFixed = operationalContributionBeforeFixed;
        this.operationalResultAfterFixed = operationalResultAfterFixed;
        this.operationalReadingAvailable = operationalReadingAvailable;
        this.operationalVariableByCategory = operationalVariableByCategory;
        this.breakEvenResult = breakEvenResult;
    }

    public Product getProduct() { return product; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public int getDaysInMonth() { return daysInMonth; }
    public int getElapsedDays() { return elapsedDays; }
    public int getRemainingDays() { return remainingDays; }
    public boolean isCurrentMonth() { return currentMonth; }
    public BigDecimal getBaseFixedMonth() { return baseFixedMonth; }
    public BigDecimal getBreakEvenUnits() { return breakEvenUnits; }
    public BigDecimal getSoldUnits() { return soldUnits; }
    public BigDecimal getMissingUnits() { return missingUnits; }
    public BigDecimal getSoldProgressPercent() { return soldProgressPercent; }
    public BigDecimal getExpectedUnitsByToday() { return expectedUnitsByToday; }
    public BigDecimal getPaceGapUnits() { return paceGapUnits; }
    public BigDecimal getAverageSellingPrice() { return averageSellingPrice; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getContributionMarginUnit() { return contributionMarginUnit; }
    public BigDecimal getTargetUnitsPerRemainingDay() { return targetUnitsPerRemainingDay; }
    public BigDecimal getTargetSalesPerRemainingDay() { return targetSalesPerRemainingDay; }
    public String getPaceLabel() { return paceLabel; }
    public String getPaceBadgeClass() { return paceBadgeClass; }
    public String getPaceSummary() { return paceSummary; }
    public String getMainRecommendation() { return mainRecommendation; }
    public BigDecimal getOperationalVariableTotal() { return operationalVariableTotal; }
    public BigDecimal getOperationalVariablePersonnelTotal() { return operationalVariablePersonnelTotal; }
    public BigDecimal getOperationalVariableNonPersonnelTotal() { return operationalVariableNonPersonnelTotal; }
    public BigDecimal getOperationalVariableUnitCost() { return operationalVariableUnitCost; }
    public BigDecimal getOperationalContributionMargin() { return operationalContributionMargin; }
    public BigDecimal getOperationalBreakEvenUnits() { return operationalBreakEvenUnits; }
    public BigDecimal getOperationalGapUnits() { return operationalGapUnits; }
    public BigDecimal getOperationalGapSales() { return operationalGapSales; }
    public BigDecimal getOperationalContributionBeforeFixed() { return operationalContributionBeforeFixed; }
    public BigDecimal getOperationalResultAfterFixed() { return operationalResultAfterFixed; }
    public boolean isOperationalReadingAvailable() { return operationalReadingAvailable; }
    public Map<String, BigDecimal> getOperationalVariableByCategory() { return operationalVariableByCategory; }
    public BreakEvenResult getBreakEvenResult() { return breakEvenResult; }
}
