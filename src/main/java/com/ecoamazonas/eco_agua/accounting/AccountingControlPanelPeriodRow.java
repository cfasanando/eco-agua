package com.ecoamazonas.eco_agua.accounting;

public class AccountingControlPanelPeriodRow {

    private final int year;
    private final int month;
    private final String label;
    private final boolean closed;
    private final int totalEntries;
    private final int draftEntries;
    private final int unbalancedEntries;

    public AccountingControlPanelPeriodRow(
            int year,
            int month,
            String label,
            boolean closed,
            int totalEntries,
            int draftEntries,
            int unbalancedEntries
    ) {
        this.year = year;
        this.month = month;
        this.label = label;
        this.closed = closed;
        this.totalEntries = totalEntries;
        this.draftEntries = draftEntries;
        this.unbalancedEntries = unbalancedEntries;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public String getLabel() {
        return label;
    }

    public boolean isClosed() {
        return closed;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public int getDraftEntries() {
        return draftEntries;
    }

    public int getUnbalancedEntries() {
        return unbalancedEntries;
    }

    public String getStatusLabel() {
        return closed ? "Cerrado" : "Abierto";
    }

    public String getStatusBadgeClass() {
        return closed ? "text-bg-success" : "text-bg-warning";
    }

    public boolean hasWarnings() {
        return draftEntries > 0 || unbalancedEntries > 0;
    }
}
