package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_finance_payment_obligation", indexes = {
        @Index(name = "idx_pf_obligation_user_due", columnList = "user_id,due_date"),
        @Index(name = "idx_pf_obligation_user_status", columnList = "user_id,status"),
        @Index(name = "idx_pf_obligation_user_group", columnList = "user_id,obligation_group"),
        @Index(name = "idx_pf_obligation_schedule_line", columnList = "schedule_line_id")
})
public class PersonalFinancePaymentObligation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private PersonalFinanceObligationSourceType sourceType = PersonalFinanceObligationSourceType.MANUAL;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "schedule_line_id")
    private Long scheduleLineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "obligation_group", nullable = false, length = 40)
    private PersonalFinanceObligationGroup group = PersonalFinanceObligationGroup.OTHER;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "amount_due", precision = 14, scale = 2, nullable = false)
    private BigDecimal amountDue = BigDecimal.ZERO;

    @Column(name = "amount_paid", precision = 14, scale = 2, nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 8)
    private PersonalFinanceCurrency currency = PersonalFinanceCurrency.PEN;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PersonalFinanceObligationStatus status = PersonalFinanceObligationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private PersonalFinancePriority priority = PersonalFinancePriority.MEDIUM;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    public BigDecimal pendingAmount() {
        BigDecimal pending = amountDue.subtract(amountPaid);
        return pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending;
    }

    public boolean isPaidLike() {
        return status == PersonalFinanceObligationStatus.PAID || pendingAmount().compareTo(BigDecimal.ZERO) == 0;
    }

    private void normalize() {
        if (sourceType == null) sourceType = PersonalFinanceObligationSourceType.MANUAL;
        if (group == null) group = PersonalFinanceObligationGroup.OTHER;
        if (amountDue == null) amountDue = BigDecimal.ZERO;
        if (amountPaid == null) amountPaid = BigDecimal.ZERO;
        if (currency == null) currency = PersonalFinanceCurrency.PEN;
        if (status == null) status = PersonalFinanceObligationStatus.PENDING;
        if (priority == null) priority = PersonalFinancePriority.MEDIUM;
        if (amountPaid.compareTo(BigDecimal.ZERO) < 0) amountPaid = BigDecimal.ZERO;
        if (amountDue.compareTo(BigDecimal.ZERO) < 0) amountDue = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public PersonalFinanceObligationSourceType getSourceType() { return sourceType; }
    public void setSourceType(PersonalFinanceObligationSourceType sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Long getScheduleLineId() { return scheduleLineId; }
    public void setScheduleLineId(Long scheduleLineId) { this.scheduleLineId = scheduleLineId; }
    public PersonalFinanceObligationGroup getGroup() { return group; }
    public void setGroup(PersonalFinanceObligationGroup group) { this.group = group; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public PersonalFinanceCurrency getCurrency() { return currency; }
    public void setCurrency(PersonalFinanceCurrency currency) { this.currency = currency; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public PersonalFinanceObligationStatus getStatus() { return status; }
    public void setStatus(PersonalFinanceObligationStatus status) { this.status = status; }
    public PersonalFinancePriority getPriority() { return priority; }
    public void setPriority(PersonalFinancePriority priority) { this.priority = priority; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
