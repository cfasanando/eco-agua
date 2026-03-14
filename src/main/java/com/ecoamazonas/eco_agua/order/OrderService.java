package com.ecoamazonas.eco_agua.order;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.client.ClientRepository;
import com.ecoamazonas.eco_agua.expense.PersonnelExpenseAutoSyncService;
import com.ecoamazonas.eco_agua.inventory.InventoryMovementType;
import com.ecoamazonas.eco_agua.inventory.InventoryService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final int SUGGESTION_LOOKBACK_DAYS = 60;

    private final SaleOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final InventoryService inventoryService;
    private final PersonnelExpenseAutoSyncService personnelExpenseAutoSyncService;

    public OrderService(
            SaleOrderRepository orderRepository,
            ProductRepository productRepository,
            ClientRepository clientRepository,
            InventoryService inventoryService,
            PersonnelExpenseAutoSyncService personnelExpenseAutoSyncService
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.clientRepository = clientRepository;
        this.inventoryService = inventoryService;
        this.personnelExpenseAutoSyncService = personnelExpenseAutoSyncService;
    }

    @Transactional
    public SaleOrder createOrderFromForm(
            LocalDate orderDate,
            Long clientId,
            String deliveryPerson,
            Integer borrowedBottles,
            String comment,
            List<Long> productIds,
            List<BigDecimal> quantities,
            List<BigDecimal> unitPrices,
            List<BigDecimal> lineTotals,
            OrderStatus initialStatus
    ) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client must be selected.");
        }

        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one product line.");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        LocalDate effectiveDate = (orderDate != null ? orderDate : LocalDate.now());

        SaleOrder order = new SaleOrder();
        order.setClient(client);
        order.setOrderDate(effectiveDate);
        order.setStatus(initialStatus != null ? initialStatus : OrderStatus.REQUESTED);
        order.setDeliveryPerson(deliveryPerson);
        order.setBorrowedBottles(borrowedBottles != null ? borrowedBottles : 0);
        order.setComment(comment);

        long countForDay = orderRepository.countByOrderDate(effectiveDate);
        order.setOrderNumber((int) (countForDay + 1));

        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            if (productId == null) {
                continue;
            }

            BigDecimal qty = (quantities != null && quantities.size() > i)
                    ? quantities.get(i)
                    : null;

            BigDecimal price = (unitPrices != null && unitPrices.size() > i)
                    ? unitPrices.get(i)
                    : null;

            if (qty == null || price == null) {
                continue;
            }

            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

            BigDecimal availableStock = product.getStock() != null ? product.getStock() : BigDecimal.ZERO;

            if (availableStock.compareTo(qty) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: " + product.getName() +
                                " (available: " + availableStock + ", required: " + qty + ")"
                );
            }

            SaleOrderItem item = new SaleOrderItem();
            item.setProduct(product);
            item.setDescription(product.getName());
            item.setQuantity(qty);
            item.setUnitPrice(price);

            BigDecimal lineTotal = null;
            if (lineTotals != null && lineTotals.size() > i) {
                lineTotal = lineTotals.get(i);
            }

            if (lineTotal == null) {
                lineTotal = qty.multiply(price);
            }

            lineTotal = lineTotal.setScale(2, RoundingMode.HALF_UP);
            item.setTotal(lineTotal);

            order.addItem(item);
            total = total.add(lineTotal);
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order has no valid lines.");
        }

        order.setTotalAmount(total);

        SaleOrder saved = orderRepository.save(order);

        saved.getItems().forEach(item -> inventoryService.registerProductMovement(
                item.getProduct().getId(),
                BigDecimal.ZERO,
                item.getQuantity(),
                InventoryMovementType.SALE,
                "SALE_ORDER",
                saved.getId(),
                "Order #" + saved.getOrderNumber(),
                saved.getOrderDate()
        ));

        orderRepository.flush();
        personnelExpenseAutoSyncService.syncSalaryExpensesForDate(saved.getOrderDate());

        return saved;
    }

    @Transactional
    public SaleOrder changeStatus(Long orderId, OrderStatus newStatus) {
        return changeStatus(orderId, newStatus, false, null, null);
    }

    @Transactional
    public SaleOrder changeStatus(
            Long orderId,
            OrderStatus newStatus,
            Boolean returnToStock,
            String cancelReason,
            LocalDate cancelDate
    ) {
        SaleOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == newStatus) {
            return order;
        }

        if (newStatus == OrderStatus.CANCELED && order.getStatus() != OrderStatus.CANCELED) {
            boolean effectiveReturn = Boolean.TRUE.equals(returnToStock);
            LocalDate movementDate = (cancelDate != null ? cancelDate : LocalDate.now());

            if (effectiveReturn) {
                String observation = "Order canceled - stock returned";
                if (cancelReason != null && !cancelReason.isBlank()) {
                    observation = observation + ". Reason: " + cancelReason;
                }

                final String observationText = observation;
                final LocalDate finalMovementDate = movementDate;

                order.getItems().forEach(item -> inventoryService.registerProductMovement(
                        item.getProduct().getId(),
                        item.getQuantity(),
                        BigDecimal.ZERO,
                        InventoryMovementType.ADJUSTMENT,
                        "SALE_ORDER",
                        order.getId(),
                        observationText,
                        finalMovementDate
                ));
            }
        }

        order.setStatus(newStatus);

        orderRepository.save(order);
        orderRepository.flush();
        personnelExpenseAutoSyncService.syncSalaryExpensesForDate(order.getOrderDate());

        return order;
    }

    @Transactional(readOnly = true)
    public List<SaleOrder> findOrdersForDate(LocalDate date) {
        return orderRepository.findByOrderDate(date);
    }

    @Transactional(readOnly = true)
    public List<SaleOrder> findOrdersForDateAndStatus(LocalDate date, OrderStatus status) {
        return orderRepository.findByOrderDateAndStatus(date, status);
    }

    @Transactional(readOnly = true)
    public List<SaleOrder> findOrdersBetweenDatesAndStatus(
            LocalDate startDate,
            LocalDate endDate,
            OrderStatus status
    ) {
        if (startDate == null && endDate == null) {
            throw new IllegalArgumentException("At least one date must be provided.");
        }

        if (startDate == null) {
            startDate = endDate;
        }

        if (endDate == null) {
            endDate = startDate;
        }

        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        return orderRepository.findByOrderDateBetweenAndStatus(startDate, endDate, status);
    }

    @Transactional(readOnly = true)
    public SaleOrder findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<SaleOrder> findOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByOrderDateAscIdAsc(status);
    }

    @Transactional(readOnly = true)
    public List<SaleOrder> findPaidOrdersBetween(LocalDate startDate, LocalDate endDate) {
        return findOrdersBetweenDatesAndStatus(startDate, endDate, OrderStatus.PAID);
    }

    @Transactional(readOnly = true)
    public BigDecimal getPaidSalesTotalForDate(LocalDate date) {
        LocalDate effectiveDate = (date != null ? date : LocalDate.now());

        return findOrdersForDateAndStatus(effectiveDate, OrderStatus.PAID).stream()
                .map(SaleOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<PossibleOrderSuggestion> getPossibleOrderSuggestions(LocalDate referenceDate, int maxResults) {
        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now();
        LocalDate startDate = today.minusDays(SUGGESTION_LOOKBACK_DAYS);

        List<Client> activeClients = clientRepository.findByActiveTrueOrderByNameAsc();
        List<SaleOrder> historicalOrders = orderRepository.findHistoricalOrdersForSuggestionBetween(
                startDate,
                today,
                List.of(OrderStatus.PAID, OrderStatus.CREDIT)
        );

        List<Long> clientIdsWithOrderToday = orderRepository.findDistinctClientIdsWithOrderOnDate(
                today,
                List.of(OrderStatus.REQUESTED, OrderStatus.PAID, OrderStatus.CREDIT)
        );

        Map<Long, Client> activeClientMap = new LinkedHashMap<>();
        for (Client client : activeClients) {
            if (client.getId() != null) {
                activeClientMap.put(client.getId(), client);
            }
        }

        Map<Long, LinkedHashSet<LocalDate>> purchaseDatesByClientId = new LinkedHashMap<>();

        for (SaleOrder order : historicalOrders) {
            if (order.getClient() == null || order.getClient().getId() == null || order.getOrderDate() == null) {
                continue;
            }

            Long clientId = order.getClient().getId();
            if (!activeClientMap.containsKey(clientId)) {
                continue;
            }

            purchaseDatesByClientId
                    .computeIfAbsent(clientId, key -> new LinkedHashSet<>())
                    .add(order.getOrderDate());
        }

        List<PossibleOrderSuggestion> suggestions = new ArrayList<>();

        for (Client client : activeClients) {
            Long clientId = client.getId();
            if (clientId == null) {
                continue;
            }

            if (clientIdsWithOrderToday.contains(clientId)) {
                continue;
            }

            LinkedHashSet<LocalDate> dateSet = purchaseDatesByClientId.get(clientId);
            if (dateSet == null || dateSet.isEmpty()) {
                continue;
            }

            List<LocalDate> purchaseDates = new ArrayList<>(dateSet);
            purchaseDates.sort(LocalDate::compareTo);

            LocalDate lastOrderDate = purchaseDates.get(purchaseDates.size() - 1);
            long daysSinceLastOrder = Math.max(ChronoUnit.DAYS.between(lastOrderDate, today), 0);
            int purchaseDayCount = purchaseDates.size();

            int averageIntervalDays = calculateAverageIntervalDaysRecent(purchaseDates, daysSinceLastOrder);
            LocalDate expectedNextOrderDate = lastOrderDate.plusDays(averageIntervalDays);
            long overdueDays = expectedNextOrderDate.isBefore(today)
                    ? ChronoUnit.DAYS.between(expectedNextOrderDate, today)
                    : 0;

            int probabilityPercent = calculateProbabilityPercent(
                    purchaseDates,
                    daysSinceLastOrder,
                    averageIntervalDays,
                    overdueDays
            );

            boolean shouldSuggest = shouldSuggestClient(
                    purchaseDayCount,
                    daysSinceLastOrder,
                    averageIntervalDays,
                    overdueDays,
                    probabilityPercent
            );

            if (!shouldSuggest) {
                continue;
            }

            SuggestionStatus suggestionStatus = resolveSuggestionStatus(
                    purchaseDayCount,
                    daysSinceLastOrder,
                    averageIntervalDays,
                    overdueDays,
                    probabilityPercent
            );

            String profileName = client.getProfile() != null ? client.getProfile().getName() : "-";

            suggestions.add(new PossibleOrderSuggestion(
                    clientId,
                    client.getName(),
                    client.getPhone(),
                    profileName,
                    lastOrderDate,
                    daysSinceLastOrder,
                    purchaseDayCount,
                    averageIntervalDays,
                    expectedNextOrderDate,
                    overdueDays,
                    probabilityPercent,
                    suggestionStatus.label(),
                    suggestionStatus.statusClass(),
                    suggestionStatus.actionLabel()
            ));
        }

        suggestions.sort(
                Comparator.comparingInt(PossibleOrderSuggestion::getProbabilityPercent).reversed()
                        .thenComparingLong(PossibleOrderSuggestion::getOverdueDays).reversed()
                        .thenComparingLong(PossibleOrderSuggestion::getDaysSinceLastOrder).reversed()
                        .thenComparing(PossibleOrderSuggestion::getClientName, String.CASE_INSENSITIVE_ORDER)
        );

        if (maxResults > 0 && suggestions.size() > maxResults) {
            return new ArrayList<>(suggestions.subList(0, maxResults));
        }

        return suggestions;
    }

    private int calculateAverageIntervalDaysRecent(List<LocalDate> purchaseDates, long daysSinceLastOrder) {
        if (purchaseDates == null || purchaseDates.isEmpty()) {
            return 7;
        }

        if (purchaseDates.size() == 1) {
            if (daysSinceLastOrder <= 0) {
                return 5;
            }

            return (int) Math.max(3, Math.min(daysSinceLastOrder, 10));
        }

        long total = 0;
        int intervals = 0;

        for (int i = 1; i < purchaseDates.size(); i++) {
            long diff = ChronoUnit.DAYS.between(purchaseDates.get(i - 1), purchaseDates.get(i));
            total += Math.max(diff, 1);
            intervals++;
        }

        if (intervals == 0) {
            return 5;
        }

        double average = (double) total / intervals;
        return Math.max(1, (int) Math.round(average));
    }

    private int calculateProbabilityPercent(
            List<LocalDate> purchaseDates,
            long daysSinceLastOrder,
            int averageIntervalDays,
            long overdueDays
    ) {
        int purchaseDayCount = purchaseDates.size();

        if (purchaseDayCount == 1) {
            if (daysSinceLastOrder >= 7) {
                return 78;
            }
            if (daysSinceLastOrder >= 5) {
                return 66;
            }
            if (daysSinceLastOrder >= 3) {
                return 52;
            }
            return 28;
        }

        double regularityScore = calculateRegularityScore(purchaseDates);
        double frequencyScore = Math.min(1.0, purchaseDayCount / 5.0);
        double cycleProgress = averageIntervalDays > 0 ? (double) daysSinceLastOrder / averageIntervalDays : 0.0;
        double progressScore = Math.min(cycleProgress, 1.35);

        int probabilityPercent = (int) Math.round(
                (progressScore / 1.35) * 60.0 +
                regularityScore * 20.0 +
                frequencyScore * 20.0
        );

        if (overdueDays > 0) {
            probabilityPercent = Math.min(probabilityPercent + 12, 99);
        } else if (daysSinceLastOrder >= Math.max(1, averageIntervalDays - 1)) {
            probabilityPercent = Math.min(probabilityPercent + 8, 99);
        }

        return Math.max(20, Math.min(probabilityPercent, 99));
    }

    private boolean shouldSuggestClient(
            int purchaseDayCount,
            long daysSinceLastOrder,
            int averageIntervalDays,
            long overdueDays,
            int probabilityPercent
    ) {
        if (purchaseDayCount == 1) {
            return daysSinceLastOrder >= 3;
        }

        if (purchaseDayCount == 2) {
            return overdueDays > 0
                    || daysSinceLastOrder >= Math.max(1, averageIntervalDays - 1)
                    || probabilityPercent >= 45;
        }

        return overdueDays > 0
                || daysSinceLastOrder >= Math.max(1, averageIntervalDays - 1)
                || probabilityPercent >= 45;
    }

    private SuggestionStatus resolveSuggestionStatus(
            int purchaseDayCount,
            long daysSinceLastOrder,
            int averageIntervalDays,
            long overdueDays,
            int probabilityPercent
    ) {
        if (overdueDays > 0) {
            return new SuggestionStatus(
                    "Atrasado +" + overdueDays + "d",
                    "danger",
                    "Llamar hoy"
            );
        }

        if (purchaseDayCount == 1 && daysSinceLastOrder >= 3) {
            return new SuggestionStatus(
                    "Seguimiento",
                    "warning",
                    "Contactar"
            );
        }

        if (daysSinceLastOrder >= Math.max(1, averageIntervalDays - 1) || probabilityPercent >= 70) {
            return new SuggestionStatus(
                    "Toca pronto",
                    "warning",
                    "Seguimiento"
            );
        }

        return new SuggestionStatus(
                "Monitorear",
                "secondary",
                "Monitorear"
            );
    }

    private double calculateRegularityScore(List<LocalDate> purchaseDates) {
        if (purchaseDates == null || purchaseDates.size() <= 1) {
            return 0.45;
        }

        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < purchaseDates.size(); i++) {
            long diff = ChronoUnit.DAYS.between(purchaseDates.get(i - 1), purchaseDates.get(i));
            intervals.add(Math.max(diff, 1));
        }

        if (intervals.isEmpty()) {
            return 0.45;
        }

        double avg = intervals.stream().mapToLong(Long::longValue).average().orElse(1.0);
        if (avg <= 0) {
            return 0.45;
        }

        double variance = 0.0;
        for (Long interval : intervals) {
            double delta = interval - avg;
            variance += delta * delta;
        }
        variance = variance / intervals.size();

        double stdDev = Math.sqrt(variance);
        double score = 1.0 - Math.min(stdDev / (avg * 1.25), 1.0);

        return Math.max(0.35, Math.min(score, 1.0));
    }

    private record SuggestionStatus(
            String label,
            String statusClass,
            String actionLabel
    ) {
    }
}