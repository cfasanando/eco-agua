package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AccountingLedgerAccountGroup {

    private Long accountId;
    private String accountCode;
    private String accountName;
    private String accountTypeLabel;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private BigDecimal endingBalance = BigDecimal.ZERO;
    private List<AccountingLedgerRow> rows = new ArrayList<>();

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

    public BigDecimal getEndingBalance() {
        return endingBalance == null ? BigDecimal.ZERO : endingBalance;
    }

    public void setEndingBalance(BigDecimal endingBalance) {
        this.endingBalance = endingBalance == null ? BigDecimal.ZERO : endingBalance;
    }

    public List<AccountingLedgerRow> getRows() {
        return rows;
    }

    public void setRows(List<AccountingLedgerRow> rows) {
        this.rows = rows == null ? new ArrayList<>() : rows;
    }

    public void addRow(AccountingLedgerRow row) {
        this.rows.add(row);
        this.totalDebit = getTotalDebit().add(row.getDebitAmount());
        this.totalCredit = getTotalCredit().add(row.getCreditAmount());
        this.endingBalance = getEndingBalance().add(row.getDebitAmount()).subtract(row.getCreditAmount());
        row.setRunningBalance(this.endingBalance);
    }

    public String getEndingBalanceSideLabel() {
        int comparison = getEndingBalance().compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            return "Deudor";
        }
        if (comparison < 0) {
            return "Acreedor";
        }
        return "Cuadrado";
    }

    public BigDecimal getAbsoluteEndingBalance() {
        return getEndingBalance().abs();
    }
}
