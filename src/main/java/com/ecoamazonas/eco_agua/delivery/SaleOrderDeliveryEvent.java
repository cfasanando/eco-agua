package com.ecoamazonas.eco_agua.delivery;

import com.ecoamazonas.eco_agua.order.SaleOrder;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale_order_delivery_event")
public class SaleOrderDeliveryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_order_id", nullable = false)
    private Long saleOrderId;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private DeliveryEventType eventType;

    @Column(name = "observation", length = 255)
    private String observation;

    @Column(name = "incident_reason", length = 80)
    private String incidentReason;

    @Column(name = "proof_reference", length = 255)
    private String proofReference;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "containers_delivered_snapshot", nullable = false)
    private Integer containersDeliveredSnapshot = 0;

    @Column(name = "containers_returned_snapshot", nullable = false)
    private Integer containersReturnedSnapshot = 0;

    @Column(name = "delivery_person_snapshot", length = 200)
    private String deliveryPersonSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id", insertable = false, updatable = false)
    private SaleOrder saleOrder;

    @PrePersist
    protected void onCreate() {
        if (eventDate == null) {
            eventDate = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (containersDeliveredSnapshot == null) {
            containersDeliveredSnapshot = 0;
        }
        if (containersReturnedSnapshot == null) {
            containersReturnedSnapshot = 0;
        }
    }

    public Long getId() { return id; }
    public Long getSaleOrderId() { return saleOrderId; }
    public LocalDateTime getEventDate() { return eventDate; }
    public DeliveryEventType getEventType() { return eventType; }
    public String getObservation() { return observation; }
    public String getIncidentReason() { return incidentReason; }
    public String getProofReference() { return proofReference; }
    public String getPaymentMethod() { return paymentMethod; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public String getPaymentReference() { return paymentReference; }
    public boolean hasPaymentInfo() { return paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0; }
    public boolean hasProofInfo() { return proofReference != null && !proofReference.isBlank(); }
    public boolean hasIncidentReason() { return incidentReason != null && !incidentReason.isBlank(); }
    public Integer getContainersDeliveredSnapshot() { return containersDeliveredSnapshot; }
    public Integer getContainersReturnedSnapshot() { return containersReturnedSnapshot; }
    public String getDeliveryPersonSnapshot() { return deliveryPersonSnapshot; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public SaleOrder getSaleOrder() { return saleOrder; }

    public void setId(Long id) { this.id = id; }
    public void setSaleOrderId(Long saleOrderId) { this.saleOrderId = saleOrderId; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    public void setEventType(DeliveryEventType eventType) { this.eventType = eventType; }
    public void setObservation(String observation) { this.observation = observation; }
    public void setIncidentReason(String incidentReason) { this.incidentReason = incidentReason; }
    public void setProofReference(String proofReference) { this.proofReference = proofReference; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public void setContainersDeliveredSnapshot(Integer containersDeliveredSnapshot) { this.containersDeliveredSnapshot = containersDeliveredSnapshot; }
    public void setContainersReturnedSnapshot(Integer containersReturnedSnapshot) { this.containersReturnedSnapshot = containersReturnedSnapshot; }
    public void setDeliveryPersonSnapshot(String deliveryPersonSnapshot) { this.deliveryPersonSnapshot = deliveryPersonSnapshot; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setSaleOrder(SaleOrder saleOrder) {
        this.saleOrder = saleOrder;
        this.saleOrderId = saleOrder != null ? saleOrder.getId() : null;
    }
}
