package com.ecoamazonas.eco_agua.delivery;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DeliveryDailyRow {
    private final Long orderId;
    private final Integer orderNumber;
    private final LocalDate orderDate;
    private final String clientName;
    private final String phone;
    private final String address;
    private final String reference;
    private final String zoneName;
    private final String deliveryPerson;
    private final DeliveryStatus deliveryStatus;
    private final BigDecimal totalAmount;
    private final Integer borrowedBottles;

    public DeliveryDailyRow(Long orderId, Integer orderNumber, LocalDate orderDate, String clientName, String phone, String address, String reference, String zoneName, String deliveryPerson, DeliveryStatus deliveryStatus, BigDecimal totalAmount, Integer borrowedBottles) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.clientName = clientName;
        this.phone = phone;
        this.address = address;
        this.reference = reference;
        this.zoneName = zoneName;
        this.deliveryPerson = deliveryPerson;
        this.deliveryStatus = deliveryStatus;
        this.totalAmount = totalAmount;
        this.borrowedBottles = borrowedBottles;
    }

    public Long getOrderId() { return orderId; }
    public Integer getOrderNumber() { return orderNumber; }
    public LocalDate getOrderDate() { return orderDate; }
    public String getClientName() { return clientName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getReference() { return reference; }
    public String getZoneName() { return zoneName; }
    public String getDeliveryPerson() { return deliveryPerson; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Integer getBorrowedBottles() { return borrowedBottles; }
}
