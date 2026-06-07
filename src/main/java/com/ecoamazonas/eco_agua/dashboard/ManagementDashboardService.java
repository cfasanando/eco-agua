package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.accounting.AccountingJournalEntryStatus;
import com.ecoamazonas.eco_agua.accounting.repository.AccountingJournalEntryRepository;
import com.ecoamazonas.eco_agua.expense.AccountsPayableSummary;
import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseService;
import com.ecoamazonas.eco_agua.product.cost.ProductProfitabilitySnapshot;
import com.ecoamazonas.eco_agua.product.cost.ProductProfitabilityService;
import com.ecoamazonas.eco_agua.product.cost.ProductProfitabilitySummary;
import com.ecoamazonas.eco_agua.product.cost.SalesChannelProfitabilitySnapshot;
import com.ecoamazonas.eco_agua.product.cost.SalesChannelProfitabilityService;
import com.ecoamazonas.eco_agua.product.cost.SalesChannelProfitabilitySummary;
import com.ecoamazonas.eco_agua.production.ProductionScheduleSnapshot;
import com.ecoamazonas.eco_agua.production.ProductionScheduleSummary;
import com.ecoamazonas.eco_agua.production.ProductionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class ManagementDashboardService {

    private static final Logger log = LoggerFactory.getLogger(ManagementDashboardService.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final BusinessOverviewService businessOverviewService;
    private final ProductProfitabilityService productProfitabilityService;
    private final SalesChannelProfitabilityService salesChannelProfitabilityService;
    private final ProductionService productionService;
    private final ExpenseService expenseService;
    private final AccountingJournalEntryRepository accountingJournalEntryRepository;

    public ManagementDashboardService(
            BusinessOverviewService businessOverviewService,
            ProductProfitabilityService productProfitabilityService,
            SalesChannelProfitabilityService salesChannelProfitabilityService,
            ProductionService productionService,
            ExpenseService expenseService,
            AccountingJournalEntryRepository accountingJournalEntryRepository
    ) {
        this.businessOverviewService = businessOverviewService;
        this.productProfitabilityService = productProfitabilityService;
        this.salesChannelProfitabilityService = salesChannelProfitabilityService;
        this.productionService = productionService;
        this.expenseService = expenseService;
        this.accountingJournalEntryRepository = accountingJournalEntryRepository;
    }

    public ManagementDashboardSnapshot buildSnapshot(LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.withDayOfMonth(1);

        if (effectiveStart.isAfter(effectiveEnd)) {
            LocalDate tmp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = tmp;
        }

        List<String> sectionErrors = new ArrayList<>();

        LocalDate finalEffectiveStart = effectiveStart;
        LocalDate finalEffectiveEnd = effectiveEnd;

        BusinessOverviewSnapshot businessOverview = readSection(
                "Estado del negocio",
                () -> businessOverviewService.buildSnapshot(finalEffectiveStart, finalEffectiveEnd),
                sectionErrors
        );

        ProductProfitabilitySnapshot productProfitability = readSection(
                "Rentabilidad por producto",
                () -> productProfitabilityService.buildSnapshot(finalEffectiveStart, finalEffectiveEnd),
                sectionErrors
        );

        SalesChannelProfitabilitySnapshot channelProfitability = readSection(
                "Rentabilidad por canal",
                () -> salesChannelProfitabilityService.buildSnapshot(finalEffectiveStart, finalEffectiveEnd),
                sectionErrors
        );

        ProductionScheduleSnapshot productionSchedule = readSection(
                "Agenda de producción",
                () -> productionService.buildSchedule(
                        LocalDate.now().minusDays(7),
                        LocalDate.now().plusDays(14),
                        null,
                        null,
                        null
                ),
                sectionErrors
        );

        AccountsPayableSummary accountsPayableSummary = readSection(
                "Cuentas por pagar",
                () -> {
                    List<Expense> openDebts = expenseService.findOpenDebts(LocalDate.of(1970, 1, 1), finalEffectiveEnd.plusYears(2));
                    return expenseService.buildAccountsPayableSummary(openDebts, LocalDate.now());
                },
                sectionErrors
        );

        Integer accountingDraftCountValue = readSection(
                "Borradores contables",
                () -> accountingJournalEntryRepository
                        .findByStatusAndSourceEventIsNotNullOrderByEntryDateDescIdDesc(AccountingJournalEntryStatus.DRAFT)
                        .size(),
                sectionErrors
        );
        int accountingDraftCount = accountingDraftCountValue != null ? accountingDraftCountValue : 0;

        List<ManagementDashboardAlert> alerts = buildAlerts(
                businessOverview,
                productProfitability,
                channelProfitability,
                productionSchedule,
                accountsPayableSummary,
                accountingDraftCount,
                sectionErrors
        );

        return new ManagementDashboardSnapshot(
                effectiveStart,
                effectiveEnd,
                businessOverview,
                productProfitability,
                channelProfitability,
                productionSchedule,
                accountsPayableSummary,
                accountingDraftCount,
                alerts,
                sectionErrors
        );
    }

    private <T> T readSection(String sectionName, Supplier<T> supplier, List<String> sectionErrors) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            log.warn("Management dashboard section failed: {}", sectionName, exception);
            sectionErrors.add(sectionName);
            return null;
        }
    }

    private List<ManagementDashboardAlert> buildAlerts(
            BusinessOverviewSnapshot businessOverview,
            ProductProfitabilitySnapshot productProfitability,
            SalesChannelProfitabilitySnapshot channelProfitability,
            ProductionScheduleSnapshot productionSchedule,
            AccountsPayableSummary accountsPayableSummary,
            int accountingDraftCount,
            List<String> sectionErrors
    ) {
        List<ManagementDashboardAlert> alerts = new ArrayList<>();

        if (sectionErrors != null && !sectionErrors.isEmpty()) {
            alerts.add(new ManagementDashboardAlert(
                    "warning",
                    "El dashboard cargó con datos parciales",
                    "No se pudo leer: " + String.join(", ", sectionErrors) + ". Revisa esas pantallas por separado.",
                    null,
                    null
            ));
        }

        ProductProfitabilitySummary productSummary = productProfitability != null ? productProfitability.getSummary() : null;
        if (productSummary != null && valueOrZero(productSummary.getTotalGrossProfit()).compareTo(ZERO) < 0) {
            alerts.add(new ManagementDashboardAlert(
                    "danger",
                    "La utilidad bruta estimada es negativa",
                    "El periodo analizado muestra costo variable mayor que los ingresos de venta.",
                    "Ver rentabilidad",
                    "/admin/products/profitability"
            ));
        }

        if (productSummary != null && productSummary.getTopSellingLowMarginProduct() != null) {
            alerts.add(new ManagementDashboardAlert(
                    "warning",
                    "Producto muy vendido con margen bajo",
                    productSummary.getTopSellingLowMarginProduct().getProductName() + " vende bien, pero su margen necesita revisión.",
                    "Simular precio",
                    "/admin/products/price-simulator?productId=" + productSummary.getTopSellingLowMarginProduct().getProductId()
            ));
        }

        SalesChannelProfitabilitySummary channelSummary = channelProfitability != null ? channelProfitability.getSummary() : null;
        if (channelSummary != null && channelSummary.getTopSellingLowMarginChannel() != null) {
            alerts.add(new ManagementDashboardAlert(
                    "warning",
                    "Canal con movimiento y margen bajo",
                    channelSummary.getTopSellingLowMarginChannel().getChannelLabel() + " concentra ventas con margen bajo.",
                    "Ver canales",
                    "/admin/products/channel-profitability"
            ));
        }

        if (businessOverview != null && businessOverview.getLowStockCount() > 0) {
            alerts.add(new ManagementDashboardAlert(
                    "warning",
                    "Hay productos con stock crítico",
                    businessOverview.getLowStockCount() + " producto(s) requieren reposición o revisión de inventario.",
                    "Ver stock",
                    "/warehouse/products-stock"
            ));
        }

        if (accountsPayableSummary != null && accountsPayableSummary.getOverdueDebtCount() > 0) {
            alerts.add(new ManagementDashboardAlert(
                    "danger",
                    "Hay cuentas por pagar vencidas",
                    accountsPayableSummary.getOverdueDebtCount() + " deuda(s) vencidas con proveedores o gastos pendientes.",
                    "Ver pagos",
                    "/expenses/debts"
            ));
        }

        ProductionScheduleSummary productionSummary = productionSchedule != null ? productionSchedule.getSummary() : null;
        if (productionSummary != null && productionSummary.getPendingQualityOrders() > 0) {
            alerts.add(new ManagementDashboardAlert(
                    "warning",
                    "Hay controles de calidad pendientes",
                    productionSummary.getPendingQualityOrders() + " producción(es) necesitan revisión de calidad.",
                    "Ver calidad",
                    "/production/quality"
            ));
        }

        if (productionSummary != null && productionSummary.getOverdueDraftOrders() > 0) {
            alerts.add(new ManagementDashboardAlert(
                    "warning",
                    "Hay borradores de producción atrasados",
                    productionSummary.getOverdueDraftOrders() + " borrador(es) tienen fecha anterior a hoy.",
                    "Ver agenda",
                    "/production/schedule?status=DRAFT"
            ));
        }

        if (accountingDraftCount > 0) {
            alerts.add(new ManagementDashboardAlert(
                    "info",
                    "Hay borradores contables por revisar",
                    accountingDraftCount + " asiento(s) automáticos o internos están pendientes de revisión.",
                    "Revisar borradores",
                    "/accounting/draft-review"
            ));
        }

        if (alerts.isEmpty()) {
            alerts.add(new ManagementDashboardAlert(
                    "success",
                    "Sin alertas críticas en este resumen",
                    "Los indicadores principales no muestran problemas urgentes para el periodo seleccionado.",
                    null,
                    null
            ));
        }

        return alerts;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : ZERO;
    }
}
