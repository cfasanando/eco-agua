package com.ecoamazonas.eco_agua.reorder;

import java.time.LocalDate;

public class ReorderAgendaRow {

    private final Long clientId;
    private final String clientName;
    private final String phone;
    private final String profileName;
    private final LocalDate lastOrderDate;
    private final long daysSinceLastOrder;
    private final int purchaseDayCount;
    private final int averageIntervalDays;
    private final LocalDate expectedNextOrderDate;
    private final long overdueDays;
    private final int probabilityPercent;
    private final String statusLabel;
    private final String statusClass;
    private final String actionLabel;
    private final ReorderFollowUpStatus followUpStatus;
    private final LocalDate nextContactDate;
    private final String followUpObservation;

    public ReorderAgendaRow(
            Long clientId,
            String clientName,
            String phone,
            String profileName,
            LocalDate lastOrderDate,
            long daysSinceLastOrder,
            int purchaseDayCount,
            int averageIntervalDays,
            LocalDate expectedNextOrderDate,
            long overdueDays,
            int probabilityPercent,
            String statusLabel,
            String statusClass,
            String actionLabel,
            ReorderFollowUpStatus followUpStatus,
            LocalDate nextContactDate,
            String followUpObservation
    ) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.phone = phone;
        this.profileName = profileName;
        this.lastOrderDate = lastOrderDate;
        this.daysSinceLastOrder = daysSinceLastOrder;
        this.purchaseDayCount = purchaseDayCount;
        this.averageIntervalDays = averageIntervalDays;
        this.expectedNextOrderDate = expectedNextOrderDate;
        this.overdueDays = overdueDays;
        this.probabilityPercent = probabilityPercent;
        this.statusLabel = statusLabel;
        this.statusClass = statusClass;
        this.actionLabel = actionLabel;
        this.followUpStatus = followUpStatus;
        this.nextContactDate = nextContactDate;
        this.followUpObservation = followUpObservation;
    }

    public Long getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getPhone() { return phone; }
    public String getProfileName() { return profileName; }
    public LocalDate getLastOrderDate() { return lastOrderDate; }
    public long getDaysSinceLastOrder() { return daysSinceLastOrder; }
    public int getPurchaseDayCount() { return purchaseDayCount; }
    public int getAverageIntervalDays() { return averageIntervalDays; }
    public LocalDate getExpectedNextOrderDate() { return expectedNextOrderDate; }
    public long getOverdueDays() { return overdueDays; }
    public int getProbabilityPercent() { return probabilityPercent; }
    public String getStatusLabel() { return statusLabel; }
    public String getStatusClass() { return statusClass; }
    public String getActionLabel() { return actionLabel; }
    public ReorderFollowUpStatus getFollowUpStatus() { return followUpStatus; }
    public LocalDate getNextContactDate() { return nextContactDate; }
    public String getFollowUpObservation() { return followUpObservation; }
}
