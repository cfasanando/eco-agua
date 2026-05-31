package com.ecoamazonas.eco_agua.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ClientPortfolioRow {

    private final Long clientId;
    private final String clientName;
    private final String profileName;
    private final String phone;
    private final LocalDate registrationDate;
    private final boolean historyAvailable;
    private final LocalDate lastOrderDate;
    private final long daysSinceLastOrder;
    private final int averageIntervalDays;
    private final long overdueDays;
    private final int commercialOrdersInPeriod;
    private final BigDecimal totalRevenueInPeriod;
    private final BigDecimal estimatedProfitInPeriod;
    private final BigDecimal averageTicketInPeriod;
    private final BigDecimal profitMarginPercentInPeriod;
    private final BigDecimal creditPendingAllTime;
    private final int borrowedBottlesInPeriod;
    private final int borrowedBottlesAllTime;
    private final int healthScore;
    private final String healthLabel;
    private final String healthBadgeClass;
    private final String recommendedAction;
    private final String strategyNote;
    private final boolean reactivationCandidate;
    private final boolean dormant;

    public ClientPortfolioRow(
            Long clientId,
            String clientName,
            String profileName,
            String phone,
            LocalDate registrationDate,
            boolean historyAvailable,
            LocalDate lastOrderDate,
            long daysSinceLastOrder,
            int averageIntervalDays,
            long overdueDays,
            int commercialOrdersInPeriod,
            BigDecimal totalRevenueInPeriod,
            BigDecimal estimatedProfitInPeriod,
            BigDecimal averageTicketInPeriod,
            BigDecimal profitMarginPercentInPeriod,
            BigDecimal creditPendingAllTime,
            int borrowedBottlesInPeriod,
            int borrowedBottlesAllTime,
            int healthScore,
            String healthLabel,
            String healthBadgeClass,
            String recommendedAction,
            String strategyNote,
            boolean reactivationCandidate,
            boolean dormant
    ) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.profileName = profileName;
        this.phone = phone;
        this.registrationDate = registrationDate;
        this.historyAvailable = historyAvailable;
        this.lastOrderDate = lastOrderDate;
        this.daysSinceLastOrder = daysSinceLastOrder;
        this.averageIntervalDays = averageIntervalDays;
        this.overdueDays = overdueDays;
        this.commercialOrdersInPeriod = commercialOrdersInPeriod;
        this.totalRevenueInPeriod = totalRevenueInPeriod;
        this.estimatedProfitInPeriod = estimatedProfitInPeriod;
        this.averageTicketInPeriod = averageTicketInPeriod;
        this.profitMarginPercentInPeriod = profitMarginPercentInPeriod;
        this.creditPendingAllTime = creditPendingAllTime;
        this.borrowedBottlesInPeriod = borrowedBottlesInPeriod;
        this.borrowedBottlesAllTime = borrowedBottlesAllTime;
        this.healthScore = healthScore;
        this.healthLabel = healthLabel;
        this.healthBadgeClass = healthBadgeClass;
        this.recommendedAction = recommendedAction;
        this.strategyNote = strategyNote;
        this.reactivationCandidate = reactivationCandidate;
        this.dormant = dormant;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public boolean isHistoryAvailable() {
        return historyAvailable;
    }

    public LocalDate getLastOrderDate() {
        return lastOrderDate;
    }

    public long getDaysSinceLastOrder() {
        return daysSinceLastOrder;
    }

    public int getAverageIntervalDays() {
        return averageIntervalDays;
    }

    public long getOverdueDays() {
        return overdueDays;
    }

    public int getCommercialOrdersInPeriod() {
        return commercialOrdersInPeriod;
    }

    public BigDecimal getTotalRevenueInPeriod() {
        return totalRevenueInPeriod;
    }

    public BigDecimal getEstimatedProfitInPeriod() {
        return estimatedProfitInPeriod;
    }

    public BigDecimal getAverageTicketInPeriod() {
        return averageTicketInPeriod;
    }

    public BigDecimal getProfitMarginPercentInPeriod() {
        return profitMarginPercentInPeriod;
    }

    public BigDecimal getCreditPendingAllTime() {
        return creditPendingAllTime;
    }

    public int getBorrowedBottlesInPeriod() {
        return borrowedBottlesInPeriod;
    }

    public int getBorrowedBottlesAllTime() {
        return borrowedBottlesAllTime;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public String getHealthLabel() {
        return healthLabel;
    }

    public String getHealthBadgeClass() {
        return healthBadgeClass;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public String getStrategyNote() {
        return strategyNote;
    }

    public boolean isReactivationCandidate() {
        return reactivationCandidate;
    }

    public boolean isDormant() {
        return dormant;
    }
}
