package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.income.OtherIncome;
import com.ecoamazonas.eco_agua.order.SaleOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CashflowDayDetail {

    private LocalDate date;
    private List<SaleOrder> sales = new ArrayList<>();
    private List<OtherIncome> otherIncomes = new ArrayList<>();
    private List<Expense> expenses = new ArrayList<>();

    private BigDecimal salesTotal = BigDecimal.ZERO;
    private BigDecimal otherIncomeTotal = BigDecimal.ZERO;
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal expenseTotal = BigDecimal.ZERO;
    private BigDecimal netResult = BigDecimal.ZERO;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<SaleOrder> getSales() {
        return sales;
    }

    public void setSales(List<SaleOrder> sales) {
        this.sales = (sales != null ? sales : new ArrayList<>());
    }

    public List<OtherIncome> getOtherIncomes() {
        return otherIncomes;
    }

    public void setOtherIncomes(List<OtherIncome> otherIncomes) {
        this.otherIncomes = (otherIncomes != null ? otherIncomes : new ArrayList<>());
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = (expenses != null ? expenses : new ArrayList<>());
    }

    public BigDecimal getSalesTotal() {
        return salesTotal;
    }

    public void setSalesTotal(BigDecimal salesTotal) {
        this.salesTotal = (salesTotal != null ? salesTotal : BigDecimal.ZERO);
    }

    public BigDecimal getOtherIncomeTotal() {
        return otherIncomeTotal;
    }

    public void setOtherIncomeTotal(BigDecimal otherIncomeTotal) {
        this.otherIncomeTotal = (otherIncomeTotal != null ? otherIncomeTotal : BigDecimal.ZERO);
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = (totalIncome != null ? totalIncome : BigDecimal.ZERO);
    }

    public BigDecimal getExpenseTotal() {
        return expenseTotal;
    }

    public void setExpenseTotal(BigDecimal expenseTotal) {
        this.expenseTotal = (expenseTotal != null ? expenseTotal : BigDecimal.ZERO);
    }

    public BigDecimal getNetResult() {
        return netResult;
    }

    public void setNetResult(BigDecimal netResult) {
        this.netResult = (netResult != null ? netResult : BigDecimal.ZERO);
    }
}