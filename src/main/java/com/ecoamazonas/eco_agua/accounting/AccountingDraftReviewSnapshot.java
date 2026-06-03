package com.ecoamazonas.eco_agua.accounting;

import java.util.List;

public class AccountingDraftReviewSnapshot {

    private final List<AccountingDraftReviewRow> rows;
    private final AccountingDraftReviewSummary summary;

    public AccountingDraftReviewSnapshot(List<AccountingDraftReviewRow> rows) {
        this.rows = rows;
        this.summary = new AccountingDraftReviewSummary(rows);
    }

    public List<AccountingDraftReviewRow> getRows() {
        return rows;
    }

    public AccountingDraftReviewSummary getSummary() {
        return summary;
    }
}
