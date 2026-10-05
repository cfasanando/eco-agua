package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_finance_reserved_fund_movement", indexes = {
        @Index(name = "idx_pf_reserved_movement_user_date", columnList = "user_id,created_at"),
        @Index(name = "idx_pf_reserved_movement_fund", columnList = "reserved_fund_id"),
        @Index(name = "idx_pf_reserved_movement_payment", columnList = "payment_id")
})
public class PersonalFinanceReservedFundMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserved_fund_id", nullable = false)
    private PersonalFinanceReservedFund reservedFund;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private PersonalFinanceReservedFundMovementType movementType;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "balance_after", precision = 14, scale = 2, nullable = false)
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Column(name = "related_fund_id")
    private Long relatedFundId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (amount == null) amount = BigDecimal.ZERO;
        if (balanceAfter == null) balanceAfter = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public PersonalFinanceReservedFund getReservedFund() { return reservedFund; }
    public void setReservedFund(PersonalFinanceReservedFund reservedFund) { this.reservedFund = reservedFund; }
    public PersonalFinanceReservedFundMovementType getMovementType() { return movementType; }
    public void setMovementType(PersonalFinanceReservedFundMovementType movementType) { this.movementType = movementType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public Long getRelatedFundId() { return relatedFundId; }
    public void setRelatedFundId(Long relatedFundId) { this.relatedFundId = relatedFundId; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
