package com.ecoamazonas.eco_agua.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SalesChannelReportRow {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final SalesChannel channel;
    private int orderCount;
    private int paidOrderCount;
    private int creditOrderCount;
    private BigDecimal totalSalesAmount = ZERO;
    private BigDecimal paidAmount = ZERO;
    private BigDecimal pendingAmount = ZERO;

    public SalesChannelReportRow(SalesChannel channel) {
        this.channel = channel;
    }

    public void addOrder(SaleOrder order) {
        if (order == null) {
            return;
        }

        orderCount++;

        if (order.getStatus() == OrderStatus.PAID) {
            paidOrderCount++;
        } else if (order.getStatus() == OrderStatus.CREDIT) {
            creditOrderCount++;
        }

        BigDecimal orderTotal = safeAmount(order.getTotalAmount());
        BigDecimal orderPaid = resolvePaidAmount(order, orderTotal);
        BigDecimal orderPending = order.getStatus() == OrderStatus.CREDIT
                ? safeAmount(order.getPendingAmount())
                : ZERO;

        totalSalesAmount = totalSalesAmount.add(orderTotal).setScale(2, RoundingMode.HALF_UP);
        paidAmount = paidAmount.add(orderPaid).setScale(2, RoundingMode.HALF_UP);
        pendingAmount = pendingAmount.add(orderPending).setScale(2, RoundingMode.HALF_UP);
    }

    public SalesChannel getChannel() {
        return channel;
    }

    public String getChannelCode() {
        return channel != null ? channel.name() : "UNKNOWN";
    }

    public String getChannelLabel() {
        return channel != null ? channel.getLabel() : "Sin canal";
    }

    public int getOrderCount() {
        return orderCount;
    }

    public int getPaidOrderCount() {
        return paidOrderCount;
    }

    public int getCreditOrderCount() {
        return creditOrderCount;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public BigDecimal getAverageTicketAmount() {
        if (orderCount <= 0) {
            return ZERO;
        }

        return totalSalesAmount.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
    }

    public boolean hasMovement() {
        return orderCount > 0;
    }

    public boolean hasPendingAmount() {
        return pendingAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getReading() {
        if (!hasMovement()) {
            return "Sin movimiento";
        }

        if (hasPendingAmount()) {
            return "Revisar fiados";
        }

        if (creditOrderCount > 0) {
            return "Fiados cobrados";
        }

        return "Cobro cerrado";
    }

    public String getReadingBadgeClass() {
        if (!hasMovement()) {
            return "text-bg-light";
        }

        if (hasPendingAmount()) {
            return "text-bg-warning";
        }

        return "text-bg-success";
    }

    private BigDecimal resolvePaidAmount(SaleOrder order, BigDecimal orderTotal) {
        if (order == null) {
            return ZERO;
        }

        if (order.getStatus() == OrderStatus.PAID) {
            return orderTotal;
        }

        return safeAmount(order.getPaidAmount());
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
