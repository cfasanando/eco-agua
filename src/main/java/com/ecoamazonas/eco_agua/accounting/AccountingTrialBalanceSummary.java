package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;

public class AccountingTrialBalanceSummary {

    private int totalAccounts;
    private int totalMovements;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private BigDecimal totalDebitBalance = BigDecimal.ZERO;
    private BigDecimal totalCreditBalance = BigDecimal.ZERO;

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(int totalAccounts) {
        this.totalAccounts = Math.max(totalAccounts, 0);
    }

    public int getTotalMovements() {
        return totalMovements;
    }

    public void setTotalMovements(int totalMovements) {
        this.totalMovements = Math.max(totalMovements, 0);
    }

    public BigDecimal getTotalDebit() {
        return totalDebit == null ? BigDecimal.ZERO : totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit == null ? BigDecimal.ZERO : totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
    }

    public BigDecimal getTotalDebitBalance() {
        return totalDebitBalance == null ? BigDecimal.ZERO : totalDebitBalance;
    }

    public void setTotalDebitBalance(BigDecimal totalDebitBalance) {
        this.totalDebitBalance = totalDebitBalance == null ? BigDecimal.ZERO : totalDebitBalance;
    }

    public BigDecimal getTotalCreditBalance() {
        return totalCreditBalance == null ? BigDecimal.ZERO : totalCreditBalance;
    }

    public void setTotalCreditBalance(BigDecimal totalCreditBalance) {
        this.totalCreditBalance = totalCreditBalance == null ? BigDecimal.ZERO : totalCreditBalance;
    }

    public BigDecimal getMovementDifference() {
        return getTotalDebit().subtract(getTotalCredit()).abs();
    }

    public BigDecimal getBalanceDifference() {
        return getTotalDebitBalance().subtract(getTotalCreditBalance()).abs();
    }

    public boolean isBalanced() {
        return getMovementDifference().compareTo(BigDecimal.ZERO) == 0
                && getBalanceDifference().compareTo(BigDecimal.ZERO) == 0;
    }
}
