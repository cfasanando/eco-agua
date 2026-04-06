package com.ecoamazonas.eco_agua.container;

import com.ecoamazonas.eco_agua.client.Client;
import com.ecoamazonas.eco_agua.order.SaleOrder;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_container_movement")
public class ClientContainerMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "sale_order_id")
    private Long saleOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private ContainerMovementType movementType;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "observation", length = 255)
    private String observation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id", insertable = false, updatable = false)
    private SaleOrder saleOrder;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (movementDate == null) {
            movementDate = LocalDate.now();
        }
        if (quantity == null) {
            quantity = 0;
        }
    }

    @Transient
    public int getSignedQuantity() {
        int value = quantity != null ? quantity : 0;

        return switch (movementType) {
            case LOAN, ADJUSTMENT_OUT -> value;
            case RETURN, ADJUSTMENT_IN -> -value;
        };
    }

    public Long getId() {
        return id;
    }

    public Long getClientId() {
        return clientId;
    }

    public Long getSaleOrderId() {
        return saleOrderId;
    }

    public ContainerMovementType getMovementType() {
        return movementType;
    }

    public LocalDate getMovementDate() {
        return movementDate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getObservation() {
        return observation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Client getClient() {
        return client;
    }

    public SaleOrder getSaleOrder() {
        return saleOrder;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public void setSaleOrderId(Long saleOrderId) {
        this.saleOrderId = saleOrderId;
    }

    public void setMovementType(ContainerMovementType movementType) {
        this.movementType = movementType;
    }

    public void setMovementDate(LocalDate movementDate) {
        this.movementDate = movementDate;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setClient(Client client) {
        this.client = client;
        this.clientId = client != null ? client.getId() : null;
    }

    public void setSaleOrder(SaleOrder saleOrder) {
        this.saleOrder = saleOrder;
        this.saleOrderId = saleOrder != null ? saleOrder.getId() : null;
    }
}
