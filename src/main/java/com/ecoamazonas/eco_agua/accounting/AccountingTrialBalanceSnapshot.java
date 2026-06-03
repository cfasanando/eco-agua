package com.ecoamazonas.eco_agua.accounting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AccountingTrialBalanceSnapshot {

    private LocalDate startDate;
    private LocalDate endDate;
    private String statusFilter = "ALL";
    private AccountingTrialBalanceSummary summary = new AccountingTrialBalanceSummary();
    private List<AccountingTrialBalanceRow> rows = new ArrayList<>();

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

    public AccountingTrialBalanceSummary getSummary() {
        return summary;
    }

    public void setSummary(AccountingTrialBalanceSummary summary) {
        this.summary = summary == null ? new AccountingTrialBalanceSummary() : summary;
    }

    public List<AccountingTrialBalanceRow> getRows() {
        return rows;
    }

    public void setRows(List<AccountingTrialBalanceRow> rows) {
        this.rows = rows == null ? new ArrayList<>() : rows;
    }
}
