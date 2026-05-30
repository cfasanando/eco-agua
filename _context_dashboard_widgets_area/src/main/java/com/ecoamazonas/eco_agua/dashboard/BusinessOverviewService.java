package com.ecoamazonas.eco_agua.dashboard;

import com.ecoamazonas.eco_agua.cashflow.CashflowItem;
import com.ecoamazonas.eco_agua.cashflow.CashflowService;
import com.ecoamazonas.eco_agua.category.Category;
import com.ecoamazonas.eco_agua.client.ClientPortfolioRow;
import com.ecoamazonas.eco_agua.client.ClientPortfolioService;
import com.ecoamazonas.eco_agua.client.ClientPortfolioSnapshot;
import com.ecoamazonas.eco_agua.expense.Expense;
import com.ecoamazonas.eco_agua.expense.ExpenseService;
import com.ecoamazonas.eco_agua.order.OrderService;
import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderItem;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import com.ecoamazonas.eco_agua.product.Product;
import com.ecoamazonas.eco_agua.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BusinessOverviewService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal LOW_STOCK_LIMIT = new BigDecimal("5");

    private final CashflowService cashflowService;
    private final OrderService orderService;
    private final SaleOrderRepository saleOrderRepository;
    private final ExpenseService expenseService;
    private final ClientPortfolioService clientPortfolioService;
    private final ProductRepository productRepository;

    public BusinessOverviewService(
            CashflowService cashflowService,
            OrderService orderService,
            SaleOrderRepository saleOrderRepository,
            ExpenseService expenseService,
            ClientPortfolioService clientPortfolioService,
            ProductRepository productRepository
    ) {
        this.cashflowService = cashflowService;
        this.orderService = orderService;
        this.saleOrderRepository = saleOrderRepository;
        this.expenseService = expenseService;
        this.clientPortfolioService = clientPortfolioService;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public BusinessOverviewSnapshot buildSnapshot(LocalDate fromDate, LocalDate toDate) {
        LocalDate effectiveToDate = toDate != null ? toDate : LocalDate.now();
        LocalDate effectiveFromDate = fromDate != null ? fromDate : effectiveToDate.minusDays(29);

        if (effectiveFromDate.isAfter(effectiveToDate)) {
            LocalDate tmp = effectiveFromDate;
            effectiveFromDate = effectiveToDate;
            effectiveToDate = tmp;
        }

        long periodDays = ChronoUnit.DAYS.between(effectiveFromDate, effectiveToDate) + 1;
        LocalDate previousToDate = effectiveFromDate.minusDays(1);
        LocalDate previousFromDate = previousToDate.minusDays(periodDays - 1);

        List<CashflowItem> currentCashflow = cashflowService.buildCashflow(effectiveFromDate, effectiveToDate);
        List<CashflowItem> previousCashflow = cashflowService.buildCashflow(previousFromDate, previousToDate);

        BigDecimal totalIncomes = nvl(cashflowService.calculateTotalIncomes(currentCashflow));
        BigDecimal totalExpenses = nvl(cashflowService.calculateTotalExpenses(currentCashflow));
        BigDecimal netResult = nvl(cashflowService.calculateNetResult(currentCashflow));

        BigDecimal previousTotalIncomes = nvl(cashflowService.calculateTotalIncomes(previousCashflow));
        BigDecimal previousTotalExpenses = nvl(cashflowService.calculateTotalExpenses(previousCashflow));
        BigDecimal previousNetResult = nvl(cashflowService.calculateNetResult(previousCashflow));

        List<SaleOrder> paidOrders = orderService.findOrdersBetweenDatesAndStatus(effectiveFromDate, effectiveToDate, OrderStatus.PAID);
        List<SaleOrder> creditOrders = orderService.findOrdersBetweenDatesAndStatus(effectiveFromDate, effectiveToDate, OrderStatus.CREDIT);
        List<SaleOrder> requestedOrders = orderService.findOrdersBetweenDatesAndStatus(effectiveFromDate, effectiveToDate, OrderStatus.REQUESTED);

        List<SaleOrder> previousPaidOrders = orderService.findOrdersBetweenDatesAndStatus(previousFromDate, previousToDate, OrderStatus.PAID);
        List<SaleOrder> previousCreditOrders = orderService.findOrdersBetweenDatesAndStatus(previousFromDate, previousToDate, OrderStatus.CREDIT);

        BigDecimal cashCollected = sumOrderTotals(paidOrders);
        BigDecimal creditGeneratedInPeriod = sumOrderTotals(creditOrders);
        BigDecimal pendingDeliveryAmount = sumOrderTotals(requestedOrders);
        BigDecimal commercialSales = cashCollected.add(creditGeneratedInPeriod);
        BigDecimal previousCommercialSales = sumOrderTotals(previousPaidOrders).add(sumOrderTotals(previousCreditOrders));
        BigDecimal averageDailyCommercialSales = divide(commercialSales, BigDecimal.valueOf(periodDays), 2);

        ClientPortfolioSnapshot portfolio = clientPortfolioService.buildSnapshot(effectiveFromDate, effectiveToDate);

        List<SaleOrder> detailedPeriodOrders = saleOrderRepository
                .findDetailedOrdersByOrderDateBetweenOrderByClientNameAscOrderDateDescIdDesc(effectiveFromDate, effectiveToDate);

        List<BusinessOverviewProductRow> topProducts = buildTopProducts(detailedPeriodOrders);
        List<BusinessOverviewExpenseCategoryRow> topExpenseCategories = buildTopExpenseCategories(
                expenseService.findByDateRange(effectiveFromDate, effectiveToDate)
        );
        List<BusinessOverviewStockRow> lowStockProducts = buildLowStockProducts();

        BigDecimal incomeChangePercent = calculateChangePercent(totalIncomes, previousTotalIncomes);
        BigDecimal expenseChangePercent = calculateChangePercent(totalExpenses, previousTotalExpenses);
        BigDecimal netChangePercent = calculateChangePercent(netResult, previousNetResult);
        BigDecimal commercialSalesChangePercent = calculateChangePercent(commercialSales, previousCommercialSales);

        int overallScore = calculateOverallScore(
                netResult,
                commercialSales,
                portfolio.getTotalCreditPendingAllTime(),
                portfolio.getReactivationCandidates().size(),
                portfolio.getDormantClients().size(),
                lowStockProducts.size()
        );
        String overallHealthLabel = resolveHealthLabel(overallScore);
        String overallHealthBadgeClass = resolveHealthBadgeClass(overallScore);
        String overallHealthSummary = buildOverallSummary(
                overallScore,
                netResult,
                commercialSales,
                totalExpenses,
                portfolio.getTotalCreditPendingAllTime(),
                lowStockProducts.size()
        );

        List<BusinessOverviewAlert> alerts = buildAlerts(
                netResult,
                commercialSales,
                previousCommercialSales,
                portfolio.getTotalCreditPendingAllTime(),
                requestedOrders.size(),
                portfolio.getReactivationCandidates().size(),
                portfolio.getDormantClients().size(),
                lowStockProducts.size()
        );

        List<String> keyTakeaways = buildKeyTakeaways(
                totalIncomes,
                totalExpenses,
                netResult,
                commercialSales,
                commercialSalesChangePercent,
                portfolio,
                lowStockProducts.size(),
                topProducts,
                topExpenseCategories
        );

        return new BusinessOverviewSnapshot(
                effectiveFromDate,
                effectiveToDate,
                previousFromDate,
                previousToDate,
                periodDays,
                totalIncomes,
                totalExpenses,
                netResult,
                previousTotalIncomes,
                previousTotalExpenses,
                previousNetResult,
                incomeChangePercent,
                expenseChangePercent,
                netChangePercent,
                commercialSales,
                previousCommercialSales,
                commercialSalesChangePercent,
                cashCollected,
                creditGeneratedInPeriod,
                pendingDeliveryAmount,
                averageDailyCommercialSales,
                portfolio.getTotalCreditPendingAllTime(),
                paidOrders.size(),
                creditOrders.size(),
                requestedOrders.size(),
                portfolio.getClientsWithOrdersInPeriod(),
                portfolio.getTotalActiveClients(),
                portfolio.getReactivationCandidates().size(),
                portfolio.getDormantClients().size(),
                portfolio.getCreditRiskClients().size(),
                lowStockProducts.size(),
                portfolio.getTotalBorrowedBottlesInPeriod(),
                overallScore,
                overallHealthLabel,
                overallHealthBadgeClass,
                overallHealthSummary,
                keyTakeaways,
                alerts,
                topProducts,
                topExpenseCategories,
                lowStockProducts,
                portfolio.getTopRevenueClients(),
                portfolio.getTopProfitClients(),
                portfolio.getReactivationCandidates(),
                portfolio.getCreditRiskClients()
        );
    }

    private List<BusinessOverviewProductRow> buildTopProducts(List<SaleOrder> orders) {
        Map<String, ProductAccumulator> acc = new LinkedHashMap<>();

        for (SaleOrder order : orders) {
            if (!isCommercial(order)) {
                continue;
            }

            if (order.getItems() == null) {
                continue;
            }

            for (SaleOrderItem item : order.getItems()) {
                if (item == null) {
                    continue;
                }

                String key = resolveProductKey(item);
                ProductAccumulator row = acc.computeIfAbsent(key, ignored -> new ProductAccumulator(resolveProductLabel(item)));

                BigDecimal quantity = nvl(item.getQuantity());
                BigDecimal revenue = nvl(item.getTotal());
                BigDecimal unitCost = resolveProductUnitCost(item);
                BigDecimal estimatedCost = unitCost.multiply(quantity);

                row.quantity = row.quantity.add(quantity);
                row.revenue = row.revenue.add(revenue);
                row.estimatedCost = row.estimatedCost.add(estimatedCost);
                row.estimatedProfit = row.estimatedProfit.add(revenue.subtract(estimatedCost));
            }
        }

        return acc.values().stream()
                .map(this::toProductRow)
                .sorted(Comparator.comparing(BusinessOverviewProductRow::getRevenue).reversed())
                .limit(6)
                .toList();
    }

    private List<BusinessOverviewExpenseCategoryRow> buildTopExpenseCategories(List<Expense> expenses) {
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        BigDecimal total = ZERO;

        for (Expense expense : expenses) {
            if (expense == null) {
                continue;
            }

            String categoryName = resolveExpenseCategoryName(expense);
            BigDecimal amount = nvl(expense.getAmount());
            grouped.merge(categoryName, amount, BigDecimal::add);
            total = total.add(amount);
        }

        BigDecimal denominator = total.compareTo(ZERO) > 0 ? total : BigDecimal.ONE;

        return grouped.entrySet().stream()
                .map(entry -> new BusinessOverviewExpenseCategoryRow(
                        entry.getKey(),
                        entry.getValue(),
                        entry.getValue()
                                .multiply(ONE_HUNDRED)
                                .divide(denominator, 1, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(BusinessOverviewExpenseCategoryRow::getAmount).reversed())
                .limit(6)
                .toList();
    }

    private List<BusinessOverviewStockRow> buildLowStockProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(Objects::nonNull)
                .filter(product -> nvl(product.getStock()).compareTo(LOW_STOCK_LIMIT) <= 0)
                .sorted(Comparator.comparing(Product::getStock, Comparator.nullsFirst(BigDecimal::compareTo))
                        .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(8)
                .map(this::toStockRow)
                .toList();
    }

    private List<BusinessOverviewAlert> buildAlerts(
            BigDecimal netResult,
            BigDecimal commercialSales,
            BigDecimal previousCommercialSales,
            BigDecimal creditPending,
            int requestedOrdersCount,
            int reactivationCount,
            int dormantCount,
            int lowStockCount
    ) {
        List<BusinessOverviewAlert> alerts = new ArrayList<>();

        if (netResult.compareTo(ZERO) < 0) {
            alerts.add(new BusinessOverviewAlert(
                    "danger",
                    "Resultado negativo",
                    "Los gastos superaron a los ingresos en el rango analizado. Conviene revisar gasto variable y precios.",
                    "Ver flujo de caja",
                    "/cashflow"
            ));
        }

        if (previousCommercialSales.compareTo(ZERO) > 0 && commercialSales.compareTo(previousCommercialSales) < 0) {
            alerts.add(new BusinessOverviewAlert(
                    "warning",
                    "Caída de ventas comerciales",
                    "Las ventas pagadas + fiadas están por debajo del período anterior. Conviene revisar reactivación y promociones.",
                    "Ver cartera",
                    "/admin/clients/portfolio"
            ));
        }

        if (commercialSales.compareTo(ZERO) > 0 && creditPending.compareTo(commercialSales.multiply(new BigDecimal("0.35"))) > 0) {
            alerts.add(new BusinessOverviewAlert(
                    "warning",
                    "Fiado alto para el nivel de ventas",
                    "La cartera pendiente ya pesa mucho frente a las ventas del período. Prioriza cobranza antes de seguir extendiendo crédito.",
                    "Ir a cuentas por cobrar",
                    "/income/credit"
            ));
        }

        if (requestedOrdersCount > 0) {
            alerts.add(new BusinessOverviewAlert(
                    "info",
                    "Pedidos por entregar",
                    "Hay pedidos solicitados que todavía no se cerraron. Eso impacta caja y servicio si se quedan pendientes demasiado tiempo.",
                    "Volver a inicio",
                    "/home"
            ));
        }

        if (reactivationCount > 0 || dormantCount > 0) {
            alerts.add(new BusinessOverviewAlert(
                    "info",
                    "Clientes por recuperar",
                    "Ya hay señales claras de clientes fuera de ciclo o dormidos. Un seguimiento comercial semanal puede recuperar ingresos rápidamente.",
                    "Ver cartera",
                    "/admin/clients/portfolio"
            ));
        }

        if (lowStockCount > 0) {
            alerts.add(new BusinessOverviewAlert(
                    "warning",
                    "Stock crítico detectado",
                    "Hay productos activos con stock bajo o agotado. Revisa reposición antes de que afecte ventas o tiempos de entrega.",
                    "Ver almacén",
                    "/warehouse/products-stock"
            ));
        }

        if (alerts.isEmpty()) {
            alerts.add(new BusinessOverviewAlert(
                    "success",
                    "Panel sin alertas críticas",
                    "No se detectaron focos urgentes con las reglas actuales. Es un buen momento para trabajar mejora comercial y metas.",
                    "Ver punto de equilibrio",
                    "/cashflow/break-even"
            ));
        }

        return alerts;
    }

    private List<String> buildKeyTakeaways(
            BigDecimal totalIncomes,
            BigDecimal totalExpenses,
            BigDecimal netResult,
            BigDecimal commercialSales,
            BigDecimal commercialSalesChangePercent,
            ClientPortfolioSnapshot portfolio,
            int lowStockCount,
            List<BusinessOverviewProductRow> topProducts,
            List<BusinessOverviewExpenseCategoryRow> topExpenseCategories
    ) {
        List<String> notes = new ArrayList<>();

        notes.add("Ingresos del período: S/. " + formatMoney(totalIncomes) + " y gastos registrados: S/. " + formatMoney(totalExpenses) + ".");

        if (netResult.compareTo(ZERO) >= 0) {
            notes.add("El período cierra con resultado positivo de S/. " + formatMoney(netResult) + ". Mantén foco en cobrar y sostener margen.");
        } else {
            notes.add("El período cierra en negativo por S/. " + formatMoney(netResult.abs()) + ". Ajusta gasto y empuja ventas rentables.");
        }

        notes.add("La venta comercial (pagado + fiado) fue S/. " + formatMoney(commercialSales) +
                " con variación de " + formatPercent(commercialSalesChangePercent) + " frente al período anterior.");

        if (!topProducts.isEmpty()) {
            BusinessOverviewProductRow first = topProducts.get(0);
            notes.add("El producto más fuerte fue " + first.getProductName() +
                    " con ingresos por S/. " + formatMoney(first.getRevenue()) + ".");
        }

        if (!topExpenseCategories.isEmpty()) {
            BusinessOverviewExpenseCategoryRow first = topExpenseCategories.get(0);
            notes.add("La mayor presión de gasto está en " + first.getCategoryName() +
                    " con S/. " + formatMoney(first.getAmount()) + ".");
        }

        if (portfolio.getReactivationCandidates().size() > 0) {
            notes.add("Hay " + portfolio.getReactivationCandidates().size() + " cliente(s) con oportunidad clara de reactivación.");
        }

        if (portfolio.getCreditRiskClients().size() > 0) {
            notes.add("La cartera pendiente acumulada está en S/. " + formatMoney(portfolio.getTotalCreditPendingAllTime()) + ". Conviene ordenar cobranza.");
        }

        if (lowStockCount > 0) {
            notes.add("Se detectaron " + lowStockCount + " producto(s) con stock crítico. Revisa reposición para no frenar ventas.");
        }

        return notes.stream().limit(7).toList();
    }

    private int calculateOverallScore(
            BigDecimal netResult,
            BigDecimal commercialSales,
            BigDecimal creditPending,
            int reactivationCount,
            int dormantCount,
            int lowStockCount
    ) {
        int score = 100;

        if (netResult.compareTo(ZERO) < 0) {
            score -= 30;
        }

        if (commercialSales.compareTo(ZERO) > 0 && creditPending.compareTo(commercialSales.multiply(new BigDecimal("0.35"))) > 0) {
            score -= 20;
        }

        score -= Math.min(reactivationCount * 4, 20);
        score -= Math.min(dormantCount * 5, 20);
        score -= Math.min(lowStockCount * 3, 15);

        return Math.max(score, 20);
    }

    private String resolveHealthLabel(int score) {
        if (score >= 85) {
            return "Saludable";
        }
        if (score >= 70) {
            return "Estable";
        }
        if (score >= 50) {
            return "En observación";
        }
        return "Crítico";
    }

    private String resolveHealthBadgeClass(int score) {
        if (score >= 85) {
            return " text-bg-success";
        }
        if (score >= 70) {
            return " text-bg-primary";
        }
        if (score >= 50) {
            return " text-bg-warning";
        }
        return " text-bg-danger";
    }

    private String buildOverallSummary(
            int score,
            BigDecimal netResult,
            BigDecimal commercialSales,
            BigDecimal totalExpenses,
            BigDecimal creditPending,
            int lowStockCount
    ) {
        if (score >= 85) {
            return "El negocio muestra una lectura saludable en el período: resultado positivo, buena actividad comercial y focos controlables.";
        }
        if (score >= 70) {
            return "El negocio está estable, pero conviene seguir de cerca la cobranza y el gasto para no perder margen.";
        }
        if (score >= 50) {
            return "Hay señales de atención: el resultado, la cartera o el stock ya requieren seguimiento semanal más disciplinado.";
        }
        return "El período deja señales críticas. Conviene revisar precios, gasto, cobranza y reposición cuanto antes para proteger caja.";
    }

    private BusinessOverviewProductRow toProductRow(ProductAccumulator accumulator) {
        BigDecimal averageUnitPrice = accumulator.quantity.compareTo(ZERO) > 0
                ? accumulator.revenue.divide(accumulator.quantity, 2, RoundingMode.HALF_UP)
                : ZERO;

        return new BusinessOverviewProductRow(
                accumulator.productName,
                accumulator.quantity,
                accumulator.revenue,
                accumulator.estimatedCost,
                accumulator.estimatedProfit,
                averageUnitPrice
        );
    }

    private BusinessOverviewStockRow toStockRow(Product product) {
        BigDecimal stock = nvl(product.getStock());
        String label;
        String badgeClass;

        if (stock.compareTo(ZERO) <= 0) {
            label = "Agotado";
            badgeClass = " text-bg-danger";
        } else if (stock.compareTo(new BigDecimal("2")) <= 0) {
            label = "Muy bajo";
            badgeClass = " text-bg-warning";
        } else {
            label = "Bajo";
            badgeClass = " text-bg-primary";
        }

        return new BusinessOverviewStockRow(
                product.getId(),
                product.getName(),
                stock,
                label,
                badgeClass
        );
    }

    private boolean isCommercial(SaleOrder order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }

        return order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CREDIT;
    }

    private String resolveExpenseCategoryName(Expense expense) {
        try {
            Category category = expense.getCategory();
            if (category != null && category.getName() != null && !category.getName().isBlank()) {
                return category.getName();
            }
        } catch (Exception ignored) {
            // Keep graceful fallback when relations are incomplete.
        }

        return "Sin categoría";
    }

    private String resolveProductKey(SaleOrderItem item) {
        try {
            Product product = item.getProduct();
            if (product != null && product.getId() != null) {
                return "product:" + product.getId();
            }
        } catch (Exception ignored) {
            // Keep graceful fallback for deleted products.
        }

        String description = item.getDescription();
        if (description != null && !description.isBlank()) {
            return "desc:" + description.trim().toLowerCase();
        }

        return "desc:producto-eliminado";
    }

    private String resolveProductLabel(SaleOrderItem item) {
        try {
            Product product = item.getProduct();
            if (product != null && product.getName() != null && !product.getName().isBlank()) {
                return product.getName();
            }
        } catch (Exception ignored) {
            // Keep graceful fallback for deleted products.
        }

        String description = item.getDescription();
        if (description != null && !description.isBlank()) {
            return description;
        }

        return "Producto eliminado";
    }

    private BigDecimal resolveProductUnitCost(SaleOrderItem item) {
        try {
            Product product = item.getProduct();
            if (product != null) {
                return nvl(product.getSuppliesCost());
            }
        } catch (Exception ignored) {
            // Keep graceful fallback for deleted products.
        }

        return ZERO;
    }

    private BigDecimal sumOrderTotals(List<SaleOrder> orders) {
        return orders.stream()
                .filter(Objects::nonNull)
                .map(SaleOrder::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal calculateChangePercent(BigDecimal current, BigDecimal previous) {
        BigDecimal currentValue = nvl(current);
        BigDecimal previousValue = nvl(previous);

        if (previousValue.compareTo(ZERO) == 0) {
            if (currentValue.compareTo(ZERO) == 0) {
                return ZERO;
            }
            return ONE_HUNDRED;
        }

        return currentValue
                .subtract(previousValue)
                .multiply(ONE_HUNDRED)
                .divide(previousValue.abs(), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal divide(BigDecimal value, BigDecimal divisor, int scale) {
        if (divisor == null || divisor.compareTo(ZERO) == 0) {
            return ZERO;
        }

        return nvl(value).divide(divisor, scale, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private String formatMoney(BigDecimal value) {
        return nvl(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercent(BigDecimal value) {
        BigDecimal safeValue = nvl(value).setScale(1, RoundingMode.HALF_UP);
        String prefix = safeValue.compareTo(ZERO) > 0 ? "+" : "";
        return prefix + safeValue.toPlainString() + "%";
    }

    private static class ProductAccumulator {
        private final String productName;
        private BigDecimal quantity = ZERO;
        private BigDecimal revenue = ZERO;
        private BigDecimal estimatedCost = ZERO;
        private BigDecimal estimatedProfit = ZERO;

        private ProductAccumulator(String productName) {
            this.productName = productName;
        }
    }
}
