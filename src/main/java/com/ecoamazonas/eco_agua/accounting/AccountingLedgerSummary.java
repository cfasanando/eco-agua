package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;

public class AccountingLedgerSummary {

    private int totalAccounts;
    private int totalMovements;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private BigDecimal difference = BigDecimal.ZERO;

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(int totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public int getTotalMovements() {
        return totalMovements;
    }

    public void setTotalMovements(int totalMovements) {
        this.totalMovements = totalMovements;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit == null ? BigDecimal.ZERO : totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
        refreshDifference();
    }

    public BigDecimal getTotalCredit() {
        return totalCredit == null ? BigDecimal.ZERO : totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
        refreshDifference();
    }

    public BigDecimal getDifference() {
        return difference == null ? BigDecimal.ZERO : difference;
    }

    public boolean isBalanced() {
        return getDifference().compareTo(BigDecimal.ZERO) == 0;
    }

    private void refreshDifference() {
        this.difference = getTotalDebit().subtract(getTotalCredit()).abs();
    }
}
