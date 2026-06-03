package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AccountingControlPanelRecentEntry {

    private final Long id;
    private final LocalDate entryDate;
    private final String description;
    private final String sourceLabel;
    private final String statusLabel;
    private final AccountingJournalEntryStatus status;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;
    private final boolean balanced;

    public AccountingControlPanelRecentEntry(AccountingJournalEntry entry) {
        this.id = entry.getId();
        this.entryDate = entry.getEntryDate();
        this.description = entry.getDescription();
        this.sourceLabel = entry.getSourceDetailLabel();
        this.status = entry.getStatus();
        this.statusLabel = entry.getStatus().getLabel();
        this.totalDebit = entry.getTotalDebit();
        this.totalCredit = entry.getTotalCredit();
        this.balanced = entry.isBalanced();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public AccountingJournalEntryStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public String getStatusBadgeClass() {
        if (AccountingJournalEntryStatus.POSTED.equals(status)) {
            return "text-bg-success";
        }
        if (AccountingJournalEntryStatus.CANCELLED.equals(status)) {
            return "text-bg-secondary";
        }
        return "text-bg-warning";
    }
}
