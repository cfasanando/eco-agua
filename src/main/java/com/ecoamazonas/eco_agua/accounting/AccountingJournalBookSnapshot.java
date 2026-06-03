package com.ecoamazonas.eco_agua.accounting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AccountingJournalBookSnapshot {

    private LocalDate startDate;
    private LocalDate endDate;
    private String statusFilter = "ALL";
    private List<AccountingJournalEntry> entries = new ArrayList<>();
    private AccountingJournalBookSummary summary = new AccountingJournalBookSummary();

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
        this.statusFilter = statusFilter == null || statusFilter.isBlank() ? "ALL" : statusFilter;
    }

    public List<AccountingJournalEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<AccountingJournalEntry> entries) {
        this.entries = entries == null ? new ArrayList<>() : entries;
    }

    public AccountingJournalBookSummary getSummary() {
        return summary;
    }

    public void setSummary(AccountingJournalBookSummary summary) {
        this.summary = summary == null ? new AccountingJournalBookSummary() : summary;
    }
}
