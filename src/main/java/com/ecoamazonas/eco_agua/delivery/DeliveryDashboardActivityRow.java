package com.ecoamazonas.eco_agua.delivery;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.order.SaleOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeliveryDashboardActivityRow {
    private final Long orderId;
    private final Integer orderNumber;
    private final String clientName;
    private final LocalDateTime eventDate;
    private final DeliveryEventType eventType;
    private final String observation;
    private final String incidentReason;
    private final String proofReference;
    private final BigDecimal paymentAmount;
    private final String paymentMethod;
    private final String deliveryPerson;

    public DeliveryDashboardActivityRow(SaleOrderDeliveryEvent event) {
        SaleOrder order = event.getSaleOrder();
        Client client = order != null ? order.getClient() : null;
        this.orderId = order != null ? order.getId() : event.getSaleOrderId();
        this.orderNumber = order != null ? order.getOrderNumber() : null;
        this.clientName = client != null ? client.getName() : "Cliente no disponible";
        this.eventDate = event.getEventDate();
        this.eventType = event.getEventType();
        this.observation = event.getObservation();
        this.incidentReason = event.getIncidentReason();
        this.proofReference = event.getProofReference();
        this.paymentAmount = event.getPaymentAmount();
        this.paymentMethod = event.getPaymentMethod();
        this.deliveryPerson = event.getDeliveryPersonSnapshot();
    }

    public Long getOrderId() { return orderId; }
    public Integer getOrderNumber() { return orderNumber; }
    public String getClientName() { return clientName; }
    public LocalDateTime getEventDate() { return eventDate; }
    public DeliveryEventType getEventType() { return eventType; }
    public String getObservation() { return observation; }
    public String getIncidentReason() { return incidentReason; }
    public String getProofReference() { return proofReference; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getDeliveryPerson() { return deliveryPerson; }

    public String getEventTypeLabel() {
        if (eventType == null) {
            return "Actividad";
        }
        return switch (eventType) {
            case CREATED -> "Creado";
            case IN_ROUTE -> "En ruta";
            case DELIVERED -> "Entregado";
            case NOT_DELIVERED -> "No entregado";
            case RESCHEDULED -> "Reprogramado";
            case CANCELED -> "Cancelado";
            case PAYMENT -> "Cobro en ruta";
            case NOTE -> "Nota";
        };
    }

    public String getBadgeClass() {
        if (eventType == null) {
            return "text-bg-secondary";
        }
        return switch (eventType) {
            case CREATED, NOTE -> "text-bg-secondary";
            case IN_ROUTE -> "text-bg-primary";
            case DELIVERED -> "text-bg-success";
            case NOT_DELIVERED, CANCELED -> "text-bg-danger";
            case RESCHEDULED -> "text-bg-info text-dark";
            case PAYMENT -> "text-bg-warning text-dark";
        };
    }

    public boolean hasPaymentInfo() {
        return paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
