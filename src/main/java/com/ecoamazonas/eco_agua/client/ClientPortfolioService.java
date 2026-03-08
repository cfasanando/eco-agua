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
public class ClientPortfolioService {

    private final ClientRepository clientRepository;
    private final SaleOrderRepository saleOrderRepository;

    public ClientPortfolioService(
            ClientRepository clientRepository,
            SaleOrderRepository saleOrderRepository
    ) {
        this.clientRepository = clientRepository;
        this.saleOrderRepository = saleOrderRepository;
    }

    @Transactional(readOnly = true)
    public ClientPortfolioSnapshot buildSnapshot(LocalDate fromDate, LocalDate toDate) {
        LocalDate effectiveToDate = toDate != null ? toDate : LocalDate.now();
        LocalDate effectiveFromDate = fromDate != null ? fromDate : effectiveToDate.minusDays(29);

        if (effectiveFromDate.isAfter(effectiveToDate)) {
            LocalDate tmp = effectiveFromDate;
            effectiveFromDate = effectiveToDate;
            effectiveToDate = tmp;
        }

        long periodDays = ChronoUnit.DAYS.between(effectiveFromDate, effectiveToDate) + 1;
        List<Client> activeClients = clientRepository.findByActiveTrueOrderByNameAsc();
        List<SaleOrder> allOrders = saleOrderRepository.findDetailedOrdersOrderByClientNameAscOrderDateDescIdDesc();
        List<SaleOrder> periodOrders = saleOrderRepository.findDetailedOrdersByOrderDateBetweenOrderByClientNameAscOrderDateDescIdDesc(
                effectiveFromDate,
                effectiveToDate
        );

        Map<Long, Client> activeClientMap = new LinkedHashMap<>();
        for (Client client : activeClients) {
            if (client != null && client.getId() != null) {
                activeClientMap.put(client.getId(), client);
            }
        }

        Map<Long, List<SaleOrder>> allOrdersByClientId = groupOrdersByClientId(allOrders, activeClientMap);
        Map<Long, List<SaleOrder>> periodOrdersByClientId = groupOrdersByClientId(periodOrders, activeClientMap);

        List<ClientPortfolioRow> rows = new ArrayList<>();
        BigDecimal totalRevenueInPeriod = BigDecimal.ZERO;
        BigDecimal totalEstimatedProfitInPeriod = BigDecimal.ZERO;
        BigDecimal totalCreditPendingAllTime = BigDecimal.ZERO;
        int totalBorrowedBottlesInPeriod = 0;
        int clientsWithHistory = 0;
        int clientsWithOrdersInPeriod = 0;

        for (Client client : activeClients) {
            Long clientId = client.getId();
            if (clientId == null) {
                continue;
            }

            List<SaleOrder> clientAllOrders = allOrdersByClientId.getOrDefault(clientId, List.of());
            List<SaleOrder> clientPeriodOrders = periodOrdersByClientId.getOrDefault(clientId, List.of());

            List<SaleOrder> nonCanceledAllOrders = filterNonCanceled(clientAllOrders);
            List<SaleOrder> commercialAllOrders = filterCommercial(clientAllOrders);
            List<SaleOrder> nonCanceledPeriodOrders = filterNonCanceled(clientPeriodOrders);
            List<SaleOrder> commercialPeriodOrders = filterCommercial(clientPeriodOrders);

            boolean historyAvailable = !nonCanceledAllOrders.isEmpty();
            if (historyAvailable) {
                clientsWithHistory++;
            }
            if (!commercialPeriodOrders.isEmpty()) {
                clientsWithOrdersInPeriod++;
            }

            SaleOrder latestOrder = nonCanceledAllOrders.isEmpty() ? null : nonCanceledAllOrders.get(0);
            LocalDate lastOrderDate = latestOrder != null ? latestOrder.getOrderDate() : null;
            long daysSinceLastOrder = lastOrderDate != null
                    ? Math.max(ChronoUnit.DAYS.between(lastOrderDate, LocalDate.now()), 0)
                    : -1;
            int averageIntervalDays = calculateAverageIntervalDays(commercialAllOrders, daysSinceLastOrder);
            long overdueDays = calculateOverdueDays(lastOrderDate, averageIntervalDays, LocalDate.now());

            BigDecimal revenueInPeriod = sumOrderAmounts(commercialPeriodOrders);
            BigDecimal estimatedCostInPeriod = sumEstimatedOrderCosts(commercialPeriodOrders);
            BigDecimal estimatedProfitInPeriod = safeSubtract(revenueInPeriod, estimatedCostInPeriod);
            BigDecimal averageTicketInPeriod = divide(revenueInPeriod, commercialPeriodOrders.size(), 2);
            BigDecimal profitMarginPercentInPeriod = percent(estimatedProfitInPeriod, revenueInPeriod, 1);
            BigDecimal creditPendingAllTime = sumOrderAmountsByStatus(nonCanceledAllOrders, OrderStatus.CREDIT);
            int borrowedBottlesInPeriod = sumBorrowedBottles(nonCanceledPeriodOrders);
            int borrowedBottlesAllTime = sumBorrowedBottles(nonCanceledAllOrders);

            HealthEvaluation healthEvaluation = evaluateHealth(
                    daysSinceLastOrder,
                    averageIntervalDays,
                    revenueInPeriod,
                    estimatedProfitInPeriod,
                    creditPendingAllTime,
                    commercialPeriodOrders.size()
            );

            boolean reactivationCandidate = historyAvailable
                    && overdueDays >= 2
                    && commercialPeriodOrders.isEmpty();

            boolean dormant = historyAvailable
                    && daysSinceLastOrder > Math.max((long) averageIntervalDays * 3L, 21L)
                    && commercialPeriodOrders.isEmpty();

            String strategyNote = buildStrategyNote(
                    client,
                    revenueInPeriod,
                    estimatedProfitInPeriod,
                    creditPendingAllTime,
                    overdueDays,
                    reactivationCandidate,
                    dormant
            );

            ClientPortfolioRow row = new ClientPortfolioRow(
                    clientId,
                    client.getName(),
                    client.getProfile() != null ? client.getProfile().getName() : "-",
                    client.getPhone(),
                    client.getRegistrationDate() != null ? client.getRegistrationDate().toLocalDate() : null,
                    historyAvailable,
                    lastOrderDate,
                    daysSinceLastOrder,
                    averageIntervalDays,
                    overdueDays,
                    commercialPeriodOrders.size(),
                    revenueInPeriod,
                    estimatedProfitInPeriod,
                    averageTicketInPeriod,
                    profitMarginPercentInPeriod,
                    creditPendingAllTime,
                    borrowedBottlesInPeriod,
                    borrowedBottlesAllTime,
                    healthEvaluation.score(),
                    healthEvaluation.label(),
                    healthEvaluation.badgeClass(),
                    healthEvaluation.action(),
                    strategyNote,
                    reactivationCandidate,
                    dormant
            );

            rows.add(row);
            totalRevenueInPeriod = totalRevenueInPeriod.add(revenueInPeriod);
            totalEstimatedProfitInPeriod = totalEstimatedProfitInPeriod.add(estimatedProfitInPeriod);
            totalCreditPendingAllTime = totalCreditPendingAllTime.add(creditPendingAllTime);
            totalBorrowedBottlesInPeriod += borrowedBottlesInPeriod;
        }

        rows.sort(
                Comparator.comparingInt(ClientPortfolioRow::getHealthScore).reversed()
                        .thenComparing(ClientPortfolioRow::getTotalRevenueInPeriod, Comparator.reverseOrder())
                        .thenComparing(ClientPortfolioRow::getEstimatedProfitInPeriod, Comparator.reverseOrder())
                        .thenComparing(ClientPortfolioRow::getClientName, String.CASE_INSENSITIVE_ORDER)
        );

        List<ClientPortfolioRow> topRevenueClients = rows.stream()
                .filter(row -> row.getTotalRevenueInPeriod().compareTo(BigDecimal.ZERO) > 0)
                .sorted(
                        Comparator.comparing(ClientPortfolioRow::getTotalRevenueInPeriod, Comparator.reverseOrder())
                                .thenComparing(ClientPortfolioRow::getEstimatedProfitInPeriod, Comparator.reverseOrder())
                                .thenComparing(ClientPortfolioRow::getClientName, String.CASE_INSENSITIVE_ORDER)
                )
                .limit(8)
                .toList();

        List<ClientPortfolioRow> topProfitClients = rows.stream()
                .filter(row -> row.getEstimatedProfitInPeriod().compareTo(BigDecimal.ZERO) > 0)
                .sorted(
                        Comparator.comparing(ClientPortfolioRow::getEstimatedProfitInPeriod, Comparator.reverseOrder())
                                .thenComparing(ClientPortfolioRow::getTotalRevenueInPeriod, Comparator.reverseOrder())
                                .thenComparing(ClientPortfolioRow::getClientName, String.CASE_INSENSITIVE_ORDER)
                )
                .limit(8)
                .toList();

        List<ClientPortfolioRow> reactivationCandidates = rows.stream()
                .filter(ClientPortfolioRow::isReactivationCandidate)
                .sorted(
                        Comparator.comparingLong(ClientPortfolioRow::getOverdueDays).reversed()
                                .thenComparing(ClientPortfolioRow::getTotalRevenueInPeriod, Comparator.reverseOrder())
                                .thenComparing(ClientPortfolioRow::getClientName, String.CASE_INSENSITIVE_ORDER)
                )
                .limit(8)
                .toList();

        List<ClientPortfolioRow> creditRiskClients = rows.stream()
                .filter(row -> row.getCreditPendingAllTime().compareTo(BigDecimal.ZERO) > 0)
                .sorted(
                        Comparator.comparing(ClientPortfolioRow::getCreditPendingAllTime, Comparator.reverseOrder())
                                .thenComparingLong(ClientPortfolioRow::getDaysSinceLastOrder).reversed()
                                .thenComparing(ClientPortfolioRow::getClientName, String.CASE_INSENSITIVE_ORDER)
                )
                .limit(8)
                .toList();

        List<ClientPortfolioRow> dormantClients = rows.stream()
                .filter(ClientPortfolioRow::isDormant)
                .sorted(
                        Comparator.comparingLong(ClientPortfolioRow::getDaysSinceLastOrder).reversed()
                                .thenComparing(ClientPortfolioRow::getClientName, String.CASE_INSENSITIVE_ORDER)
                )
                .limit(8)
                .toList();

        return new ClientPortfolioSnapshot(
                effectiveFromDate,
                effectiveToDate,
                periodDays,
                activeClients.size(),
                clientsWithHistory,
                clientsWithOrdersInPeriod,
                totalRevenueInPeriod.setScale(2, RoundingMode.HALF_UP),
                totalEstimatedProfitInPeriod.setScale(2, RoundingMode.HALF_UP),
                totalCreditPendingAllTime.setScale(2, RoundingMode.HALF_UP),
                totalBorrowedBottlesInPeriod,
                rows,
                topRevenueClients,
                topProfitClients,
                reactivationCandidates,
                creditRiskClients,
                dormantClients
        );
    }

