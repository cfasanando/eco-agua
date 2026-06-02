package com.ecoamazonas.eco_agua.cashflow;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CashflowItem {

    private LocalDate date;
    private BigDecimal directSalesIncome = BigDecimal.ZERO;
    private BigDecimal creditCollectionIncome = BigDecimal.ZERO;
    private BigDecimal salesIncome = BigDecimal.ZERO;
    private BigDecimal otherIncome = BigDecimal.ZERO;
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal cashExpense = BigDecimal.ZERO;
    private BigDecimal debtPaymentExpense = BigDecimal.ZERO;
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private BigDecimal netResult = BigDecimal.ZERO;
    private BigDecimal registeredSales = BigDecimal.ZERO;
    private BigDecimal registeredExpenses = BigDecimal.ZERO;
    private BigDecimal registeredResult = BigDecimal.ZERO;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getDirectSalesIncome() {
        return directSalesIncome;
    }

    public void setDirectSalesIncome(BigDecimal directSalesIncome) {
        this.directSalesIncome = safe(directSalesIncome);
    }

    public BigDecimal getCreditCollectionIncome() {
        return creditCollectionIncome;
    }

    public void setCreditCollectionIncome(BigDecimal creditCollectionIncome) {
        this.creditCollectionIncome = safe(creditCollectionIncome);
    }

    public BigDecimal getSalesIncome() {
        return salesIncome;
    }

    public void setSalesIncome(BigDecimal salesIncome) {
        this.salesIncome = safe(salesIncome);
    }

    public BigDecimal getOtherIncome() {
        return otherIncome;
    }

    public void setOtherIncome(BigDecimal otherIncome) {
        this.otherIncome = safe(otherIncome);
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = safe(totalIncome);
    }

    public BigDecimal getCashExpense() {
        return cashExpense;
    }

    public void setCashExpense(BigDecimal cashExpense) {
        this.cashExpense = safe(cashExpense);
    }

    public BigDecimal getDebtPaymentExpense() {
        return debtPaymentExpense;
    }

    public void setDebtPaymentExpense(BigDecimal debtPaymentExpense) {
        this.debtPaymentExpense = safe(debtPaymentExpense);
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = safe(totalExpense);
    }

    public BigDecimal getNetResult() {
        return netResult;
    }

    public void setNetResult(BigDecimal netResult) {
        this.netResult = safe(netResult);
    }

    public BigDecimal getRegisteredSales() {
        return registeredSales;
    }

    public void setRegisteredSales(BigDecimal registeredSales) {
        this.registeredSales = safe(registeredSales);
    }

    public BigDecimal getRegisteredExpenses() {
        return registeredExpenses;
    }

    public void setRegisteredExpenses(BigDecimal registeredExpenses) {
        this.registeredExpenses = safe(registeredExpenses);
    }

    public BigDecimal getRegisteredResult() {
        return registeredResult;
    }

    public void setRegisteredResult(BigDecimal registeredResult) {
        this.registeredResult = safe(registeredResult);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
