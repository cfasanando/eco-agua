package com.ecoamazonas.eco_agua.reorder;

import com.ecoamazonas.eco_agua.client.Client;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reorder_follow_up")
public class ReorderFollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReorderFollowUpStatus status = ReorderFollowUpStatus.PENDING;

    @Column(name = "next_contact_date")
    private LocalDate nextContactDate;

    @Column(name = "observation", length = 255)
    private String observation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    private Client client;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        if (status == null) {
            status = ReorderFollowUpStatus.PENDING;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getClientId() { return clientId; }
    public LocalDate getReferenceDate() { return referenceDate; }
    public ReorderFollowUpStatus getStatus() { return status; }
    public LocalDate getNextContactDate() { return nextContactDate; }
    public String getObservation() { return observation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Client getClient() { return client; }

    public void setId(Long id) { this.id = id; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public void setReferenceDate(LocalDate referenceDate) { this.referenceDate = referenceDate; }
    public void setStatus(ReorderFollowUpStatus status) { this.status = status; }
    public void setNextContactDate(LocalDate nextContactDate) { this.nextContactDate = nextContactDate; }
    public void setObservation(String observation) { this.observation = observation; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setClient(Client client) {
        this.client = client;
        this.clientId = client != null ? client.getId() : null;
    }
}
