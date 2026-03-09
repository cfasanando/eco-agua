package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.client.ClientPortfolioRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BusinessOverviewSnapshot {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final LocalDate previousFromDate;
    private final LocalDate previousToDate;
    private final long periodDays;
    private final BigDecimal totalIncomes;
    private final BigDecimal totalExpenses;
    private final BigDecimal netResult;
    private final BigDecimal previousTotalIncomes;
    private final BigDecimal previousTotalExpenses;
    private final BigDecimal previousNetResult;
    private final BigDecimal incomeChangePercent;
    private final BigDecimal expenseChangePercent;
    private final BigDecimal netChangePercent;
    private final BigDecimal commercialSales;
    private final BigDecimal previousCommercialSales;
    private final BigDecimal commercialSalesChangePercent;
    private final BigDecimal cashCollected;
    private final BigDecimal creditGeneratedInPeriod;
    private final BigDecimal pendingDeliveryAmount;
    private final BigDecimal averageDailyCommercialSales;
    private final BigDecimal totalCreditPending;
    private final int paidOrdersCount;
    private final int creditOrdersCount;
    private final int requestedOrdersCount;
    private final int clientsWithSales;
    private final int activeClients;
    private final int reactivationCount;
    private final int dormantCount;
    private final int creditRiskCount;
    private final int lowStockCount;
    private final int totalBorrowedBottlesInPeriod;
    private final int overallScore;
    private final String overallHealthLabel;
    private final String overallHealthBadgeClass;
    private final String overallHealthSummary;
    private final List<String> keyTakeaways;
    private final List<BusinessOverviewAlert> alerts;
    private final List<BusinessOverviewProductRow> topProducts;
    private final List<BusinessOverviewExpenseCategoryRow> topExpenseCategories;
    private final List<BusinessOverviewStockRow> lowStockProducts;
    private final List<ClientPortfolioRow> topRevenueClients;
    private final List<ClientPortfolioRow> topProfitClients;
    private final List<ClientPortfolioRow> reactivationCandidates;
    private final List<ClientPortfolioRow> creditRiskClients;

    public BusinessOverviewSnapshot(
            LocalDate fromDate,
            LocalDate toDate,
            LocalDate previousFromDate,
            LocalDate previousToDate,
            long periodDays,
            BigDecimal totalIncomes,
            BigDecimal totalExpenses,
            BigDecimal netResult,
            BigDecimal previousTotalIncomes,
            BigDecimal previousTotalExpenses,
            BigDecimal previousNetResult,
            BigDecimal incomeChangePercent,
            BigDecimal expenseChangePercent,
            BigDecimal netChangePercent,
            BigDecimal commercialSales,
            BigDecimal previousCommercialSales,
            BigDecimal commercialSalesChangePercent,
            BigDecimal cashCollected,
            BigDecimal creditGeneratedInPeriod,
            BigDecimal pendingDeliveryAmount,
            BigDecimal averageDailyCommercialSales,
            BigDecimal totalCreditPending,
            int paidOrdersCount,
            int creditOrdersCount,
            int requestedOrdersCount,
            int clientsWithSales,
            int activeClients,
            int reactivationCount,
            int dormantCount,
            int creditRiskCount,
            int lowStockCount,
            int totalBorrowedBottlesInPeriod,
            int overallScore,
            String overallHealthLabel,
            String overallHealthBadgeClass,
            String overallHealthSummary,
            List<String> keyTakeaways,
            List<BusinessOverviewAlert> alerts,
            List<BusinessOverviewProductRow> topProducts,
            List<BusinessOverviewExpenseCategoryRow> topExpenseCategories,
            List<BusinessOverviewStockRow> lowStockProducts,
            List<ClientPortfolioRow> topRevenueClients,
            List<ClientPortfolioRow> topProfitClients,
            List<ClientPortfolioRow> reactivationCandidates,
            List<ClientPortfolioRow> creditRiskClients
    ) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.previousFromDate = previousFromDate;
        this.previousToDate = previousToDate;
        this.periodDays = periodDays;
        this.totalIncomes = totalIncomes;
        this.totalExpenses = totalExpenses;
        this.netResult = netResult;
        this.previousTotalIncomes = previousTotalIncomes;
        this.previousTotalExpenses = previousTotalExpenses;
        this.previousNetResult = previousNetResult;
        this.incomeChangePercent = incomeChangePercent;
        this.expenseChangePercent = expenseChangePercent;
        this.netChangePercent = netChangePercent;
        this.commercialSales = commercialSales;
        this.previousCommercialSales = previousCommercialSales;
        this.commercialSalesChangePercent = commercialSalesChangePercent;
        this.cashCollected = cashCollected;
        this.creditGeneratedInPeriod = creditGeneratedInPeriod;
        this.pendingDeliveryAmount = pendingDeliveryAmount;
        this.averageDailyCommercialSales = averageDailyCommercialSales;
        this.totalCreditPending = totalCreditPending;
        this.paidOrdersCount = paidOrdersCount;
        this.creditOrdersCount = creditOrdersCount;
        this.requestedOrdersCount = requestedOrdersCount;
        this.clientsWithSales = clientsWithSales;
        this.activeClients = activeClients;
        this.reactivationCount = reactivationCount;
        this.dormantCount = dormantCount;
        this.creditRiskCount = creditRiskCount;
        this.lowStockCount = lowStockCount;
        this.totalBorrowedBottlesInPeriod = totalBorrowedBottlesInPeriod;
        this.overallScore = overallScore;
        this.overallHealthLabel = overallHealthLabel;
        this.overallHealthBadgeClass = overallHealthBadgeClass;
        this.overallHealthSummary = overallHealthSummary;
        this.keyTakeaways = keyTakeaways;
        this.alerts = alerts;
        this.topProducts = topProducts;
        this.topExpenseCategories = topExpenseCategories;
        this.lowStockProducts = lowStockProducts;
        this.topRevenueClients = topRevenueClients;
        this.topProfitClients = topProfitClients;
        this.reactivationCandidates = reactivationCandidates;
        this.creditRiskClients = creditRiskClients;
    }

    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public LocalDate getPreviousFromDate() { return previousFromDate; }
    public LocalDate getPreviousToDate() { return previousToDate; }
    public long getPeriodDays() { return periodDays; }
    public BigDecimal getTotalIncomes() { return totalIncomes; }
    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public BigDecimal getNetResult() { return netResult; }
    public BigDecimal getPreviousTotalIncomes() { return previousTotalIncomes; }
    public BigDecimal getPreviousTotalExpenses() { return previousTotalExpenses; }
    public BigDecimal getPreviousNetResult() { return previousNetResult; }
    public BigDecimal getIncomeChangePercent() { return incomeChangePercent; }
    public BigDecimal getExpenseChangePercent() { return expenseChangePercent; }
    public BigDecimal getNetChangePercent() { return netChangePercent; }
    public BigDecimal getCommercialSales() { return commercialSales; }
    public BigDecimal getPreviousCommercialSales() { return previousCommercialSales; }
    public BigDecimal getCommercialSalesChangePercent() { return commercialSalesChangePercent; }
    public BigDecimal getCashCollected() { return cashCollected; }
    public BigDecimal getCreditGeneratedInPeriod() { return creditGeneratedInPeriod; }
    public BigDecimal getPendingDeliveryAmount() { return pendingDeliveryAmount; }
    public BigDecimal getAverageDailyCommercialSales() { return averageDailyCommercialSales; }
    public BigDecimal getTotalCreditPending() { return totalCreditPending; }
    public int getPaidOrdersCount() { return paidOrdersCount; }
    public int getCreditOrdersCount() { return creditOrdersCount; }
    public int getRequestedOrdersCount() { return requestedOrdersCount; }
    public int getClientsWithSales() { return clientsWithSales; }
    public int getActiveClients() { return activeClients; }
    public int getReactivationCount() { return reactivationCount; }
    public int getDormantCount() { return dormantCount; }
    public int getCreditRiskCount() { return creditRiskCount; }
    public int getLowStockCount() { return lowStockCount; }
    public int getTotalBorrowedBottlesInPeriod() { return totalBorrowedBottlesInPeriod; }
    public int getOverallScore() { return overallScore; }
    public String getOverallHealthLabel() { return overallHealthLabel; }
    public String getOverallHealthBadgeClass() { return overallHealthBadgeClass; }
    public String getOverallHealthSummary() { return overallHealthSummary; }
    public List<String> getKeyTakeaways() { return keyTakeaways; }
    public List<BusinessOverviewAlert> getAlerts() { return alerts; }
    public List<BusinessOverviewProductRow> getTopProducts() { return topProducts; }
    public List<BusinessOverviewExpenseCategoryRow> getTopExpenseCategories() { return topExpenseCategories; }
    public List<BusinessOverviewStockRow> getLowStockProducts() { return lowStockProducts; }
    public List<ClientPortfolioRow> getTopRevenueClients() { return topRevenueClients; }
    public List<ClientPortfolioRow> getTopProfitClients() { return topProfitClients; }
    public List<ClientPortfolioRow> getReactivationCandidates() { return reactivationCandidates; }
    public List<ClientPortfolioRow> getCreditRiskClients() { return creditRiskClients; }
}
