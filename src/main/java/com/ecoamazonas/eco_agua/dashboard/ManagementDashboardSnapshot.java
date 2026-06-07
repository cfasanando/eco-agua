package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.expense.AccountsPayableSummary;
import com.ecoamazonas.eco_agua.product.cost.ProductProfitabilityRow;
import com.ecoamazonas.eco_agua.product.cost.ProductProfitabilitySnapshot;
import com.ecoamazonas.eco_agua.product.cost.SalesChannelProfitabilityRow;
import com.ecoamazonas.eco_agua.product.cost.SalesChannelProfitabilitySnapshot;
import com.ecoamazonas.eco_agua.production.ProductionOrder;
import com.ecoamazonas.eco_agua.production.ProductionScheduleSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ManagementDashboardSnapshot {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BusinessOverviewSnapshot businessOverview;
    private final ProductProfitabilitySnapshot productProfitability;
    private final SalesChannelProfitabilitySnapshot channelProfitability;
    private final ProductionScheduleSnapshot productionSchedule;
    private final AccountsPayableSummary accountsPayableSummary;
    private final int accountingDraftCount;
    private final List<ManagementDashboardAlert> alerts;
    private final List<String> sectionErrors;

    public ManagementDashboardSnapshot(
            LocalDate startDate,
            LocalDate endDate,
            BusinessOverviewSnapshot businessOverview,
            ProductProfitabilitySnapshot productProfitability,
            SalesChannelProfitabilitySnapshot channelProfitability,
            ProductionScheduleSnapshot productionSchedule,
            AccountsPayableSummary accountsPayableSummary,
            int accountingDraftCount,
            List<ManagementDashboardAlert> alerts,
            List<String> sectionErrors
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.businessOverview = businessOverview;
        this.productProfitability = productProfitability;
        this.channelProfitability = channelProfitability;
        this.productionSchedule = productionSchedule;
        this.accountsPayableSummary = accountsPayableSummary;
        this.accountingDraftCount = accountingDraftCount;
        this.alerts = alerts != null ? alerts : List.of();
        this.sectionErrors = sectionErrors != null ? sectionErrors : List.of();
    }

    public static ManagementDashboardSnapshot empty(LocalDate startDate, LocalDate endDate, String errorMessage) {
        List<String> errors = errorMessage == null || errorMessage.isBlank() ? List.of() : List.of(errorMessage);
        List<ManagementDashboardAlert> alerts = List.of(new ManagementDashboardAlert(
                "warning",
                "No se pudo cargar el dashboard completo",
                errorMessage != null && !errorMessage.isBlank() ? errorMessage : "Revisa los módulos relacionados por separado.",
                null,
                null
        ));
        return new ManagementDashboardSnapshot(startDate, endDate, null, null, null, null, null, 0, alerts, errors);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BusinessOverviewSnapshot getBusinessOverview() {
        return businessOverview;
    }

    public ProductProfitabilitySnapshot getProductProfitability() {
        return productProfitability;
    }

    public SalesChannelProfitabilitySnapshot getChannelProfitability() {
        return channelProfitability;
    }

    public ProductionScheduleSnapshot getProductionSchedule() {
        return productionSchedule;
    }

    public AccountsPayableSummary getAccountsPayableSummary() {
        return accountsPayableSummary;
    }

    public int getAccountingDraftCount() {
        return accountingDraftCount;
    }

    public List<ManagementDashboardAlert> getAlerts() {
        return alerts;
    }

    public int getAlertCount() {
        return alerts.size();
    }

    public List<String> getSectionErrors() {
        return sectionErrors;
    }

    public boolean hasSectionErrors() {
        return !sectionErrors.isEmpty();
    }

    public BigDecimal getCommercialSales() {
        return businessOverview != null ? valueOrZero(businessOverview.getCommercialSales()) : ZERO;
    }

    public BigDecimal getTotalIncomes() {
        return businessOverview != null ? valueOrZero(businessOverview.getTotalIncomes()) : ZERO;
    }

    public BigDecimal getNetResult() {
        return businessOverview != null ? valueOrZero(businessOverview.getNetResult()) : ZERO;
    }

    public BigDecimal getTotalCreditPending() {
        return businessOverview != null ? valueOrZero(businessOverview.getTotalCreditPending()) : ZERO;
    }

    public int getPaidOrdersCount() {
        return businessOverview != null ? businessOverview.getPaidOrdersCount() : 0;
    }

    public int getCreditOrdersCount() {
        return businessOverview != null ? businessOverview.getCreditOrdersCount() : 0;
    }

    public int getLowStockCount() {
        return businessOverview != null ? businessOverview.getLowStockCount() : 0;
    }

    public List<BusinessOverviewStockRow> getLowStockProducts() {
        return businessOverview != null && businessOverview.getLowStockProducts() != null
                ? businessOverview.getLowStockProducts()
                : List.of();
    }

    public BigDecimal getEstimatedGrossProfit() {
        if (productProfitability == null || productProfitability.getSummary() == null) {
            return ZERO;
        }
        return valueOrZero(productProfitability.getSummary().getTotalGrossProfit());
    }

    public BigDecimal getEstimatedGrossMarginPercent() {
        if (productProfitability == null || productProfitability.getSummary() == null) {
            return ZERO;
        }
        return valueOrZero(productProfitability.getSummary().getAverageMarginPercent());
    }

    public List<ProductProfitabilityRow> getProductRows() {
        return productProfitability != null && productProfitability.getRows() != null
                ? productProfitability.getRows()
                : List.of();
    }

    public int getActiveChannelCount() {
        if (channelProfitability == null || channelProfitability.getSummary() == null) {
            return 0;
        }
        return channelProfitability.getSummary().getActiveChannelCount();
    }

    public int getChannelOrderCount() {
        if (channelProfitability == null || channelProfitability.getSummary() == null) {
            return 0;
        }
        return channelProfitability.getSummary().getTotalOrderCount();
    }

    public List<SalesChannelProfitabilityRow> getChannelRows() {
        return channelProfitability != null && channelProfitability.getRows() != null
                ? channelProfitability.getRows()
                : List.of();
    }

    public BigDecimal getAccountsPayableAmount() {
        if (accountsPayableSummary == null) {
            return ZERO;
        }
        return valueOrZero(accountsPayableSummary.getTotalPendingAmount());
    }

    public int getOverdueAccountsPayableCount() {
        return accountsPayableSummary != null ? accountsPayableSummary.getOverdueDebtCount() : 0;
    }

    public long getProductionDraftOrders() {
        if (productionSchedule == null || productionSchedule.getSummary() == null) {
            return 0;
        }
        return productionSchedule.getSummary().getDraftOrders();
    }

    public long getPendingQualityOrders() {
        if (productionSchedule == null || productionSchedule.getSummary() == null) {
            return 0;
        }
        return productionSchedule.getSummary().getPendingQualityOrders();
    }

    public List<ProductionOrder> getPendingQualityRows() {
        return productionSchedule != null && productionSchedule.getPendingQualityRows() != null
                ? productionSchedule.getPendingQualityRows()
                : List.of();
    }

    public boolean hasOperationalWarnings() {
        return getAlertCount() > 0
                || accountingDraftCount > 0
                || getAccountsPayableAmount().compareTo(ZERO) > 0
                || hasSectionErrors();
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : ZERO;
    }
}
