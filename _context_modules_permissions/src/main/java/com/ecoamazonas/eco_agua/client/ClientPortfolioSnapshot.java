package com.ecoamazonas.eco_agua.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ClientPortfolioSnapshot {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final long periodDays;
    private final int totalActiveClients;
    private final int clientsWithHistory;
    private final int clientsWithOrdersInPeriod;
    private final BigDecimal totalRevenueInPeriod;
    private final BigDecimal totalEstimatedProfitInPeriod;
    private final BigDecimal totalCreditPendingAllTime;
    private final int totalBorrowedBottlesInPeriod;
    private final List<ClientPortfolioRow> rankedClients;
    private final List<ClientPortfolioRow> topRevenueClients;
    private final List<ClientPortfolioRow> topProfitClients;
    private final List<ClientPortfolioRow> reactivationCandidates;
    private final List<ClientPortfolioRow> creditRiskClients;
    private final List<ClientPortfolioRow> dormantClients;

    public ClientPortfolioSnapshot(
            LocalDate fromDate,
            LocalDate toDate,
            long periodDays,
            int totalActiveClients,
            int clientsWithHistory,
            int clientsWithOrdersInPeriod,
            BigDecimal totalRevenueInPeriod,
            BigDecimal totalEstimatedProfitInPeriod,
            BigDecimal totalCreditPendingAllTime,
            int totalBorrowedBottlesInPeriod,
            List<ClientPortfolioRow> rankedClients,
            List<ClientPortfolioRow> topRevenueClients,
            List<ClientPortfolioRow> topProfitClients,
            List<ClientPortfolioRow> reactivationCandidates,
            List<ClientPortfolioRow> creditRiskClients,
            List<ClientPortfolioRow> dormantClients
    ) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.periodDays = periodDays;
        this.totalActiveClients = totalActiveClients;
        this.clientsWithHistory = clientsWithHistory;
        this.clientsWithOrdersInPeriod = clientsWithOrdersInPeriod;
        this.totalRevenueInPeriod = totalRevenueInPeriod;
        this.totalEstimatedProfitInPeriod = totalEstimatedProfitInPeriod;
        this.totalCreditPendingAllTime = totalCreditPendingAllTime;
        this.totalBorrowedBottlesInPeriod = totalBorrowedBottlesInPeriod;
        this.rankedClients = rankedClients;
        this.topRevenueClients = topRevenueClients;
        this.topProfitClients = topProfitClients;
        this.reactivationCandidates = reactivationCandidates;
        this.creditRiskClients = creditRiskClients;
        this.dormantClients = dormantClients;
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

    public int getTotalActiveClients() {
        return totalActiveClients;
    }

    public int getClientsWithHistory() {
        return clientsWithHistory;
    }

    public int getClientsWithOrdersInPeriod() {
        return clientsWithOrdersInPeriod;
    }

    public BigDecimal getTotalRevenueInPeriod() {
        return totalRevenueInPeriod;
    }

    public BigDecimal getTotalEstimatedProfitInPeriod() {
        return totalEstimatedProfitInPeriod;
    }

    public BigDecimal getTotalCreditPendingAllTime() {
        return totalCreditPendingAllTime;
    }

    public int getTotalBorrowedBottlesInPeriod() {
        return totalBorrowedBottlesInPeriod;
    }

    public List<ClientPortfolioRow> getRankedClients() {
        return rankedClients;
    }

    public List<ClientPortfolioRow> getTopRevenueClients() {
        return topRevenueClients;
    }

    public List<ClientPortfolioRow> getTopProfitClients() {
        return topProfitClients;
    }

    public List<ClientPortfolioRow> getReactivationCandidates() {
        return reactivationCandidates;
    }

    public List<ClientPortfolioRow> getCreditRiskClients() {
        return creditRiskClients;
    }

    public List<ClientPortfolioRow> getDormantClients() {
        return dormantClients;
    }
}
