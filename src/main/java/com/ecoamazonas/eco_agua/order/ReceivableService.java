package com.ecoamazonas.eco_agua.order;

import com.ecoamazonas.eco_agua.accounting.service.AccountingAutoJournalEntryService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReceivableService {

    private static final Logger log = LoggerFactory.getLogger(ReceivableService.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderPaymentRepository saleOrderPaymentRepository;
    private final AccountingAutoJournalEntryService accountingAutoJournalEntryService;

    public ReceivableService(
            SaleOrderRepository saleOrderRepository,
            SaleOrderPaymentRepository saleOrderPaymentRepository,
            AccountingAutoJournalEntryService accountingAutoJournalEntryService
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderPaymentRepository = saleOrderPaymentRepository;
        this.accountingAutoJournalEntryService = accountingAutoJournalEntryService;
    }

    @Transactional
    public SaleOrder ensureCreditDefaults(Long orderId) {
        SaleOrder order = findDetailedOrder(orderId);

        if (order.getStatus() == OrderStatus.CREDIT
                && order.getDueDate() == null
                && order.getOrderDate() != null) {
            order.setDueDate(order.getOrderDate().plusDays(7));
            saleOrderRepository.save(order);
        }

        return order;
    }

    @Transactional
    public SaleOrder updateDueDate(Long orderId, LocalDate dueDate) {
        SaleOrder order = findDetailedOrder(orderId);

        if (order.getStatus() != OrderStatus.CREDIT && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("Only credit or paid orders can define a due date.");
        }

        order.setDueDate(dueDate);
        return saleOrderRepository.save(order);
    }

    @Transactional
    public SaleOrderPayment registerPayment(
            Long orderId,
            LocalDate paymentDate,
            BigDecimal amount,
            String paymentMethod,
            String reference,
            String observation
    ) {
        SaleOrder order = findDetailedOrder(orderId);

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new IllegalArgumentException("Canceled orders cannot receive payments.");
        }

        if (order.getStatus() == OrderStatus.QUOTED || order.getStatus() == OrderStatus.REQUESTED) {
            throw new IllegalArgumentException("Quoted or requested orders must be confirmed as credit before receiving payments.");
        }

        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        amount = amount.setScale(2, RoundingMode.HALF_UP);

        BigDecimal pendingBefore = safeAmount(order.getPendingAmount());

        if (pendingBefore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("This order has no pending balance.");
        }

        if (amount.compareTo(pendingBefore) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed pending balance.");
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("Payment method is required.");
        }

        SaleOrderPayment payment = new SaleOrderPayment();
        payment.setSaleOrder(order);
        payment.setPaymentDate(paymentDate);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod.trim());
        payment.setReference(reference != null ? reference.trim() : null);
        payment.setObservation(observation != null ? observation.trim() : null);

        SaleOrderPayment savedPayment = saleOrderPaymentRepository.save(payment);

        List<SaleOrderPayment> refreshedPayments =
                saleOrderPaymentRepository.findBySaleOrderIdOrderByPaymentDateAscIdAsc(orderId);

        replacePayments(order, refreshedPayments);

        BigDecimal pendingAfter = safeAmount(order.getPendingAmount());

        if (pendingAfter.compareTo(BigDecimal.ZERO) <= 0) {
            order.setStatus(OrderStatus.PAID);
        } else if (order.getStatus() != OrderStatus.CREDIT) {
            order.setStatus(OrderStatus.CREDIT);
        }

        saleOrderRepository.save(order);
        generateAccountingDraftForCreditCollection(savedPayment);
        return savedPayment;
    }


    private void generateAccountingDraftForCreditCollection(SaleOrderPayment payment) {
        try {
            accountingAutoJournalEntryService.generateForCreditCollection(payment);
        } catch (Exception ex) {
            log.warn(
                    "Credit collection accounting draft was not generated. paymentId={}, reason={}",
                    payment != null ? payment.getId() : null,
                    ex.getMessage()
            );
        }
    }

    @Transactional
    public SaleOrder findDetailedOrder(Long orderId) {
        SaleOrder order = saleOrderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        List<SaleOrderPayment> payments =
                saleOrderPaymentRepository.findBySaleOrderIdOrderByPaymentDateAscIdAsc(orderId);

        replacePayments(order, payments);

        return order;
    }

    @Transactional
    public List<SaleOrder> findOpenCreditOrders(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            LocalDate today = LocalDate.now();
            startDate = today;
            endDate = today;
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

        List<SaleOrder> orders = saleOrderRepository
                .findCreditOrdersWithClientByOrderDateBetweenAndStatusOrderByOrderDateDescIdDesc(
                        startDate,
                        endDate,
                        OrderStatus.CREDIT
                );

        attachPayments(orders);
        orders.forEach(this::normalizeCreditDefaultsWithoutSaving);

        return orders;
    }

    public BigDecimal calculatePendingTotal(List<SaleOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return ZERO;
        }

        return orders.stream()
                .map(SaleOrder::getPendingAmount)
                .map(this::safeAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }


    public ReceivableSummary buildSummary(List<SaleOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return new ReceivableSummary(0, 0, 0, 0, ZERO, ZERO, ZERO, ZERO);
        }

        LocalDate today = LocalDate.now();
        Set<Long> clientIds = new HashSet<>();
        int overdueOrderCount = 0;
        int dueTodayOrderCount = 0;
        BigDecimal totalPendingAmount = ZERO;
        BigDecimal overduePendingAmount = ZERO;
        BigDecimal dueTodayPendingAmount = ZERO;
        BigDecimal currentPendingAmount = ZERO;

        for (SaleOrder order : orders) {
            if (order == null) {
                continue;
            }

            if (order.getClient() != null && order.getClient().getId() != null) {
                clientIds.add(order.getClient().getId());
            }

            BigDecimal pendingAmount = safeAmount(order.getPendingAmount());
            totalPendingAmount = totalPendingAmount.add(pendingAmount);

            if (order.getOverdueDays() > 0) {
                overdueOrderCount++;
                overduePendingAmount = overduePendingAmount.add(pendingAmount);
            } else if (order.getDueDate() != null && order.getDueDate().isEqual(today)) {
                dueTodayOrderCount++;
                dueTodayPendingAmount = dueTodayPendingAmount.add(pendingAmount);
            } else {
                currentPendingAmount = currentPendingAmount.add(pendingAmount);
            }
        }

        return new ReceivableSummary(
                orders.size(),
                clientIds.size(),
                overdueOrderCount,
                dueTodayOrderCount,
                totalPendingAmount.setScale(2, RoundingMode.HALF_UP),
                overduePendingAmount.setScale(2, RoundingMode.HALF_UP),
                dueTodayPendingAmount.setScale(2, RoundingMode.HALF_UP),
                currentPendingAmount.setScale(2, RoundingMode.HALF_UP)
        );
    }

    public List<ReceivableClientSummary> buildClientSummaries(List<SaleOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        Map<Long, ClientReceivableAggregation> aggregations = new LinkedHashMap<>();

        for (SaleOrder order : orders) {
            if (order == null || order.getClient() == null || order.getClient().getId() == null) {
                continue;
            }

            Long clientId = order.getClient().getId();
            ClientReceivableAggregation aggregation = aggregations.computeIfAbsent(
                    clientId,
                    key -> new ClientReceivableAggregation(
                            clientId,
                            order.getClient().getName(),
                            order.getClient().getPhone()
                    )
            );

            aggregation.add(order);
        }

        return aggregations.values().stream()
                .map(ClientReceivableAggregation::toSummary)
                .sorted(Comparator
                        .comparing(ReceivableClientSummary::getPendingAmount, Comparator.reverseOrder())
                        .thenComparing(ReceivableClientSummary::getClientName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public String buildCollectionWhatsappUrl(SaleOrder order) {
        if (order == null || order.getClient() == null) {
            return null;
        }

        return buildWhatsappUrl(order.getClient().getPhone(), buildCollectionMessage(order));
    }

    public String buildCollectionMessage(SaleOrder order) {
        if (order == null) {
            return "Hola, te escribimos para coordinar tu saldo pendiente.";
        }

        String orderNumber = order.getOrderNumber() != null ? order.getOrderNumber().toString() : String.valueOf(order.getId());
        String pendingAmount = safeAmount(order.getPendingAmount()).toPlainString();

        return "Hola, te escribimos para coordinar el saldo pendiente de S/. "
                + pendingAmount
                + " del pedido #"
                + orderNumber
                + ". ¿Podemos revisarlo por este medio?";
    }

    private void attachPayments(List<SaleOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }

        List<Long> orderIds = orders.stream()
                .map(SaleOrder::getId)
                .toList();

        List<SaleOrderPayment> allPayments =
                saleOrderPaymentRepository.findBySaleOrderIdInOrderBySaleOrderIdAscPaymentDateAscIdAsc(orderIds);

        Map<Long, List<SaleOrderPayment>> paymentsByOrderId = new LinkedHashMap<>();
        for (SaleOrderPayment payment : allPayments) {
            Long saleOrderId = payment.getSaleOrder() != null ? payment.getSaleOrder().getId() : null;
            if (saleOrderId == null) {
                continue;
            }
            paymentsByOrderId
                    .computeIfAbsent(saleOrderId, key -> new ArrayList<>())
                    .add(payment);
        }

        for (SaleOrder order : orders) {
            List<SaleOrderPayment> orderPayments =
                    paymentsByOrderId.getOrDefault(order.getId(), new ArrayList<>());
            replacePayments(order, orderPayments);
        }
    }

    private void replacePayments(SaleOrder order, List<SaleOrderPayment> payments) {
        if (order == null) {
            return;
        }

        order.getPayments().clear();

        if (payments != null) {
            for (SaleOrderPayment payment : payments) {
                order.addPayment(payment);
            }
        }
    }

    private void normalizeCreditDefaultsWithoutSaving(SaleOrder order) {
        if (order != null
                && order.getStatus() == OrderStatus.CREDIT
                && order.getDueDate() == null
                && order.getOrderDate() != null) {
            order.setDueDate(order.getOrderDate().plusDays(7));
        }
    }


    private String buildWhatsappUrl(String phone, String message) {
        String normalizedPhone = normalizeWhatsappNumber(phone);
        if (normalizedPhone == null) {
            return null;
        }

        String safeMessage = message != null && !message.isBlank()
                ? message
                : "Hola, te escribimos para coordinar tu saldo pendiente.";

        return "https://wa.me/" + normalizedPhone + "?text=" + URLEncoder.encode(safeMessage, StandardCharsets.UTF_8);
    }

    private String normalizeWhatsappNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String digits = phone.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() == 9 && digits.startsWith("9")) {
            return "51" + digits;
        }

        if (digits.length() >= 10) {
            return digits;
        }

        return null;
    }

    private class ClientReceivableAggregation {

        private final Long clientId;
        private final String clientName;
        private final String phone;
        private final Map<String, Integer> channelCounts = new LinkedHashMap<>();
        private int orderCount;
        private int overdueOrderCount;
        private BigDecimal pendingAmount = ZERO;
        private LocalDate oldestOrderDate;
        private LocalDate nearestDueDate;

        private ClientReceivableAggregation(Long clientId, String clientName, String phone) {
            this.clientId = clientId;
            this.clientName = clientName != null && !clientName.isBlank() ? clientName : "Cliente sin nombre";
            this.phone = phone;
        }

        private void add(SaleOrder order) {
            orderCount++;
            pendingAmount = pendingAmount.add(safeAmount(order.getPendingAmount()));

            if (order.getOverdueDays() > 0) {
                overdueOrderCount++;
            }

            if (order.getOrderDate() != null && (oldestOrderDate == null || order.getOrderDate().isBefore(oldestOrderDate))) {
                oldestOrderDate = order.getOrderDate();
            }

            if (order.getDueDate() != null && (nearestDueDate == null || order.getDueDate().isBefore(nearestDueDate))) {
                nearestDueDate = order.getDueDate();
            }

            String channelLabel = order.getSalesChannel() != null ? order.getSalesChannel().getLabel() : "WhatsApp";
            channelCounts.merge(channelLabel, 1, Integer::sum);
        }

        private ReceivableClientSummary toSummary() {
            BigDecimal normalizedPending = pendingAmount.setScale(2, RoundingMode.HALF_UP);
            return new ReceivableClientSummary(
                    clientId,
                    clientName,
                    phone,
                    orderCount,
                    overdueOrderCount,
                    normalizedPending,
                    oldestOrderDate,
                    nearestDueDate,
                    resolveMainSalesChannelLabel(),
                    buildWhatsappUrl(phone, buildClientCollectionMessage(clientName, normalizedPending))
            );
        }

        private String resolveMainSalesChannelLabel() {
            return channelCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("WhatsApp");
        }
    }

    private String buildClientCollectionMessage(String clientName, BigDecimal pendingAmount) {
        String amount = safeAmount(pendingAmount).toPlainString();
        return "Hola"
                + (clientName != null && !clientName.isBlank() ? " " + clientName : "")
                + ", te escribimos para coordinar tu saldo pendiente de S/. "
                + amount
                + ". ¿Podemos revisarlo por este medio?";
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : ZERO;
    }
}