    private Map<Long, List<SaleOrder>> groupOrdersByClientId(List<SaleOrder> orders, Map<Long, Client> activeClientMap) {
        Map<Long, List<SaleOrder>> grouped = new LinkedHashMap<>();

        for (SaleOrder order : orders) {
            if (order == null || order.getClient() == null || order.getClient().getId() == null) {
                continue;
            }

            Long clientId = order.getClient().getId();
            if (!activeClientMap.containsKey(clientId)) {
                continue;
            }

            grouped.computeIfAbsent(clientId, key -> new ArrayList<>()).add(order);
        }

        return grouped;
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

    private long calculateOverdueDays(LocalDate lastOrderDate, int averageIntervalDays, LocalDate today) {
        if (lastOrderDate == null) {
            return 0;
        }

        LocalDate expectedNextOrderDate = lastOrderDate.plusDays(Math.max(averageIntervalDays, 1));
        if (!expectedNextOrderDate.isBefore(today)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(expectedNextOrderDate, today);
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

        BigDecimal creditRatio = percent(
                creditPendingAllTime,
                revenueInPeriod.compareTo(BigDecimal.ZERO) > 0 ? revenueInPeriod : BigDecimal.ONE,
                1
        );
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

    private String buildStrategyNote(
            Client client,
            BigDecimal revenueInPeriod,
            BigDecimal estimatedProfitInPeriod,
            BigDecimal creditPendingAllTime,
            long overdueDays,
            boolean reactivationCandidate,
            boolean dormant
    ) {
        if (dormant) {
            return "Cliente dormido. Conviene reactivarlo con contacto directo, recordatorio o promoción puntual.";
        }

        if (creditPendingAllTime.compareTo(BigDecimal.ZERO) > 0) {
            return "Tiene fiado pendiente. Prioriza cobranza y define si todavía conviene seguir dándole crédito.";
        }

        if (reactivationCandidate || overdueDays > 0) {
            return "Ya salió de su ciclo habitual. Este cliente amerita seguimiento hoy para no dejar que se enfríe.";
        }

        if (revenueInPeriod.compareTo(new BigDecimal("120.00")) >= 0
                && estimatedProfitInPeriod.compareTo(new BigDecimal("40.00")) < 0) {
            return "Tiene movimiento, pero el margen está ajustado. Revisa precio por perfil o mezcla de productos.";
        }

        if (estimatedProfitInPeriod.compareTo(new BigDecimal("80.00")) >= 0) {
            String profileName = client.getProfile() != null && client.getProfile().getName() != null
                    ? client.getProfile().getName()
                    : "general";
            return "Cliente fuerte para fidelización. Vale la pena buscar más clientes parecidos a su perfil '" + profileName + "'.";
        }

        return "Seguir observando frecuencia, ticket y margen antes de tomar una acción más agresiva.";
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
