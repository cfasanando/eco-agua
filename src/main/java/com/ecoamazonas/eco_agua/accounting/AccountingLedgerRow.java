package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AccountingLedgerRow {

    private Long entryId;
    private LocalDate entryDate;
    private String entryDescription;
    private String lineDescription;
    private String sourceLabel;
    private AccountingJournalEntryStatus status;
    private BigDecimal debitAmount = BigDecimal.ZERO;
    private BigDecimal creditAmount = BigDecimal.ZERO;
    private BigDecimal runningBalance = BigDecimal.ZERO;

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getEntryDescription() {
        return entryDescription;
    }

    public void setEntryDescription(String entryDescription) {
        this.entryDescription = entryDescription;
    }

    public String getLineDescription() {
        return lineDescription;
    }

    public void setLineDescription(String lineDescription) {
        this.lineDescription = lineDescription;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public AccountingJournalEntryStatus getStatus() {
        return status;
    }

    public void setStatus(AccountingJournalEntryStatus status) {
        this.status = status;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount == null ? BigDecimal.ZERO : debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount == null ? BigDecimal.ZERO : debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount == null ? BigDecimal.ZERO : creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount == null ? BigDecimal.ZERO : creditAmount;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance == null ? BigDecimal.ZERO : runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance == null ? BigDecimal.ZERO : runningBalance;
    }

    public boolean isDebitLine() {
        return getDebitAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isCreditLine() {
        return getCreditAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    public String getBalanceSideLabel() {
        int comparison = getRunningBalance().compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            return "Deudor";
        }
        if (comparison < 0) {
            return "Acreedor";
        }
        return "Cuadrado";
    }

    public BigDecimal getAbsoluteRunningBalance() {
        return getRunningBalance().abs();
    }

    public String getStatusLabel() {
        return status != null ? status.getLabel() : "Sin estado";
    }
}
