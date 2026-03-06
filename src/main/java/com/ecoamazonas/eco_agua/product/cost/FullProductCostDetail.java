package com.ecoamazonas.eco_agua.product.cost;

import com.ecoamazonas.eco_agua.product.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FullProductCostDetail {

    private final Product product;
    private final LocalDate startDate;
    private final LocalDate endDate;

    private final ProductCostDetail industrialDetail;

    // Total units sold in the selected period
    private final BigDecimal totalUnits;

    // Total overhead expenses in the selected period
    private final BigDecimal totalOverheadExpenses;

    // Overhead cost allocated per unit
    private final BigDecimal overheadUnitCost;

    // Industrial CVU + overheadUnitCost
    private final BigDecimal fullUnitCost;

    // Overhead detail by category
    private final List<PeriodExpenseLine> expenseLines;

    public FullProductCostDetail(
            Product product,
            LocalDate startDate,
            LocalDate endDate,
            ProductCostDetail industrialDetail,
            BigDecimal totalUnits,
            BigDecimal totalOverheadExpenses,
            BigDecimal overheadUnitCost,
            BigDecimal fullUnitCost,
            List<PeriodExpenseLine> expenseLines
    ) {
        this.product = product;
        this.startDate = startDate;
        this.endDate = endDate;
        this.industrialDetail = industrialDetail;
        this.totalUnits = totalUnits;
        this.totalOverheadExpenses = totalOverheadExpenses;
        this.overheadUnitCost = overheadUnitCost;
        this.fullUnitCost = fullUnitCost;
        this.expenseLines = expenseLines;
    }

    public Product getProduct() {
        return product;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public ProductCostDetail getIndustrialDetail() {
        return industrialDetail;
    }

    public BigDecimal getTotalUnits() {
        return totalUnits;
    }

    public BigDecimal getTotalOverheadExpenses() {
        return totalOverheadExpenses;
    }

    public BigDecimal getOverheadUnitCost() {
        return overheadUnitCost;
    }

    public BigDecimal getFullUnitCost() {
        return fullUnitCost;
    }

    public List<PeriodExpenseLine> getExpenseLines() {
        return expenseLines;
    }
}
