package com.ecoamazonas.eco_agua.order;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReceivableService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderPaymentRepository saleOrderPaymentRepository;

    public ReceivableService(
            SaleOrderRepository saleOrderRepository,
            SaleOrderPaymentRepository saleOrderPaymentRepository
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderPaymentRepository = saleOrderPaymentRepository;
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

        if (order.getStatus() == OrderStatus.REQUESTED) {
            throw new IllegalArgumentException("Requested orders must be confirmed as credit before receiving payments.");
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

        saleOrderPaymentRepository.save(payment);

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
        return payment;
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

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : ZERO;
    }
}