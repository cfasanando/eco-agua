package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AccountingDraftReviewRow {

    private final Long id;
    private final LocalDate entryDate;
    private final String description;
    private final String sourceLabel;
    private final String referenceLabel;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;
    private final BigDecimal difference;
    private final int lineCount;
    private final boolean balanced;

    public AccountingDraftReviewRow(AccountingJournalEntry entry) {
        this.id = entry.getId();
        this.entryDate = entry.getEntryDate();
        this.description = entry.getDescription();
        this.sourceLabel = entry.getSourceEvent() != null ? entry.getSourceEvent().getLabel() : entry.getSourceType().getLabel();
        this.referenceLabel = buildReferenceLabel(entry);
        this.totalDebit = entry.getTotalDebit();
        this.totalCredit = entry.getTotalCredit();
        this.difference = totalDebit.subtract(totalCredit).abs();
        this.lineCount = entry.getLines() == null ? 0 : entry.getLines().size();
        this.balanced = entry.isBalanced();
    }

    private String buildReferenceLabel(AccountingJournalEntry entry) {
        if (entry.getSourceReferenceCode() != null && !entry.getSourceReferenceCode().isBlank()) {
            return entry.getSourceReferenceCode();
        }
        if (entry.getSourceReferenceId() != null) {
            return "Ref. #" + entry.getSourceReferenceId();
        }
        return "Sin referencia";
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

    public String getReferenceLabel() {
        return referenceLabel;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public int getLineCount() {
        return lineCount;
    }

    public boolean isBalanced() {
        return balanced;
    }
}
