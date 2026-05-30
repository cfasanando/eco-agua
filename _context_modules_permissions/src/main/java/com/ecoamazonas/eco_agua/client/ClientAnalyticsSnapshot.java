package com.ecoamazonas.eco_agua.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ClientAnalyticsSnapshot {

    private final Client client;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final long periodDays;
    private final boolean historyAvailable;
    private final LocalDate firstCommercialOrderDate;
    private final LocalDate lastOrderDate;
    private final String lastOrderStatusLabel;
    private final long daysSinceLastOrder;
    private final int averageIntervalDays;
    private final int totalOrdersAllTime;
    private final int commercialOrdersAllTime;
    private final int totalOrdersInPeriod;
    private final int commercialOrdersInPeriod;
    private final int paidOrdersInPeriod;
    private final int creditOrdersInPeriod;
    private final int borrowedBottlesAllTime;
    private final int borrowedBottlesInPeriod;
    private final BigDecimal totalRevenueAllTime;
    private final BigDecimal estimatedProfitAllTime;
    private final BigDecimal creditPendingAllTime;
    private final BigDecimal totalRevenueInPeriod;
    private final BigDecimal paidRevenueInPeriod;
    private final BigDecimal creditRevenueInPeriod;
    private final BigDecimal estimatedCostInPeriod;
    private final BigDecimal estimatedProfitInPeriod;
    private final BigDecimal averageTicketInPeriod;
    private final BigDecimal profitMarginPercentInPeriod;
    private final int healthScore;
    private final String healthLabel;
    private final String healthBadgeClass;
    private final String recommendedAction;
    private final List<String> opportunityNotes;
    private final List<ClientAnalyticsProductRow> topProducts;
    private final List<ClientAnalyticsOrderRow> recentOrders;

    public ClientAnalyticsSnapshot(
            Client client,
            LocalDate fromDate,
            LocalDate toDate,
            long periodDays,
            boolean historyAvailable,
            LocalDate firstCommercialOrderDate,
            LocalDate lastOrderDate,
            String lastOrderStatusLabel,
            long daysSinceLastOrder,
            int averageIntervalDays,
            int totalOrdersAllTime,
            int commercialOrdersAllTime,
            int totalOrdersInPeriod,
            int commercialOrdersInPeriod,
            int paidOrdersInPeriod,
            int creditOrdersInPeriod,
            int borrowedBottlesAllTime,
            int borrowedBottlesInPeriod,
            BigDecimal totalRevenueAllTime,
            BigDecimal estimatedProfitAllTime,
            BigDecimal creditPendingAllTime,
            BigDecimal totalRevenueInPeriod,
            BigDecimal paidRevenueInPeriod,
            BigDecimal creditRevenueInPeriod,
            BigDecimal estimatedCostInPeriod,
            BigDecimal estimatedProfitInPeriod,
            BigDecimal averageTicketInPeriod,
            BigDecimal profitMarginPercentInPeriod,
            int healthScore,
            String healthLabel,
            String healthBadgeClass,
            String recommendedAction,
            List<String> opportunityNotes,
            List<ClientAnalyticsProductRow> topProducts,
            List<ClientAnalyticsOrderRow> recentOrders
    ) {
        this.client = client;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.periodDays = periodDays;
        this.historyAvailable = historyAvailable;
        this.firstCommercialOrderDate = firstCommercialOrderDate;
        this.lastOrderDate = lastOrderDate;
        this.lastOrderStatusLabel = lastOrderStatusLabel;
        this.daysSinceLastOrder = daysSinceLastOrder;
        this.averageIntervalDays = averageIntervalDays;
        this.totalOrdersAllTime = totalOrdersAllTime;
        this.commercialOrdersAllTime = commercialOrdersAllTime;
        this.totalOrdersInPeriod = totalOrdersInPeriod;
        this.commercialOrdersInPeriod = commercialOrdersInPeriod;
        this.paidOrdersInPeriod = paidOrdersInPeriod;
        this.creditOrdersInPeriod = creditOrdersInPeriod;
        this.borrowedBottlesAllTime = borrowedBottlesAllTime;
        this.borrowedBottlesInPeriod = borrowedBottlesInPeriod;
        this.totalRevenueAllTime = totalRevenueAllTime;
        this.estimatedProfitAllTime = estimatedProfitAllTime;
        this.creditPendingAllTime = creditPendingAllTime;
        this.totalRevenueInPeriod = totalRevenueInPeriod;
        this.paidRevenueInPeriod = paidRevenueInPeriod;
        this.creditRevenueInPeriod = creditRevenueInPeriod;
        this.estimatedCostInPeriod = estimatedCostInPeriod;
        this.estimatedProfitInPeriod = estimatedProfitInPeriod;
        this.averageTicketInPeriod = averageTicketInPeriod;
        this.profitMarginPercentInPeriod = profitMarginPercentInPeriod;
        this.healthScore = healthScore;
        this.healthLabel = healthLabel;
        this.healthBadgeClass = healthBadgeClass;
        this.recommendedAction = recommendedAction;
        this.opportunityNotes = opportunityNotes;
        this.topProducts = topProducts;
        this.recentOrders = recentOrders;
    }

    public Client getClient() {
        return client;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public long getPeriodDays() {
        return periodDays;
    }

    public boolean isHistoryAvailable() {
        return historyAvailable;
    }

    public LocalDate getFirstCommercialOrderDate() {
        return firstCommercialOrderDate;
    }

    public LocalDate getLastOrderDate() {
        return lastOrderDate;
    }

    public String getLastOrderStatusLabel() {
        return lastOrderStatusLabel;
    }

    public long getDaysSinceLastOrder() {
        return daysSinceLastOrder;
    }

    public int getAverageIntervalDays() {
        return averageIntervalDays;
    }

    public int getTotalOrdersAllTime() {
        return totalOrdersAllTime;
    }

    public int getCommercialOrdersAllTime() {
        return commercialOrdersAllTime;
    }

    public int getTotalOrdersInPeriod() {
        return totalOrdersInPeriod;
    }

    public int getCommercialOrdersInPeriod() {
        return commercialOrdersInPeriod;
    }

    public int getPaidOrdersInPeriod() {
        return paidOrdersInPeriod;
    }

    public int getCreditOrdersInPeriod() {
        return creditOrdersInPeriod;
    }

    public int getBorrowedBottlesAllTime() {
        return borrowedBottlesAllTime;
    }

    public int getBorrowedBottlesInPeriod() {
        return borrowedBottlesInPeriod;
    }

    public BigDecimal getTotalRevenueAllTime() {
        return totalRevenueAllTime;
    }

    public BigDecimal getEstimatedProfitAllTime() {
        return estimatedProfitAllTime;
    }

    public BigDecimal getCreditPendingAllTime() {
        return creditPendingAllTime;
    }

    public BigDecimal getTotalRevenueInPeriod() {
        return totalRevenueInPeriod;
    }

    public BigDecimal getPaidRevenueInPeriod() {
        return paidRevenueInPeriod;
    }

    public BigDecimal getCreditRevenueInPeriod() {
        return creditRevenueInPeriod;
    }

    public BigDecimal getEstimatedCostInPeriod() {
        return estimatedCostInPeriod;
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

    public List<String> getOpportunityNotes() {
        return opportunityNotes;
    }

    public List<ClientAnalyticsProductRow> getTopProducts() {
        return topProducts;
    }

    public List<ClientAnalyticsOrderRow> getRecentOrders() {
        return recentOrders;
    }
}
