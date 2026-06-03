package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;

public class AccountingIncomeStatementRow {

    private Long accountId;
    private String accountCode;
    private String accountName;
    private String sectionCode;
    private String sectionLabel;
    private int movementCount;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private BigDecimal amount = BigDecimal.ZERO;

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

    public String getSectionCode() {
        return sectionCode;
    }

    public void setSectionCode(String sectionCode) {
        this.sectionCode = sectionCode;
    }

    public String getSectionLabel() {
        return sectionLabel;
    }

    public void setSectionLabel(String sectionLabel) {
        this.sectionLabel = sectionLabel;
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
    }

    public BigDecimal getTotalCredit() {
        return totalCredit == null ? BigDecimal.ZERO : totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
    }

    public BigDecimal getAmount() {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public void addMovement(BigDecimal debitAmount, BigDecimal creditAmount) {
        totalDebit = getTotalDebit().add(debitAmount == null ? BigDecimal.ZERO : debitAmount);
        totalCredit = getTotalCredit().add(creditAmount == null ? BigDecimal.ZERO : creditAmount);
        movementCount++;
    }
}
