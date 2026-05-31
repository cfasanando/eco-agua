package com.ecoamazonas.eco_agua.order;

import java.time.LocalDate;

public class OrderChangedEvent {

    private final Long orderId;
    private final LocalDate orderDate;

    public OrderChangedEvent(Long orderId, LocalDate orderDate) {
        this.orderId = orderId;
        this.orderDate = orderDate;
    }

    public Long getOrderId() {
        return orderId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }
}
