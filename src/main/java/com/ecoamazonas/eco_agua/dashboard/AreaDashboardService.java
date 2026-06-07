package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.accounting.AccountingControlPanelSnapshot;
import com.ecoamazonas.eco_agua.accounting.AccountingControlPanelSummary;
import com.ecoamazonas.eco_agua.accounting.service.AccountingControlPanelService;
import com.ecoamazonas.eco_agua.expense.AccountsPayableSummary;
import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseService;
import com.ecoamazonas.eco_agua.marketing.MarketingCampaignsService;
import com.ecoamazonas.eco_agua.marketing.MarketingCampaignsSnapshot;
import com.ecoamazonas.eco_agua.order.ReceivableService;
import com.ecoamazonas.eco_agua.order.ReceivableSummary;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.product.cost.ProductProfitabilityService;
import com.ecoamazonas.eco_agua.product.cost.ProductProfitabilitySnapshot;
import com.ecoamazonas.eco_agua.product.cost.ProductProfitabilitySummary;
import com.ecoamazonas.eco_agua.product.cost.SalesChannelProfitabilityService;
import com.ecoamazonas.eco_agua.product.cost.SalesChannelProfitabilitySnapshot;
import com.ecoamazonas.eco_agua.product.cost.SalesChannelProfitabilitySummary;
import com.ecoamazonas.eco_agua.production.ProductionScheduleSnapshot;
import com.ecoamazonas.eco_agua.production.ProductionScheduleSummary;
import com.ecoamazonas.eco_agua.production.ProductionService;
import com.ecoamazonas.eco_agua.warehouse.ProductReorderSuggestionService;
import com.ecoamazonas.eco_agua.warehouse.ProductReorderSuggestionSnapshot;
import com.ecoamazonas.eco_agua.warehouse.ProductReorderSuggestionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class AreaDashboardService {

    private static final Logger log = LoggerFactory.getLogger(AreaDashboardService.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final BusinessOverviewService businessOverviewService;
    private final MarketingCampaignsService marketingCampaignsService;
    private final ProductProfitabilityService productProfitabilityService;
    private final SalesChannelProfitabilityService salesChannelProfitabilityService;
    private final ProductReorderSuggestionService productReorderSuggestionService;
    private final ReceivableService receivableService;
    private final ProductionService productionService;
    private final ExpenseService expenseService;
    private final AccountingControlPanelService accountingControlPanelService;

    public AreaDashboardService(
            BusinessOverviewService businessOverviewService,
            MarketingCampaignsService marketingCampaignsService,
            ProductProfitabilityService productProfitabilityService,
            SalesChannelProfitabilityService salesChannelProfitabilityService,
            ProductReorderSuggestionService productReorderSuggestionService,
            ReceivableService receivableService,
            ProductionService productionService,
            ExpenseService expenseService,
            AccountingControlPanelService accountingControlPanelService
    ) {
        this.businessOverviewService = businessOverviewService;
        this.marketingCampaignsService = marketingCampaignsService;
        this.productProfitabilityService = productProfitabilityService;
        this.salesChannelProfitabilityService = salesChannelProfitabilityService;
        this.productReorderSuggestionService = productReorderSuggestionService;
        this.receivableService = receivableService;
        this.productionService = productionService;
        this.expenseService = expenseService;
        this.accountingControlPanelService = accountingControlPanelService;
    }

    public AreaDashboardSnapshot buildSnapshot(LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.withDayOfMonth(1);

        if (effectiveStart.isAfter(effectiveEnd)) {
            LocalDate temp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = temp;
        }

        List<String> errors = new ArrayList<>();
        List<AreaDashboardSection> sections = new ArrayList<>();

        LocalDate finalStart = effectiveStart;
        LocalDate finalEnd = effectiveEnd;

        BusinessOverviewSnapshot businessOverview = readSection(
                "Estado del negocio",
                () -> businessOverviewService.buildSnapshot(finalStart, finalEnd),
                errors
        );
        ProductProfitabilitySnapshot productProfitability = readSection(
                "Rentabilidad por producto",
                () -> productProfitabilityService.buildSnapshot(finalStart, finalEnd),
                errors
        );
        SalesChannelProfitabilitySnapshot channelProfitability = readSection(
                "Rentabilidad por canal",
                () -> salesChannelProfitabilityService.buildSnapshot(finalStart, finalEnd),
                errors
        );
        MarketingCampaignsSnapshot marketing = readSection(
                "Marketing",
                marketingCampaignsService::buildSnapshot,
                errors
        );
        ProductReorderSuggestionSnapshot reorder = readSection(
                "Reposición de productos",
                () -> productReorderSuggestionService.buildSuggestions("ALL_CONFIGURED"),
                errors
        );
        ReceivableSummary receivableSummary = readSection(
                "Cuentas por cobrar",
                () -> {
                    List<SaleOrder> orders = receivableService.findOpenCreditOrders(LocalDate.of(1970, 1, 1), finalEnd.plusYears(2));
                    return receivableService.buildSummary(orders);
                },
                errors
        );
        AccountsPayableSummary accountsPayableSummary = readSection(
                "Cuentas por pagar",
                () -> {
                    List<Expense> debts = expenseService.findOpenDebts(LocalDate.of(1970, 1, 1), finalEnd.plusYears(2));
                    return expenseService.buildAccountsPayableSummary(debts, LocalDate.now());
                },
                errors
        );
        ProductionScheduleSnapshot productionSchedule = readSection(
                "Agenda de producción",
                () -> productionService.buildSchedule(finalStart, finalEnd.plusDays(7), null, null, null),
                errors
        );
        AccountingControlPanelSnapshot accounting = readSection(
                "Control contable",
                accountingControlPanelService::buildSnapshot,
                errors
        );

        sections.add(buildMarketingSection(marketing));
        sections.add(buildSalesSection(businessOverview, channelProfitability, receivableSummary));
        sections.add(buildLogisticsSection(businessOverview, reorder, productionSchedule));
        sections.add(buildFinanceSection(businessOverview, receivableSummary, accountsPayableSummary, productProfitability));
        sections.add(buildAccountingSection(accounting));

        return new AreaDashboardSnapshot(effectiveStart, effectiveEnd, sections, errors);
    }

    private AreaDashboardSection buildMarketingSection(MarketingCampaignsSnapshot marketing) {
        int activePromotions = marketing != null ? marketing.getActivePromotionCount() : 0;
        int draftPosts = marketing != null ? marketing.getDraftPostCount() : 0;
        int pendingTasks = marketing != null && marketing.getPendingTasks() != null ? marketing.getPendingTasks().size() : 0;
        int campaigns = marketing != null && marketing.getCampaignRows() != null ? marketing.getCampaignRows().size() : 0;

        List<String> highlights = new ArrayList<>();
        if (pendingTasks > 0) {
            highlights.add("Hay acciones de marketing pendientes para revisar o ejecutar.");
        }
        if (draftPosts > 0) {
            highlights.add("Hay publicaciones en borrador que pueden convertirse en contenido activo.");
        }
        if (highlights.isEmpty()) {
            highlights.add("Marketing no muestra pendientes críticos en este resumen.");
        }

        return new AreaDashboardSection(
                "marketing",
                "Marketing",
                "Campañas, publicaciones, promociones y pendientes de contenido.",
                "bi-megaphone",
                pendingTasks > 0 ? "Pendiente" : "Estable",
                pendingTasks > 0 ? "warning" : "success",
                "Abrir marketing",
                "/marketing/admin/campaigns",
                List.of(
                        metric("Campañas", number(campaigns), "Planificadas o registradas", "primary", "Calendario", "/marketing/admin/campaigns"),
                        metric("Promociones activas", number(activePromotions), "Promociones vigentes", "success", "Promociones", "/admin/promotions"),
                        metric("Publicaciones en borrador", number(draftPosts), "Contenido pendiente", draftPosts > 0 ? "warning" : "secondary", "Plan", "/marketing/admin/publication-plan"),
                        metric("Tareas pendientes", number(pendingTasks), "Siguientes acciones sugeridas", pendingTasks > 0 ? "warning" : "success", "Reporte", "/marketing/admin/actions-report")
                ),
                highlights
        );
    }

    private AreaDashboardSection buildSalesSection(
            BusinessOverviewSnapshot businessOverview,
            SalesChannelProfitabilitySnapshot channelProfitability,
            ReceivableSummary receivableSummary
    ) {
        SalesChannelProfitabilitySummary channelSummary = channelProfitability != null ? channelProfitability.getSummary() : null;

        BigDecimal sales = businessOverview != null ? safe(businessOverview.getCommercialSales()) : ZERO;
        int paidOrders = businessOverview != null ? businessOverview.getPaidOrdersCount() : 0;
        int creditOrders = businessOverview != null ? businessOverview.getCreditOrdersCount() : 0;
        BigDecimal pendingReceivables = receivableSummary != null ? safe(receivableSummary.getTotalPendingAmount()) : ZERO;
        int activeChannels = channelSummary != null ? channelSummary.getActiveChannelCount() : 0;

        List<String> highlights = new ArrayList<>();
        if (creditOrders > 0 || pendingReceivables.compareTo(ZERO) > 0) {
            highlights.add("Ventas tiene cuentas por cobrar o fiados que deben monitorearse.");
        }
        if (channelSummary != null && channelSummary.getTopSellingLowMarginChannel() != null) {
            highlights.add("Hay un canal con movimiento y margen bajo: " + channelSummary.getTopSellingLowMarginChannel().getChannelLabel() + ".");
        }
        if (highlights.isEmpty()) {
            highlights.add("Ventas no muestra alertas críticas en el periodo seleccionado.");
        }

        return new AreaDashboardSection(
                "sales",
                "Ventas",
                "Pedidos, fiados, clientes activos, canales y cobranza comercial.",
                "bi-cart-check",
                pendingReceivables.compareTo(ZERO) > 0 ? "Cobranza" : "Estable",
                pendingReceivables.compareTo(ZERO) > 0 ? "warning" : "success",
                "Abrir pedidos",
                "/orders",
                List.of(
                        metric("Ventas", money(sales), "Ingreso comercial del periodo", "success", "Pedidos", "/orders"),
                        metric("Pedidos pagados", number(paidOrders), "Pedidos cerrados como pagados", "primary", "Ventas", "/income/sales"),
                        metric("Fiados", number(creditOrders), "Pedidos al crédito", creditOrders > 0 ? "warning" : "secondary", "Cobranzas", "/income/credit-accounts"),
                        metric("Canales activos", number(activeChannels), "Canales con movimiento", "info", "Canales", "/admin/products/channel-profitability")
                ),
                highlights
        );
    }

    private AreaDashboardSection buildLogisticsSection(
            BusinessOverviewSnapshot businessOverview,
            ProductReorderSuggestionSnapshot reorder,
            ProductionScheduleSnapshot productionSchedule
    ) {
        ProductReorderSuggestionSummary reorderSummary = reorder != null ? reorder.getSummary() : null;
        ProductionScheduleSummary productionSummary = productionSchedule != null ? productionSchedule.getSummary() : null;

        int lowStock = businessOverview != null ? businessOverview.getLowStockCount() : 0;
        int suggested = reorderSummary != null ? reorderSummary.getSuggestedProducts() : 0;
        int outOfStock = reorderSummary != null ? reorderSummary.getOutOfStockProducts() : 0;
        long pendingProduction = productionSummary != null ? productionSummary.getDraftOrders() : 0;

        List<String> highlights = new ArrayList<>();
        if (outOfStock > 0) {
            highlights.add("Hay productos sin stock que deben reponerse o revisarse.");
        }
        if (suggested > 0) {
            highlights.add("Hay productos con sugerencia de reposición según stock mínimo.");
        }
        if (pendingProduction > 0) {
            highlights.add("Hay producciones en borrador que pueden requerir insumos o confirmación.");
        }
        if (highlights.isEmpty()) {
            highlights.add("Logística no muestra faltantes críticos en el resumen.");
        }

        return new AreaDashboardSection(
                "logistics",
                "Logística e inventario",
                "Stock bajo, reposición, almacén, requerimientos e insumos para producción.",
                "bi-box-seam",
                suggested > 0 || outOfStock > 0 ? "Atención" : "Estable",
                suggested > 0 || outOfStock > 0 ? "warning" : "success",
                "Abrir almacén",
                "/warehouse/products-stock",
                List.of(
                        metric("Stock crítico", number(lowStock), "Productos bajo mínimo", lowStock > 0 ? "warning" : "success", "Stock", "/warehouse/products-stock"),
                        metric("Reposiciones sugeridas", number(suggested), "Productos a revisar", suggested > 0 ? "warning" : "success", "Sugerencias", "/warehouse/reorder-suggestions"),
                        metric("Sin stock", number(outOfStock), "Productos agotados", outOfStock > 0 ? "danger" : "secondary", "Stock", "/warehouse/products-stock"),
                        metric("Producción pendiente", number(pendingProduction), "Borradores de producción", pendingProduction > 0 ? "info" : "secondary", "Agenda", "/production/schedule")
                ),
                highlights
        );
    }

    private AreaDashboardSection buildFinanceSection(
            BusinessOverviewSnapshot businessOverview,
            ReceivableSummary receivableSummary,
            AccountsPayableSummary accountsPayableSummary,
            ProductProfitabilitySnapshot productProfitability
    ) {
        ProductProfitabilitySummary profitabilitySummary = productProfitability != null ? productProfitability.getSummary() : null;

        BigDecimal cash = businessOverview != null ? safe(businessOverview.getNetResult()) : ZERO;
        BigDecimal collected = businessOverview != null ? safe(businessOverview.getCashCollected()) : ZERO;
        BigDecimal receivables = receivableSummary != null ? safe(receivableSummary.getTotalPendingAmount()) : ZERO;
        BigDecimal payables = accountsPayableSummary != null ? safe(accountsPayableSummary.getTotalPendingAmount()) : ZERO;
        BigDecimal grossProfit = profitabilitySummary != null ? safe(profitabilitySummary.getTotalGrossProfit()) : ZERO;

        List<String> highlights = new ArrayList<>();
        if (cash.compareTo(ZERO) < 0) {
            highlights.add("La caja neta del periodo está negativa; revisar egresos y cobros.");
        }
        if (receivables.compareTo(ZERO) > 0) {
            highlights.add("Hay cuentas por cobrar pendientes.");
        }
        if (payables.compareTo(ZERO) > 0) {
            highlights.add("Hay cuentas por pagar pendientes.");
        }
        if (grossProfit.compareTo(ZERO) < 0) {
            highlights.add("La utilidad bruta estimada está negativa; revisar costos y precios.");
        }
        if (highlights.isEmpty()) {
            highlights.add("Finanzas no muestra alertas críticas para el periodo seleccionado.");
        }

        return new AreaDashboardSection(
                "finance",
                "Finanzas",
                "Caja real, cobranzas, pagos, cuentas pendientes y utilidad estimada.",
                "bi-cash-coin",
                cash.compareTo(ZERO) < 0 || grossProfit.compareTo(ZERO) < 0 ? "Riesgo" : "Controlado",
                cash.compareTo(ZERO) < 0 || grossProfit.compareTo(ZERO) < 0 ? "danger" : "success",
                "Abrir caja",
                "/cashflow",
                List.of(
                        metric("Caja neta", money(cash), "Resultado de ingresos menos egresos", cash.compareTo(ZERO) < 0 ? "danger" : "success", "Caja", "/cashflow"),
                        metric("Cobrado", money(collected), "Cobranza registrada", "primary", "Ingresos", "/income/sales"),
                        metric("Por cobrar", money(receivables), "Fiados pendientes", receivables.compareTo(ZERO) > 0 ? "warning" : "success", "Cobranzas", "/income/credit-accounts"),
                        metric("Por pagar", money(payables), "Deudas pendientes", payables.compareTo(ZERO) > 0 ? "warning" : "success", "Pagos", "/expenses/debts")
                ),
                highlights
        );
    }

    private AreaDashboardSection buildAccountingSection(AccountingControlPanelSnapshot accounting) {
        AccountingControlPanelSummary summary = accounting != null ? accounting.getSummary() : null;

        int draftEntries = summary != null ? summary.getDraftEntries() : 0;
        int automaticDraftEntries = summary != null ? summary.getAutomaticDraftEntries() : 0;
        int unbalancedEntries = summary != null ? summary.getUnbalancedEntries() : 0;
        int openPeriods = summary != null ? summary.getOpenPeriodsWithMovements() : 0;
        int configIssues = summary != null
                ? summary.getMissingTemplateEvents() + summary.getInvalidTemplates() + summary.getInactiveTemplates()
                + summary.getMissingSimpleRules() + summary.getInactiveSimpleRules()
                : 0;

        List<String> highlights = new ArrayList<>();
        if (draftEntries > 0) {
            highlights.add("Hay asientos en borrador pendientes de revisión.");
        }
        if (unbalancedEntries > 0) {
            highlights.add("Existen asientos descuadrados que deben corregirse.");
        }
        if (configIssues > 0) {
            highlights.add("Hay reglas o plantillas contables por configurar.");
        }
        if (highlights.isEmpty()) {
            highlights.add("Contabilidad interna no muestra alertas críticas en este resumen.");
        }

        return new AreaDashboardSection(
                "accounting",
                "Contabilidad interna",
                "Borradores, periodos, descuadres y configuración contable de apoyo.",
                "bi-journal-check",
                draftEntries > 0 || unbalancedEntries > 0 || configIssues > 0 ? "Revisar" : "Controlado",
                draftEntries > 0 || unbalancedEntries > 0 || configIssues > 0 ? "warning" : "success",
                "Abrir control contable",
                "/accounting/control-panel",
                List.of(
                        metric("Borradores", number(draftEntries), "Asientos pendientes", draftEntries > 0 ? "warning" : "success", "Borradores", "/accounting/draft-review"),
                        metric("Automáticos", number(automaticDraftEntries), "Asientos automáticos en revisión", automaticDraftEntries > 0 ? "info" : "secondary", "Control", "/accounting/control-panel"),
                        metric("Descuadres", number(unbalancedEntries), "Asientos con diferencia", unbalancedEntries > 0 ? "danger" : "success", "Control", "/accounting/control-panel"),
                        metric("Periodos abiertos", number(openPeriods), "Periodos con movimientos", "primary", "Periodos", "/accounting/period-close")
                ),
                highlights
        );
    }

    private <T> T readSection(String sectionName, Supplier<T> supplier, List<String> errors) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            log.warn("Area dashboard section failed: {}", sectionName, exception);
            errors.add(sectionName);
            return null;
        }
    }

    private AreaDashboardMetric metric(
            String label,
            String value,
            String helper,
            String badgeClass,
            String actionLabel,
            String actionUrl
    ) {
        return new AreaDashboardMetric(label, value, helper, badgeClass, actionLabel, actionUrl);
    }

    private String money(BigDecimal value) {
        BigDecimal safeValue = safe(value);
        return "S/ " + safeValue.setScale(2, RoundingMode.HALF_UP);
    }

    private String number(long value) {
        return Long.toString(value);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : ZERO;
    }
}
