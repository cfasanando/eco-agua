package com.ecoamazonas.eco_agua.cashflow;

import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpensePayment;
import com.ecoamazonas.eco_agua.income.OtherIncome;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CashflowDayDetail {

    private LocalDate date;
    private List<SaleOrder> directSales = new ArrayList<>();
    private List<SaleOrderPayment> creditCollections = new ArrayList<>();
    private List<OtherIncome> otherIncomes = new ArrayList<>();
    private List<Expense> cashExpenses = new ArrayList<>();
    private List<ExpensePayment> debtPayments = new ArrayList<>();

    private BigDecimal directSalesTotal = BigDecimal.ZERO;
    private BigDecimal creditCollectionTotal = BigDecimal.ZERO;
    private BigDecimal salesTotal = BigDecimal.ZERO;
    private BigDecimal otherIncomeTotal = BigDecimal.ZERO;
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal cashExpenseTotal = BigDecimal.ZERO;
    private BigDecimal debtPaymentTotal = BigDecimal.ZERO;
    private BigDecimal expenseTotal = BigDecimal.ZERO;
    private BigDecimal netResult = BigDecimal.ZERO;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<SaleOrder> getDirectSales() {
        return directSales;
    }

    public void setDirectSales(List<SaleOrder> directSales) {
        this.directSales = directSales != null ? directSales : new ArrayList<>();
    }

    public List<SaleOrder> getSales() {
        return directSales;
    }

    public void setSales(List<SaleOrder> sales) {
        setDirectSales(sales);
    }

    public List<SaleOrderPayment> getCreditCollections() {
        return creditCollections;
    }

    public void setCreditCollections(List<SaleOrderPayment> creditCollections) {
        this.creditCollections = creditCollections != null ? creditCollections : new ArrayList<>();
    }

    public List<OtherIncome> getOtherIncomes() {
        return otherIncomes;
    }

    public void setOtherIncomes(List<OtherIncome> otherIncomes) {
        this.otherIncomes = otherIncomes != null ? otherIncomes : new ArrayList<>();
    }

    public List<Expense> getCashExpenses() {
        return cashExpenses;
    }

    public void setCashExpenses(List<Expense> cashExpenses) {
        this.cashExpenses = cashExpenses != null ? cashExpenses : new ArrayList<>();
    }

    public List<Expense> getExpenses() {
        return cashExpenses;
    }

    public void setExpenses(List<Expense> expenses) {
        setCashExpenses(expenses);
    }

    public List<ExpensePayment> getDebtPayments() {
        return debtPayments;
    }

    public void setDebtPayments(List<ExpensePayment> debtPayments) {
        this.debtPayments = debtPayments != null ? debtPayments : new ArrayList<>();
    }

    public BigDecimal getDirectSalesTotal() {
        return directSalesTotal;
    }

    public void setDirectSalesTotal(BigDecimal directSalesTotal) {
        this.directSalesTotal = safe(directSalesTotal);
    }

    public BigDecimal getCreditCollectionTotal() {
        return creditCollectionTotal;
    }

    public void setCreditCollectionTotal(BigDecimal creditCollectionTotal) {
        this.creditCollectionTotal = safe(creditCollectionTotal);
    }

    public BigDecimal getSalesTotal() {
        return salesTotal;
    }

    public void setSalesTotal(BigDecimal salesTotal) {
        this.salesTotal = safe(salesTotal);
    }

    public BigDecimal getOtherIncomeTotal() {
        return otherIncomeTotal;
    }

    public void setOtherIncomeTotal(BigDecimal otherIncomeTotal) {
        this.otherIncomeTotal = safe(otherIncomeTotal);
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = safe(totalIncome);
    }

    public BigDecimal getCashExpenseTotal() {
        return cashExpenseTotal;
    }

    public void setCashExpenseTotal(BigDecimal cashExpenseTotal) {
        this.cashExpenseTotal = safe(cashExpenseTotal);
    }

    public BigDecimal getDebtPaymentTotal() {
        return debtPaymentTotal;
    }

    public void setDebtPaymentTotal(BigDecimal debtPaymentTotal) {
        this.debtPaymentTotal = safe(debtPaymentTotal);
    }

    public BigDecimal getExpenseTotal() {
        return expenseTotal;
    }

    public void setExpenseTotal(BigDecimal expenseTotal) {
        this.expenseTotal = safe(expenseTotal);
    }

    public BigDecimal getNetResult() {
        return netResult;
    }

    public void setNetResult(BigDecimal netResult) {
        this.netResult = safe(netResult);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
