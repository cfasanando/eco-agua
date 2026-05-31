package com.ecoamazonas.eco_agua.order;

import java.time.LocalDate;

public class PossibleOrderSuggestion {

    private final Long clientId;
    private final String clientName;
    private final String clientPhone;
    private final String profileName;
    private final LocalDate lastOrderDate;
    private final long daysSinceLastOrder;
    private final int successfulOrderDays;
    private final int averageIntervalDays;
    private final LocalDate expectedNextOrderDate;
    private final long overdueDays;
    private final int probabilityPercent;
    private final String statusLabel;
    private final String statusClass;
    private final String actionLabel;

    public PossibleOrderSuggestion(
            Long clientId,
            String clientName,
            String clientPhone,
            String profileName,
            LocalDate lastOrderDate,
            long daysSinceLastOrder,
            int successfulOrderDays,
            int averageIntervalDays,
            LocalDate expectedNextOrderDate,
            long overdueDays,
            int probabilityPercent,
            String statusLabel,
            String statusClass,
            String actionLabel
    ) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.profileName = profileName;
        this.lastOrderDate = lastOrderDate;
        this.daysSinceLastOrder = daysSinceLastOrder;
        this.successfulOrderDays = successfulOrderDays;
        this.averageIntervalDays = averageIntervalDays;
        this.expectedNextOrderDate = expectedNextOrderDate;
        this.overdueDays = overdueDays;
        this.probabilityPercent = probabilityPercent;
        this.statusLabel = statusLabel;
        this.statusClass = statusClass;
        this.actionLabel = actionLabel;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public String getProfileName() {
        return profileName;
    }

    public LocalDate getLastOrderDate() {
        return lastOrderDate;
    }

    public long getDaysSinceLastOrder() {
        return daysSinceLastOrder;
    }

    public int getSuccessfulOrderDays() {
        return successfulOrderDays;
    }

    public int getAverageIntervalDays() {
        return averageIntervalDays;
    }

    public LocalDate getExpectedNextOrderDate() {
        return expectedNextOrderDate;
    }

    public long getOverdueDays() {
        return overdueDays;
    }

    public int getProbabilityPercent() {
        return probabilityPercent;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public String getActionLabel() {
        return actionLabel;
    }
}