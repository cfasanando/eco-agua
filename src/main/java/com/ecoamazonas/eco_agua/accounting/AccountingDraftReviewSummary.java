package com.ecoamazonas.eco_agua.accounting;

import java.math.BigDecimal;
import java.util.List;

public class AccountingDraftReviewSummary {

    private final int totalDrafts;
    private final int balancedDrafts;
    private final int unbalancedDrafts;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;
    private final BigDecimal difference;

    public AccountingDraftReviewSummary(List<AccountingDraftReviewRow> rows) {
        this.totalDrafts = rows == null ? 0 : rows.size();
        this.balancedDrafts = rows == null ? 0 : (int) rows.stream().filter(AccountingDraftReviewRow::isBalanced).count();
        this.unbalancedDrafts = totalDrafts - balancedDrafts;
        this.totalDebit = rows == null ? BigDecimal.ZERO : rows.stream()
                .map(AccountingDraftReviewRow::getTotalDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalCredit = rows == null ? BigDecimal.ZERO : rows.stream()
                .map(AccountingDraftReviewRow::getTotalCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.difference = totalDebit.subtract(totalCredit).abs();
    }

    public int getTotalDrafts() {
        return totalDrafts;
    }

    public int getBalancedDrafts() {
        return balancedDrafts;
    }

    public int getUnbalancedDrafts() {
        return unbalancedDrafts;
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

    public boolean isBalanced() {
        return difference.compareTo(BigDecimal.ZERO) == 0;
    }
}
