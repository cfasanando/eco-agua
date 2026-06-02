package com.ecoamazonas.eco_agua.delivery;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public String getDeliveryStatusLabel() {
        if (deliveryStatus == null) {
            return "Sin estado";
        }

        return switch (deliveryStatus) {
            case PENDING -> "Pendiente";
            case IN_ROUTE -> "En ruta";
            case DELIVERED -> "Entregado";
            case NOT_DELIVERED -> "No entregado";
            case RESCHEDULED -> "Reprogramado";
            case CANCELED -> "Cancelado";
        };
    }

    public String getDeliveryStatusBadgeClass() {
        if (deliveryStatus == null) {
            return "text-bg-secondary";
        }

        return switch (deliveryStatus) {
            case PENDING -> "text-bg-warning text-dark";
            case IN_ROUTE -> "text-bg-primary";
            case DELIVERED -> "text-bg-success";
            case NOT_DELIVERED -> "text-bg-danger";
            case RESCHEDULED -> "text-bg-info text-dark";
            case CANCELED -> "text-bg-secondary";
        };
    }

    public boolean isPending() {
        return deliveryStatus == DeliveryStatus.PENDING;
    }

    public boolean isInRoute() {
        return deliveryStatus == DeliveryStatus.IN_ROUTE;
    }

    public boolean isDelivered() {
        return deliveryStatus == DeliveryStatus.DELIVERED;
    }

    public boolean isClosedForDelivery() {
        return deliveryStatus == DeliveryStatus.DELIVERED || deliveryStatus == DeliveryStatus.CANCELED;
    }

    public boolean hasPhone() {
        return normalizePhone(phone) != null;
    }

    public String getWhatsappUrl() {
        String normalizedPhone = normalizePhone(phone);
        if (normalizedPhone == null) {
            return null;
        }

        String message = "Hola, te escribimos para coordinar la entrega de tu pedido #"
                + (orderNumber != null ? orderNumber : orderId)
                + ".";
        return "https://wa.me/" + normalizedPhone + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
    }

    public String getAddressSummary() {
        boolean hasAddress = address != null && !address.isBlank();
        boolean hasReference = reference != null && !reference.isBlank();

        if (!hasAddress && !hasReference) {
            return "-";
        }

        if (hasAddress && hasReference) {
            return address + " - Ref.: " + reference;
        }

        return hasAddress ? address : "Ref.: " + reference;
    }

    private String normalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }

        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() == 9) {
            return "51" + digits;
        }

        return digits;
    }
}
