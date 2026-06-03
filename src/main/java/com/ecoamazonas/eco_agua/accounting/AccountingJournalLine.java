package com.ecoamazonas.eco_agua.accounting;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounting_journal_line")
public class AccountingJournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private AccountingJournalEntry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountingAccount account;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(length = 255)
    private String description;

    @Column(name = "debit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AccountingJournalEntry getEntry() {
        return entry;
    }

    public void setEntry(AccountingJournalEntry entry) {
        this.entry = entry;
    }

    public AccountingAccount getAccount() {
        return account;
    }

    public void setAccount(AccountingAccount account) {
        this.account = account;
    }

    public int getLineOrder() {
        return lineOrder;
    }

    public void setLineOrder(int lineOrder) {
        this.lineOrder = lineOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    @Transient
    public boolean isDebitLine() {
        return getDebitAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    @Transient
    public boolean isCreditLine() {
        return getCreditAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}
