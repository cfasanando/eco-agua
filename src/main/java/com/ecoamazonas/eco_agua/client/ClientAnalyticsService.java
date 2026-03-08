package com.ecoamazonas.eco_agua.client;

import com.ecoamazonas.eco_agua.order.OrderStatus;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import com.ecoamazonas.eco_agua.order.SaleOrderItem;
import com.ecoamazonas.eco_agua.order.SaleOrderRepository;
import com.ecoamazonas.eco_agua.product.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class ClientAnalyticsService {

    private final ClientRepository clientRepository;
    private final SaleOrderRepository saleOrderRepository;

    public ClientAnalyticsService(
            ClientRepository clientRepository,
            SaleOrderRepository saleOrderRepository
    ) {
        this.clientRepository = clientRepository;
        this.saleOrderRepository = saleOrderRepository;
    }

    @Transactional(readOnly = true)
    public ClientAnalyticsSnapshot buildSnapshot(Long clientId, LocalDate fromDate, LocalDate toDate) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id " + clientId));

        LocalDate effectiveToDate = toDate != null ? toDate : LocalDate.now();
        LocalDate effectiveFromDate = fromDate != null ? fromDate : effectiveToDate.minusDays(29);

        if (effectiveFromDate.isAfter(effectiveToDate)) {
            LocalDate tmp = effectiveFromDate;
            effectiveFromDate = effectiveToDate;
            effectiveToDate = tmp;
        }

        long periodDays = ChronoUnit.DAYS.between(effectiveFromDate, effectiveToDate) + 1;

        List<SaleOrder> allOrders = saleOrderRepository.findDetailedOrdersByClientIdOrderByOrderDateDescIdDesc(clientId);
        List<SaleOrder> periodOrders = saleOrderRepository.findDetailedOrdersByClientIdAndOrderDateBetweenOrderByOrderDateDescIdDesc(
                clientId,
                effectiveFromDate,
                effectiveToDate
        );

        List<SaleOrder> nonCanceledAllOrders = filterNonCanceled(allOrders);
        List<SaleOrder> commercialAllOrders = filterCommercial(allOrders);
        List<SaleOrder> nonCanceledPeriodOrders = filterNonCanceled(periodOrders);
        List<SaleOrder> commercialPeriodOrders = filterCommercial(periodOrders);

        SaleOrder latestOrder = nonCanceledAllOrders.isEmpty() ? null : nonCanceledAllOrders.get(0);
        LocalDate lastOrderDate = latestOrder != null ? latestOrder.getOrderDate() : null;
        String lastOrderStatusLabel = latestOrder != null ? toStatusLabel(latestOrder.getStatus()) : null;
        long daysSinceLastOrder = lastOrderDate != null
                ? Math.max(ChronoUnit.DAYS.between(lastOrderDate, LocalDate.now()), 0)
                : -1;

        LocalDate firstCommercialOrderDate = commercialAllOrders.stream()
                .map(SaleOrder::getOrderDate)
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .orElse(null);

        int averageIntervalDays = calculateAverageIntervalDays(commercialAllOrders, daysSinceLastOrder);

        BigDecimal totalRevenueAllTime = sumOrderAmounts(commercialAllOrders);
        BigDecimal estimatedCostAllTime = sumEstimatedOrderCosts(commercialAllOrders);
        BigDecimal estimatedProfitAllTime = safeSubtract(totalRevenueAllTime, estimatedCostAllTime);
        BigDecimal creditPendingAllTime = sumOrderAmountsByStatus(nonCanceledAllOrders, OrderStatus.CREDIT);

        BigDecimal totalRevenueInPeriod = sumOrderAmounts(commercialPeriodOrders);
        BigDecimal paidRevenueInPeriod = sumOrderAmountsByStatus(commercialPeriodOrders, OrderStatus.PAID);
        BigDecimal creditRevenueInPeriod = sumOrderAmountsByStatus(commercialPeriodOrders, OrderStatus.CREDIT);
        BigDecimal estimatedCostInPeriod = sumEstimatedOrderCosts(commercialPeriodOrders);
        BigDecimal estimatedProfitInPeriod = safeSubtract(totalRevenueInPeriod, estimatedCostInPeriod);
        BigDecimal averageTicketInPeriod = divide(totalRevenueInPeriod, commercialPeriodOrders.size(), 2);
        BigDecimal profitMarginPercentInPeriod = percent(estimatedProfitInPeriod, totalRevenueInPeriod, 1);

        int borrowedBottlesAllTime = sumBorrowedBottles(nonCanceledAllOrders);
        int borrowedBottlesInPeriod = sumBorrowedBottles(nonCanceledPeriodOrders);

        int paidOrdersInPeriod = countByStatus(commercialPeriodOrders, OrderStatus.PAID);
        int creditOrdersInPeriod = countByStatus(commercialPeriodOrders, OrderStatus.CREDIT);

        HealthEvaluation healthEvaluation = evaluateHealth(
                daysSinceLastOrder,
                averageIntervalDays,
                totalRevenueInPeriod,
                estimatedProfitInPeriod,
                creditPendingAllTime,
                commercialPeriodOrders.size()
        );

        List<String> opportunityNotes = buildOpportunityNotes(
                daysSinceLastOrder,
                averageIntervalDays,
                totalRevenueInPeriod,
                estimatedProfitInPeriod,
                creditPendingAllTime,
                commercialPeriodOrders.size(),
                client
        );

        List<ClientAnalyticsProductRow> topProducts = buildTopProducts(commercialPeriodOrders);
        List<ClientAnalyticsOrderRow> recentOrders = buildRecentOrders(nonCanceledPeriodOrders);

        return new ClientAnalyticsSnapshot(
                client,
                effectiveFromDate,
                effectiveToDate,
                periodDays,
                !nonCanceledAllOrders.isEmpty(),
                firstCommercialOrderDate,
                lastOrderDate,
                lastOrderStatusLabel,
                daysSinceLastOrder,
                averageIntervalDays,
                nonCanceledAllOrders.size(),
                commercialAllOrders.size(),
                nonCanceledPeriodOrders.size(),
                commercialPeriodOrders.size(),
                paidOrdersInPeriod,
                creditOrdersInPeriod,
                borrowedBottlesAllTime,
                borrowedBottlesInPeriod,
                totalRevenueAllTime,
                estimatedProfitAllTime,
                creditPendingAllTime,
                totalRevenueInPeriod,
                paidRevenueInPeriod,
                creditRevenueInPeriod,
                estimatedCostInPeriod,
                estimatedProfitInPeriod,
                averageTicketInPeriod,
                profitMarginPercentInPeriod,
                healthEvaluation.score(),
                healthEvaluation.label(),
                healthEvaluation.badgeClass(),
                healthEvaluation.action(),
                opportunityNotes,
                topProducts,
                recentOrders
        );
    }

    private List<SaleOrder> filterNonCanceled(List<SaleOrder> orders) {
        List<SaleOrder> results = new ArrayList<>();
        for (SaleOrder order : orders) {
            if (order == null || order.getStatus() == OrderStatus.CANCELED) {
                continue;
            }
            results.add(order);
        }
        return results;
    }

    private List<SaleOrder> filterCommercial(List<SaleOrder> orders) {
        List<SaleOrder> results = new ArrayList<>();
        for (SaleOrder order : orders) {
            if (order == null) {
                continue;
            }
            if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CREDIT) {
                results.add(order);
            }
        }
        return results;
    }

    private int calculateAverageIntervalDays(List<SaleOrder> commercialOrders, long daysSinceLastOrder) {
        LinkedHashSet<LocalDate> uniqueDates = new LinkedHashSet<>();
        for (SaleOrder order : commercialOrders) {
            if (order.getOrderDate() != null) {
                uniqueDates.add(order.getOrderDate());
            }
        }

        List<LocalDate> purchaseDates = new ArrayList<>(uniqueDates);
        purchaseDates.sort(LocalDate::compareTo);

        if (purchaseDates.isEmpty()) {
            return 7;
        }

        if (purchaseDates.size() == 1) {
            if (daysSinceLastOrder <= 0) {
                return 5;
            }
            return (int) Math.max(3, Math.min(daysSinceLastOrder, 10));
        }

        long totalDays = 0;
        int intervals = 0;
        for (int i = 1; i < purchaseDates.size(); i++) {
            long diff = ChronoUnit.DAYS.between(purchaseDates.get(i - 1), purchaseDates.get(i));
            if (diff > 0) {
                totalDays += diff;
                intervals++;
            }
        }

        if (intervals == 0) {
            return 7;
        }

        return (int) Math.max(1, Math.round((double) totalDays / intervals));
    }

    private BigDecimal sumOrderAmounts(List<SaleOrder> orders) {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleOrder order : orders) {
            if (order.getTotalAmount() != null) {
                total = total.add(order.getTotalAmount());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumOrderAmountsByStatus(List<SaleOrder> orders, OrderStatus status) {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleOrder order : orders) {
            if (order.getStatus() == status && order.getTotalAmount() != null) {
                total = total.add(order.getTotalAmount());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumEstimatedOrderCosts(List<SaleOrder> orders) {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleOrder order : orders) {
            total = total.add(calculateEstimatedOrderCost(order));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEstimatedOrderCost(SaleOrder order) {
        BigDecimal total = BigDecimal.ZERO;
        if (order.getItems() == null) {
            return total;
        }

        for (SaleOrderItem item : order.getItems()) {
            if (item == null || item.getQuantity() == null) {
                continue;
            }
            Product product = item.getProduct();
            BigDecimal unitCost = product != null && product.getSuppliesCost() != null
                    ? product.getSuppliesCost()
                    : BigDecimal.ZERO;
            total = total.add(unitCost.multiply(item.getQuantity()));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private int sumBorrowedBottles(List<SaleOrder> orders) {
        int total = 0;
        for (SaleOrder order : orders) {
            total += order.getBorrowedBottles() != null ? order.getBorrowedBottles() : 0;
        }
        return total;
    }

    private int countByStatus(List<SaleOrder> orders, OrderStatus status) {
        int total = 0;
        for (SaleOrder order : orders) {
            if (order.getStatus() == status) {
                total++;
            }
        }
        return total;
    }

    private BigDecimal safeSubtract(BigDecimal amount, BigDecimal cost) {
        BigDecimal left = amount != null ? amount : BigDecimal.ZERO;
        BigDecimal right = cost != null ? cost : BigDecimal.ZERO;
        return left.subtract(right).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal divide(BigDecimal amount, int divisor, int scale) {
        if (amount == null || divisor <= 0) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return amount.divide(BigDecimal.valueOf(divisor), scale, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator, int scale) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, scale, RoundingMode.HALF_UP);
    }

    private List<ClientAnalyticsProductRow> buildTopProducts(List<SaleOrder> orders) {
        Map<String, ProductAggregate> aggregates = new LinkedHashMap<>();

        for (SaleOrder order : orders) {
            if (order.getItems() == null) {
                continue;
            }

            for (SaleOrderItem item : order.getItems()) {
                if (item == null) {
                    continue;
                }

                String productName = resolveProductName(item);
                ProductAggregate aggregate = aggregates.computeIfAbsent(productName, key -> new ProductAggregate());

                BigDecimal quantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal revenue = item.getTotal() != null ? item.getTotal() : BigDecimal.ZERO;
                BigDecimal unitCost = item.getProduct() != null && item.getProduct().getSuppliesCost() != null
                        ? item.getProduct().getSuppliesCost()
                        : BigDecimal.ZERO;
                BigDecimal estimatedCost = unitCost.multiply(quantity);

                aggregate.quantity = aggregate.quantity.add(quantity);
                aggregate.revenue = aggregate.revenue.add(revenue);
                aggregate.estimatedCost = aggregate.estimatedCost.add(estimatedCost);
            }
        }

        List<ClientAnalyticsProductRow> rows = new ArrayList<>();
        for (Map.Entry<String, ProductAggregate> entry : aggregates.entrySet()) {
            ProductAggregate aggregate = entry.getValue();
            BigDecimal profit = aggregate.revenue.subtract(aggregate.estimatedCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal averageUnitPrice = aggregate.quantity.compareTo(BigDecimal.ZERO) > 0
                    ? aggregate.revenue.divide(aggregate.quantity, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            rows.add(new ClientAnalyticsProductRow(
                    entry.getKey(),
                    aggregate.quantity.setScale(2, RoundingMode.HALF_UP),
                    aggregate.revenue.setScale(2, RoundingMode.HALF_UP),
                    aggregate.estimatedCost.setScale(2, RoundingMode.HALF_UP),
                    profit,
                    averageUnitPrice
            ));
        }

        rows.sort((left, right) -> {
            int revenueCompare = right.getRevenue().compareTo(left.getRevenue());
            if (revenueCompare != 0) {
                return revenueCompare;
            }

            int quantityCompare = right.getQuantity().compareTo(left.getQuantity());
            if (quantityCompare != 0) {
                return quantityCompare;
            }

            return String.CASE_INSENSITIVE_ORDER.compare(left.getProductName(), right.getProductName());
        });

        if (rows.size() > 8) {
            return new ArrayList<>(rows.subList(0, 8));
        }

        return rows;
    }

    private List<ClientAnalyticsOrderRow> buildRecentOrders(List<SaleOrder> orders) {
        List<ClientAnalyticsOrderRow> rows = new ArrayList<>();

        for (SaleOrder order : orders) {
            rows.add(new ClientAnalyticsOrderRow(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getOrderDate(),
                    toStatusLabel(order.getStatus()),
                    toStatusBadgeClass(order.getStatus()),
                    valueOrZero(order.getTotalAmount()),
                    calculateEstimatedOrderCost(order),
                    safeSubtract(valueOrZero(order.getTotalAmount()), calculateEstimatedOrderCost(order)),
                    order.getBorrowedBottles() != null ? order.getBorrowedBottles() : 0,
                    order.getDeliveryPerson(),
                    order.getComment()
            ));
        }

        if (rows.size() > 20) {
            return new ArrayList<>(rows.subList(0, 20));
        }

        return rows;
    }

    private HealthEvaluation evaluateHealth(
            long daysSinceLastOrder,
            int averageIntervalDays,
            BigDecimal revenueInPeriod,
            BigDecimal estimatedProfitInPeriod,
            BigDecimal creditPendingAllTime,
            int commercialOrdersInPeriod
    ) {
        int score = 0;

        if (daysSinceLastOrder < 0) {
            return new HealthEvaluation(0, "Sin historial", "text-bg-secondary", "Primero hay que registrar pedidos para evaluar este cliente.");
        }

        if (daysSinceLastOrder <= Math.max(averageIntervalDays + 1L, 5L)) {
            score += 35;
        } else if (daysSinceLastOrder <= Math.max((averageIntervalDays * 2L), 10L)) {
            score += 22;
        } else {
            score += 8;
        }

        if (commercialOrdersInPeriod >= 8) {
            score += 25;
        } else if (commercialOrdersInPeriod >= 4) {
            score += 18;
        } else if (commercialOrdersInPeriod >= 2) {
            score += 12;
        } else if (commercialOrdersInPeriod == 1) {
            score += 6;
        }

        if (estimatedProfitInPeriod.compareTo(new BigDecimal("180.00")) >= 0) {
            score += 22;
        } else if (estimatedProfitInPeriod.compareTo(new BigDecimal("90.00")) >= 0) {
            score += 16;
        } else if (estimatedProfitInPeriod.compareTo(new BigDecimal("30.00")) >= 0) {
            score += 10;
        } else if (estimatedProfitInPeriod.compareTo(BigDecimal.ZERO) > 0) {
            score += 5;
        }

        BigDecimal creditRatio = percent(creditPendingAllTime, revenueInPeriod.compareTo(BigDecimal.ZERO) > 0 ? revenueInPeriod : BigDecimal.ONE, 1);
        if (creditPendingAllTime.compareTo(BigDecimal.ZERO) == 0) {
            score += 18;
        } else if (creditRatio.compareTo(new BigDecimal("25.0")) <= 0) {
            score += 12;
        } else if (creditRatio.compareTo(new BigDecimal("50.0")) <= 0) {
            score += 7;
        } else {
            score += 2;
        }

        if (score >= 80) {
            return new HealthEvaluation(score, "Muy rentable", "text-bg-success", "Mantener fidelización, pedir referidos y cuidar la frecuencia.");
        }
        if (score >= 60) {
            return new HealthEvaluation(score, "Rentable", "text-bg-primary", "Cliente valioso: conviene hacer seguimiento regular para no perder ritmo.");
        }
        if (score >= 40) {
            return new HealthEvaluation(score, "A vigilar", "text-bg-warning", "Conviene revisar frecuencia, promociones y cobranza antes de seguir empujando ventas.");
        }

        return new HealthEvaluation(score, "En riesgo", "text-bg-danger", "No conviene soltarlo, pero sí hacer una gestión más selectiva y medir si responde.");
    }

    private List<String> buildOpportunityNotes(
            long daysSinceLastOrder,
            int averageIntervalDays,
            BigDecimal revenueInPeriod,
            BigDecimal estimatedProfitInPeriod,
            BigDecimal creditPendingAllTime,
            int commercialOrdersInPeriod,
            Client client
    ) {
        List<String> notes = new ArrayList<>();

        if (daysSinceLastOrder >= 0 && daysSinceLastOrder > averageIntervalDays + 2L) {
            notes.add("El cliente está fuera de su ciclo habitual. Conviene hacer seguimiento directo hoy.");
        }

        if (commercialOrdersInPeriod >= 4 && estimatedProfitInPeriod.compareTo(new BigDecimal("80.00")) >= 0) {
            notes.add("Tiene buen movimiento en el período. Es candidato para fidelización o un plan de referidos.");
        }

        if (creditPendingAllTime.compareTo(BigDecimal.ZERO) > 0) {
            notes.add("Tiene saldo fiado registrado. Antes de darle más crédito, revisa cobranza y comportamiento reciente.");
        }

        if (revenueInPeriod.compareTo(new BigDecimal("120.00")) >= 0 && estimatedProfitInPeriod.compareTo(new BigDecimal("40.00")) < 0) {
            notes.add("Vende, pero deja poco margen estimado. Vale la pena revisar precio por perfil o mezcla de productos.");
        }

        if (client.getProfile() != null && client.getProfile().getName() != null) {
            notes.add("Su perfil actual es '" + client.getProfile().getName() + "'. Puedes comparar este perfil con otros clientes rentables para captar similares.");
        }

        if (notes.isEmpty()) {
            notes.add("Todavía no hay suficiente movimiento para una estrategia más fina. Por ahora conviene seguir observando frecuencia y ticket promedio.");
        }

        return notes;
    }

    private String resolveProductName(SaleOrderItem item) {
        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            return item.getDescription().trim();
        }
        if (item.getProduct() != null && item.getProduct().getName() != null && !item.getProduct().getName().isBlank()) {
            return item.getProduct().getName().trim();
        }
        return "Producto sin nombre";
    }

    private String toStatusLabel(OrderStatus status) {
        if (status == null) {
            return "Sin estado";
        }

        return switch (status) {
            case PAID -> "Pagado";
            case CREDIT -> "Fiado";
            case REQUESTED -> "Solicitado";
            case CANCELED -> "Anulado";
        };
    }

    private String toStatusBadgeClass(OrderStatus status) {
        if (status == null) {
            return "text-bg-secondary";
        }

        return switch (status) {
            case PAID -> "text-bg-success";
            case CREDIT -> "text-bg-warning";
            case REQUESTED -> "text-bg-info";
            case CANCELED -> "text-bg-danger";
        };
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static class ProductAggregate {
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal estimatedCost = BigDecimal.ZERO;
    }

    private static class HealthEvaluation {
        private final int score;
        private final String label;
        private final String badgeClass;
        private final String action;

        private HealthEvaluation(int score, String label, String badgeClass, String action) {
            this.score = score;
            this.label = label;
            this.badgeClass = badgeClass;
            this.action = action;
        }

        public int score() {
            return score;
        }

        public String label() {
            return label;
        }

        public String badgeClass() {
            return badgeClass;
        }

        public String action() {
            return action;
        }
    }
}
