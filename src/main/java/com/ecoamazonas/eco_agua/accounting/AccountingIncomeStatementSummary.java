package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;

public class AccountingIncomeStatementSummary {

    private int totalAccounts;
    private int totalMovements;
    private BigDecimal salesIncome = BigDecimal.ZERO;
    private BigDecimal salesDeductions = BigDecimal.ZERO;
    private BigDecimal otherIncome = BigDecimal.ZERO;
    private BigDecimal costOfSales = BigDecimal.ZERO;
    private BigDecimal operatingExpenses = BigDecimal.ZERO;

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(int totalAccounts) {
        this.totalAccounts = Math.max(totalAccounts, 0);
    }

    public int getTotalMovements() {
        return totalMovements;
    }

    public void setTotalMovements(int totalMovements) {
        this.totalMovements = Math.max(totalMovements, 0);
    }

    public BigDecimal getSalesIncome() {
        return salesIncome == null ? BigDecimal.ZERO : salesIncome;
    }

    public void setSalesIncome(BigDecimal salesIncome) {
        this.salesIncome = salesIncome == null ? BigDecimal.ZERO : salesIncome;
    }

    public BigDecimal getSalesDeductions() {
        return salesDeductions == null ? BigDecimal.ZERO : salesDeductions;
    }

    public void setSalesDeductions(BigDecimal salesDeductions) {
        this.salesDeductions = salesDeductions == null ? BigDecimal.ZERO : salesDeductions;
    }

    public BigDecimal getOtherIncome() {
        return otherIncome == null ? BigDecimal.ZERO : otherIncome;
    }

    public void setOtherIncome(BigDecimal otherIncome) {
        this.otherIncome = otherIncome == null ? BigDecimal.ZERO : otherIncome;
    }

    public BigDecimal getCostOfSales() {
        return costOfSales == null ? BigDecimal.ZERO : costOfSales;
    }

    public void setCostOfSales(BigDecimal costOfSales) {
        this.costOfSales = costOfSales == null ? BigDecimal.ZERO : costOfSales;
    }

    public BigDecimal getOperatingExpenses() {
        return operatingExpenses == null ? BigDecimal.ZERO : operatingExpenses;
    }

    public void setOperatingExpenses(BigDecimal operatingExpenses) {
        this.operatingExpenses = operatingExpenses == null ? BigDecimal.ZERO : operatingExpenses;
    }

    public BigDecimal getNetSales() {
        return getSalesIncome().subtract(getSalesDeductions());
    }

    public BigDecimal getTotalIncome() {
        return getNetSales().add(getOtherIncome());
    }

    public BigDecimal getGrossProfit() {
        return getNetSales().subtract(getCostOfSales());
    }

    public BigDecimal getTotalCostsAndExpenses() {
        return getCostOfSales().add(getOperatingExpenses());
    }

    public BigDecimal getNetResult() {
        return getTotalIncome().subtract(getTotalCostsAndExpenses());
    }

    public BigDecimal getAbsoluteNetResult() {
        return getNetResult().abs();
    }

    public boolean isProfit() {
        return getNetResult().compareTo(BigDecimal.ZERO) >= 0;
    }

    public String getResultLabel() {
        return isProfit() ? "Utilidad" : "Pérdida";
    }
}
