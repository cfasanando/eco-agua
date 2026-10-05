package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "personal_finance_reserved_fund", indexes = {
        @Index(name = "idx_pf_reserved_fund_user_status", columnList = "user_id,status"),
        @Index(name = "idx_pf_reserved_fund_user_currency", columnList = "user_id,currency"),
        @Index(name = "idx_pf_reserved_fund_obligation", columnList = "obligation_id"),
        @Index(name = "idx_pf_reserved_fund_debt", columnList = "debt_id"),
        @Index(name = "idx_pf_reserved_fund_negotiation", columnList = "negotiation_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_pf_reserved_fund_public_id", columnNames = "public_id")
})
public class PersonalFinanceReservedFund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, length = 36, updatable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private PersonalFinanceReservedFundTargetType targetType = PersonalFinanceReservedFundTargetType.FREE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obligation_id")
    private PersonalFinancePaymentObligation obligation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id")
    private PersonalFinanceDebt debt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negotiation_id")
    private PersonalFinanceDebtNegotiation negotiation;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "target_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal targetAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 8)
    private PersonalFinanceCurrency currency = PersonalFinanceCurrency.PEN;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PersonalFinanceReservedFundStatus status = PersonalFinanceReservedFundStatus.ACTIVE;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (publicId == null || publicId.isBlank()) {
            publicId = UUID.randomUUID().toString();
        }
        createdAt = now;
        updatedAt = now;
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (targetType == null) targetType = PersonalFinanceReservedFundTargetType.FREE;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) < 0) targetAmount = BigDecimal.ZERO;
        if (currency == null) currency = PersonalFinanceCurrency.PEN;
        if (status == null) status = PersonalFinanceReservedFundStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == PersonalFinanceReservedFundStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public PersonalFinanceReservedFundTargetType getTargetType() { return targetType; }
    public void setTargetType(PersonalFinanceReservedFundTargetType targetType) { this.targetType = targetType; }
    public PersonalFinancePaymentObligation getObligation() { return obligation; }
    public void setObligation(PersonalFinancePaymentObligation obligation) { this.obligation = obligation; }
    public PersonalFinanceDebt getDebt() { return debt; }
    public void setDebt(PersonalFinanceDebt debt) { this.debt = debt; }
    public PersonalFinanceDebtNegotiation getNegotiation() { return negotiation; }
    public void setNegotiation(PersonalFinanceDebtNegotiation negotiation) { this.negotiation = negotiation; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    public PersonalFinanceCurrency getCurrency() { return currency; }
    public void setCurrency(PersonalFinanceCurrency currency) { this.currency = currency; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public PersonalFinanceReservedFundStatus getStatus() { return status; }
    public void setStatus(PersonalFinanceReservedFundStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
