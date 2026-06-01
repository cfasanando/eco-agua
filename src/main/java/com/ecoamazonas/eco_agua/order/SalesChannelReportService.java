package com.ecoamazonas.eco_agua.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesChannelReportService {

    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderPaymentRepository saleOrderPaymentRepository;

    public SalesChannelReportService(
            SaleOrderRepository saleOrderRepository,
            SaleOrderPaymentRepository saleOrderPaymentRepository
    ) {
        this.saleOrderRepository = saleOrderRepository;
        this.saleOrderPaymentRepository = saleOrderPaymentRepository;
    }

    @Transactional(readOnly = true)
    public SalesChannelReportSummary buildReport(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<SaleOrder> orders = saleOrderRepository.findByOrderDateBetween(startDate, endDate).stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CREDIT)
                .toList();

        attachPayments(orders);

        Map<SalesChannel, SalesChannelReportRow> rowsByChannel = new LinkedHashMap<>();
        Arrays.stream(SalesChannel.values())
                .forEach(channel -> rowsByChannel.put(channel, new SalesChannelReportRow(channel)));

        for (SaleOrder order : orders) {
            SalesChannel channel = order.getSalesChannel() != null ? order.getSalesChannel() : SalesChannel.WHATSAPP;
            rowsByChannel
                    .computeIfAbsent(channel, SalesChannelReportRow::new)
                    .addOrder(order);
        }

        List<SalesChannelReportRow> rows = rowsByChannel.values().stream()
                .sorted(Comparator
                        .comparing(SalesChannelReportRow::hasMovement).reversed()
                        .thenComparing(SalesChannelReportRow::getTotalSalesAmount, Comparator.reverseOrder())
                        .thenComparing(SalesChannelReportRow::getChannelLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new SalesChannelReportSummary(rows);
    }

    private void attachPayments(List<SaleOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }

        List<Long> orderIds = orders.stream()
                .map(SaleOrder::getId)
                .filter(id -> id != null)
                .toList();

        if (orderIds.isEmpty()) {
            return;
        }

        List<SaleOrderPayment> payments = saleOrderPaymentRepository
                .findBySaleOrderIdInOrderBySaleOrderIdAscPaymentDateAscIdAsc(orderIds);

        Map<Long, List<SaleOrderPayment>> paymentsByOrderId = new LinkedHashMap<>();
        for (SaleOrderPayment payment : payments) {
            Long orderId = payment.getSaleOrder() != null ? payment.getSaleOrder().getId() : null;
            if (orderId == null) {
                continue;
            }

            paymentsByOrderId
                    .computeIfAbsent(orderId, key -> new ArrayList<>())
                    .add(payment);
        }

        for (SaleOrder order : orders) {
            order.getPayments().clear();
            paymentsByOrderId
                    .getOrDefault(order.getId(), List.of())
                    .forEach(order::addPayment);
        }
    }
}
