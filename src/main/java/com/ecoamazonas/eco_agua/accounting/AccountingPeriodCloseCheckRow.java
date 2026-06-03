package com.ecoamazonas.eco_agua.accounting;

public class AccountingPeriodCloseCheckRow {

    private final String title;
    private final String detail;
    private final boolean ok;
    private final String actionUrl;
    private final String actionLabel;

    public AccountingPeriodCloseCheckRow(String title, String detail, boolean ok, String actionUrl, String actionLabel) {
        this.title = title;
        this.detail = detail;
        this.ok = ok;
        this.actionUrl = actionUrl;
        this.actionLabel = actionLabel;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isOk() {
        return ok;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public boolean hasAction() {
        return actionUrl != null && !actionUrl.isBlank();
    }
}
