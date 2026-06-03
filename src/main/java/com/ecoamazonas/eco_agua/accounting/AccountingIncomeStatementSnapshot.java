package com.ecoamazonas.eco_agua.accounting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AccountingIncomeStatementSnapshot {

    private LocalDate startDate;
    private LocalDate endDate;
    private String statusFilter = "POSTED";
    private AccountingIncomeStatementSummary summary = new AccountingIncomeStatementSummary();
    private List<AccountingIncomeStatementRow> salesIncomeRows = new ArrayList<>();
    private List<AccountingIncomeStatementRow> salesDeductionRows = new ArrayList<>();
    private List<AccountingIncomeStatementRow> otherIncomeRows = new ArrayList<>();
    private List<AccountingIncomeStatementRow> costRows = new ArrayList<>();
    private List<AccountingIncomeStatementRow> expenseRows = new ArrayList<>();

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public AccountingIncomeStatementSummary getSummary() {
        return summary;
    }

    public void setSummary(AccountingIncomeStatementSummary summary) {
        this.summary = summary == null ? new AccountingIncomeStatementSummary() : summary;
    }

    public List<AccountingIncomeStatementRow> getSalesIncomeRows() {
        return salesIncomeRows;
    }

    public void setSalesIncomeRows(List<AccountingIncomeStatementRow> salesIncomeRows) {
        this.salesIncomeRows = salesIncomeRows == null ? new ArrayList<>() : salesIncomeRows;
    }

    public List<AccountingIncomeStatementRow> getSalesDeductionRows() {
        return salesDeductionRows;
    }

    public void setSalesDeductionRows(List<AccountingIncomeStatementRow> salesDeductionRows) {
        this.salesDeductionRows = salesDeductionRows == null ? new ArrayList<>() : salesDeductionRows;
    }

    public List<AccountingIncomeStatementRow> getOtherIncomeRows() {
        return otherIncomeRows;
    }

    public void setOtherIncomeRows(List<AccountingIncomeStatementRow> otherIncomeRows) {
        this.otherIncomeRows = otherIncomeRows == null ? new ArrayList<>() : otherIncomeRows;
    }

    public List<AccountingIncomeStatementRow> getCostRows() {
        return costRows;
    }

    public void setCostRows(List<AccountingIncomeStatementRow> costRows) {
        this.costRows = costRows == null ? new ArrayList<>() : costRows;
    }

    public List<AccountingIncomeStatementRow> getExpenseRows() {
        return expenseRows;
    }

    public void setExpenseRows(List<AccountingIncomeStatementRow> expenseRows) {
        this.expenseRows = expenseRows == null ? new ArrayList<>() : expenseRows;
    }

    public boolean hasRows() {
        return !salesIncomeRows.isEmpty()
                || !salesDeductionRows.isEmpty()
                || !otherIncomeRows.isEmpty()
                || !costRows.isEmpty()
                || !expenseRows.isEmpty();
    }
}
