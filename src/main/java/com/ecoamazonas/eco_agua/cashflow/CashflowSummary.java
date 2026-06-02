package com.ecoamazonas.eco_agua.cashflow;

import java.math.BigDecimal;

public class CashflowSummary {

    private BigDecimal directSalesIncome = BigDecimal.ZERO;
    private BigDecimal creditCollections = BigDecimal.ZERO;
    private BigDecimal otherIncome = BigDecimal.ZERO;
    private BigDecimal collectedIncome = BigDecimal.ZERO;
    private BigDecimal cashExpenses = BigDecimal.ZERO;
    private BigDecimal debtPayments = BigDecimal.ZERO;
    private BigDecimal paidExpenses = BigDecimal.ZERO;
    private BigDecimal netCashResult = BigDecimal.ZERO;
    private BigDecimal registeredSales = BigDecimal.ZERO;
    private BigDecimal registeredExpenses = BigDecimal.ZERO;
    private BigDecimal registeredResult = BigDecimal.ZERO;
    private BigDecimal pendingReceivables = BigDecimal.ZERO;
    private BigDecimal pendingPayables = BigDecimal.ZERO;

    public BigDecimal getDirectSalesIncome() {
        return directSalesIncome;
    }

    public void setDirectSalesIncome(BigDecimal directSalesIncome) {
        this.directSalesIncome = safe(directSalesIncome);
    }

    public BigDecimal getCreditCollections() {
        return creditCollections;
    }

    public void setCreditCollections(BigDecimal creditCollections) {
        this.creditCollections = safe(creditCollections);
    }

    public BigDecimal getOtherIncome() {
        return otherIncome;
    }

    public void setOtherIncome(BigDecimal otherIncome) {
        this.otherIncome = safe(otherIncome);
    }

    public BigDecimal getCollectedIncome() {
        return collectedIncome;
    }

    public void setCollectedIncome(BigDecimal collectedIncome) {
        this.collectedIncome = safe(collectedIncome);
    }

    public BigDecimal getCashExpenses() {
        return cashExpenses;
    }

    public void setCashExpenses(BigDecimal cashExpenses) {
        this.cashExpenses = safe(cashExpenses);
    }

    public BigDecimal getDebtPayments() {
        return debtPayments;
    }

    public void setDebtPayments(BigDecimal debtPayments) {
        this.debtPayments = safe(debtPayments);
    }

    public BigDecimal getPaidExpenses() {
        return paidExpenses;
    }

    public void setPaidExpenses(BigDecimal paidExpenses) {
        this.paidExpenses = safe(paidExpenses);
    }

    public BigDecimal getNetCashResult() {
        return netCashResult;
    }

    public void setNetCashResult(BigDecimal netCashResult) {
        this.netCashResult = safe(netCashResult);
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

    public BigDecimal getPendingReceivables() {
        return pendingReceivables;
    }

    public void setPendingReceivables(BigDecimal pendingReceivables) {
        this.pendingReceivables = safe(pendingReceivables);
    }

    public BigDecimal getPendingPayables() {
        return pendingPayables;
    }

    public void setPendingPayables(BigDecimal pendingPayables) {
        this.pendingPayables = safe(pendingPayables);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
