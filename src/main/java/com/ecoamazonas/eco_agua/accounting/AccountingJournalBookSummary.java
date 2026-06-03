package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;

public class AccountingJournalBookSummary {

    private int totalEntries;
    private int totalLines;
    private int draftEntries;
    private int postedEntries;
    private int cancelledEntries;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public int getDraftEntries() {
        return draftEntries;
    }

    public void setDraftEntries(int draftEntries) {
        this.draftEntries = draftEntries;
    }

    public int getPostedEntries() {
        return postedEntries;
    }

    public void setPostedEntries(int postedEntries) {
        this.postedEntries = postedEntries;
    }

    public int getCancelledEntries() {
        return cancelledEntries;
    }

    public void setCancelledEntries(int cancelledEntries) {
        this.cancelledEntries = cancelledEntries;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
    }

    public BigDecimal getDifference() {
        return totalDebit.subtract(totalCredit).abs();
    }

    public boolean isBalanced() {
        return totalDebit.compareTo(totalCredit) == 0;
    }
}
