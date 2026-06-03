package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;

public class AccountingTrialBalanceRow {

    private Long accountId;
    private String accountCode;
    private String accountName;
    private String accountTypeLabel;
    private int movementCount;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private BigDecimal debitBalance = BigDecimal.ZERO;
    private BigDecimal creditBalance = BigDecimal.ZERO;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountTypeLabel() {
        return accountTypeLabel;
    }

    public void setAccountTypeLabel(String accountTypeLabel) {
        this.accountTypeLabel = accountTypeLabel;
    }

    public int getMovementCount() {
        return movementCount;
    }

    public void setMovementCount(int movementCount) {
        this.movementCount = Math.max(movementCount, 0);
    }

    public BigDecimal getTotalDebit() {
        return totalDebit == null ? BigDecimal.ZERO : totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
        recalculateBalance();
    }

    public BigDecimal getTotalCredit() {
        return totalCredit == null ? BigDecimal.ZERO : totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
        recalculateBalance();
    }

    public BigDecimal getDebitBalance() {
        return debitBalance == null ? BigDecimal.ZERO : debitBalance;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance == null ? BigDecimal.ZERO : creditBalance;
    }

    public void addMovement(BigDecimal debitAmount, BigDecimal creditAmount) {
        totalDebit = getTotalDebit().add(debitAmount == null ? BigDecimal.ZERO : debitAmount);
        totalCredit = getTotalCredit().add(creditAmount == null ? BigDecimal.ZERO : creditAmount);
        movementCount++;
        recalculateBalance();
    }

    public BigDecimal getNetBalance() {
        return getTotalDebit().subtract(getTotalCredit());
    }

    public BigDecimal getAbsoluteBalance() {
        return getNetBalance().abs();
    }

    public String getBalanceSideLabel() {
        int comparison = getNetBalance().compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            return "Deudor";
        }
        if (comparison < 0) {
            return "Acreedor";
        }
        return "Cuadrado";
    }

    public boolean isDebitBalanceLine() {
        return getDebitBalance().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isCreditBalanceLine() {
        return getCreditBalance().compareTo(BigDecimal.ZERO) > 0;
    }

    private void recalculateBalance() {
        BigDecimal net = getTotalDebit().subtract(getTotalCredit());
        if (net.compareTo(BigDecimal.ZERO) > 0) {
            debitBalance = net;
            creditBalance = BigDecimal.ZERO;
        } else if (net.compareTo(BigDecimal.ZERO) < 0) {
            debitBalance = BigDecimal.ZERO;
            creditBalance = net.abs();
        } else {
            debitBalance = BigDecimal.ZERO;
            creditBalance = BigDecimal.ZERO;
        }
    }
}
