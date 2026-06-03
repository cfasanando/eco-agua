package com.ecoamazonas.eco_agua.accounting;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "accounting_period_close",
        uniqueConstraints = @UniqueConstraint(name = "uk_accounting_period_close_period", columnNames = {"period_year", "period_month"})
)
public class AccountingPeriodClose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountingPeriodCloseStatus status = AccountingPeriodCloseStatus.OPEN;

    @Column(length = 500)
    private String notes;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
    }

    public Integer getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(Integer periodMonth) {
        this.periodMonth = periodMonth;
    }

    public AccountingPeriodCloseStatus getStatus() {
        return status;
    }

    public void setStatus(AccountingPeriodCloseStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getReopenedAt() {
        return reopenedAt;
    }

    public void setReopenedAt(LocalDateTime reopenedAt) {
        this.reopenedAt = reopenedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Transient
    public boolean isClosed() {
        return AccountingPeriodCloseStatus.CLOSED.equals(status);
    }
}
