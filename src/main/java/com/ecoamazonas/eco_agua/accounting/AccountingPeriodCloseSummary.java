package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;

public class AccountingPeriodCloseSummary {

    private final int totalEntries;
    private final int draftEntries;
    private final int postedEntries;
    private final int cancelledEntries;
    private final int unbalancedEntries;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;
    private final BigDecimal difference;
    private final boolean readyToClose;

    public AccountingPeriodCloseSummary(
            int totalEntries,
            int draftEntries,
            int postedEntries,
            int cancelledEntries,
            int unbalancedEntries,
            BigDecimal totalDebit,
            BigDecimal totalCredit
    ) {
        this.totalEntries = totalEntries;
        this.draftEntries = draftEntries;
        this.postedEntries = postedEntries;
        this.cancelledEntries = cancelledEntries;
        this.unbalancedEntries = unbalancedEntries;
        this.totalDebit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
        this.totalCredit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
        this.difference = this.totalDebit.subtract(this.totalCredit);
        this.readyToClose = draftEntries == 0 && unbalancedEntries == 0 && this.difference.compareTo(BigDecimal.ZERO) == 0;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public int getDraftEntries() {
        return draftEntries;
    }

    public int getPostedEntries() {
        return postedEntries;
    }

    public int getCancelledEntries() {
        return cancelledEntries;
    }

    public int getUnbalancedEntries() {
        return unbalancedEntries;
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

    public BigDecimal getAbsoluteDifference() {
        return difference.abs();
    }

    public boolean isReadyToClose() {
        return readyToClose;
    }
}
