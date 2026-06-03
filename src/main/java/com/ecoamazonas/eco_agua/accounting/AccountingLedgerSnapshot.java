package com.ecoamazonas.eco_agua.accounting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AccountingLedgerSnapshot {

    private LocalDate startDate;
    private LocalDate endDate;
    private String statusFilter = "ALL";
    private Long accountId;
    private AccountingLedgerSummary summary = new AccountingLedgerSummary();
    private List<AccountingLedgerAccountGroup> accountGroups = new ArrayList<>();

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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public AccountingLedgerSummary getSummary() {
        return summary;
    }

    public void setSummary(AccountingLedgerSummary summary) {
        this.summary = summary == null ? new AccountingLedgerSummary() : summary;
    }

    public List<AccountingLedgerAccountGroup> getAccountGroups() {
        return accountGroups;
    }

    public void setAccountGroups(List<AccountingLedgerAccountGroup> accountGroups) {
        this.accountGroups = accountGroups == null ? new ArrayList<>() : accountGroups;
    }
}
