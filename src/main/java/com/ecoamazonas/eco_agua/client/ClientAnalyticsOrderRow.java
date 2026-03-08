package com.ecoamazonas.eco_agua.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ClientAnalyticsOrderRow {

    private final Long orderId;
    private final Integer orderNumber;
    private final LocalDate orderDate;
    private final String statusLabel;
    private final String statusBadgeClass;
    private final BigDecimal totalAmount;
    private final BigDecimal estimatedCost;
    private final BigDecimal estimatedProfit;
    private final Integer borrowedBottles;
    private final String deliveryPerson;
    private final String comment;

    public ClientAnalyticsOrderRow(
            Long orderId,
            Integer orderNumber,
            LocalDate orderDate,
            String statusLabel,
            String statusBadgeClass,
            BigDecimal totalAmount,
            BigDecimal estimatedCost,
            BigDecimal estimatedProfit,
            Integer borrowedBottles,
            String deliveryPerson,
            String comment
    ) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.statusLabel = statusLabel;
        this.statusBadgeClass = statusBadgeClass;
        this.totalAmount = totalAmount;
        this.estimatedCost = estimatedCost;
        this.estimatedProfit = estimatedProfit;
        this.borrowedBottles = borrowedBottles;
        this.deliveryPerson = deliveryPerson;
        this.comment = comment;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public BigDecimal getEstimatedProfit() {
        return estimatedProfit;
    }

    public Integer getBorrowedBottles() {
        return borrowedBottles;
    }

    public String getDeliveryPerson() {
        return deliveryPerson;
    }

    public String getComment() {
        return comment;
    }
}
