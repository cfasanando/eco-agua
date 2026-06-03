package com.ecoamazonas.eco_agua.accounting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AccountingPeriodCloseSnapshot {

    private final int year;
    private final int month;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final AccountingPeriodCloseStatus status;
    private final LocalDateTime closedAt;
    private final LocalDateTime reopenedAt;
    private final String notes;
    private final AccountingPeriodCloseSummary summary;
    private final List<AccountingPeriodCloseCheckRow> checks;

    public AccountingPeriodCloseSnapshot(
            int year,
            int month,
            LocalDate startDate,
            LocalDate endDate,
            AccountingPeriodCloseStatus status,
            LocalDateTime closedAt,
            LocalDateTime reopenedAt,
            String notes,
            AccountingPeriodCloseSummary summary,
            List<AccountingPeriodCloseCheckRow> checks
    ) {
        this.year = year;
        this.month = month;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.closedAt = closedAt;
        this.reopenedAt = reopenedAt;
        this.notes = notes;
        this.summary = summary;
        this.checks = checks;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public AccountingPeriodCloseStatus getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return status.getLabel();
    }

    public boolean isClosed() {
        return AccountingPeriodCloseStatus.CLOSED.equals(status);
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public LocalDateTime getReopenedAt() {
        return reopenedAt;
    }

    public String getNotes() {
        return notes;
    }

    public AccountingPeriodCloseSummary getSummary() {
        return summary;
    }

    public List<AccountingPeriodCloseCheckRow> getChecks() {
        return checks;
    }

    public boolean isReadyToClose() {
        return summary.isReadyToClose() && !isClosed();
    }
}
