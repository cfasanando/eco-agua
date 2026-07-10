package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_finance_debt_schedule_line", indexes = {
        @Index(name = "idx_pf_schedule_user_due", columnList = "user_id,due_date"),
        @Index(name = "idx_pf_schedule_debt_due", columnList = "debt_id,due_date"),
        @Index(name = "idx_pf_schedule_user_status", columnList = "user_id,status"),
        @Index(name = "idx_pf_schedule_obligation", columnList = "generated_obligation_id")
})
public class PersonalFinanceDebtScheduleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debt_id", nullable = false)
    private PersonalFinanceDebt debt;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 40)
    private PersonalFinanceScheduleLineType lineType = PersonalFinanceScheduleLineType.INSTALLMENT;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "principal_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal principalAmount = BigDecimal.ZERO;

    @Column(name = "interest_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @Column(name = "insurance_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    @Column(name = "fee_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 8)
    private PersonalFinanceCurrency currency = PersonalFinanceCurrency.PEN;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PersonalFinanceObligationStatus status = PersonalFinanceObligationStatus.PENDING;

    @Column(name = "paid_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_principal_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal paidPrincipalAmount = BigDecimal.ZERO;

    @Column(name = "paid_interest_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal paidInterestAmount = BigDecimal.ZERO;

    @Column(name = "paid_insurance_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal paidInsuranceAmount = BigDecimal.ZERO;

    @Column(name = "paid_fee_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal paidFeeAmount = BigDecimal.ZERO;

    @Column(name = "paid_penalty_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal paidPenaltyAmount = BigDecimal.ZERO;

    @Column(name = "paid_other_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal paidOtherAmount = BigDecimal.ZERO;

    @Column(name = "paid_at")
    private LocalDate paidAt;

    @Column(name = "generated_obligation_id")
    private Long generatedObligationId;

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

    public BigDecimal calculatedTotal() {
        BigDecimal total = safe(totalAmount);
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            return total;
        }
        return safe(principalAmount).add(safe(interestAmount)).add(safe(insuranceAmount)).add(safe(feeAmount));
    }

    public BigDecimal pendingAmount() {
        BigDecimal pending = calculatedTotal().subtract(safe(paidAmount));
        return pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending;
    }

    public boolean isPaidLike() {
        return status == PersonalFinanceObligationStatus.PAID || pendingAmount().compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal principalPendingAmount() {
        BigDecimal pending = safe(principalAmount).subtract(safe(paidPrincipalAmount));
        return pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending;
    }

    public BigDecimal interestPendingAmount() {
        BigDecimal pending = safe(interestAmount).subtract(safe(paidInterestAmount));
        return pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending;
    }

    public boolean hasGeneratedObligation() {
        return generatedObligationId != null;
    }

    private void normalize() {
        if (lineType == null) lineType = PersonalFinanceScheduleLineType.INSTALLMENT;
        if (principalAmount == null) principalAmount = BigDecimal.ZERO;
        if (interestAmount == null) interestAmount = BigDecimal.ZERO;
        if (insuranceAmount == null) insuranceAmount = BigDecimal.ZERO;
        if (feeAmount == null) feeAmount = BigDecimal.ZERO;
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        if (paidPrincipalAmount == null) paidPrincipalAmount = BigDecimal.ZERO;
        if (paidInterestAmount == null) paidInterestAmount = BigDecimal.ZERO;
        if (paidInsuranceAmount == null) paidInsuranceAmount = BigDecimal.ZERO;
        if (paidFeeAmount == null) paidFeeAmount = BigDecimal.ZERO;
        if (paidPenaltyAmount == null) paidPenaltyAmount = BigDecimal.ZERO;
        if (paidOtherAmount == null) paidOtherAmount = BigDecimal.ZERO;
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) totalAmount = principalAmount.add(interestAmount).add(insuranceAmount).add(feeAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;
        if (currency == null) currency = PersonalFinanceCurrency.PEN;
        if (status == null) status = PersonalFinanceObligationStatus.PENDING;
        if (paidAmount.compareTo(BigDecimal.ZERO) < 0) paidAmount = BigDecimal.ZERO;
        if (paidAmount.compareTo(totalAmount) >= 0 && totalAmount.compareTo(BigDecimal.ZERO) > 0) status = PersonalFinanceObligationStatus.PAID;
        if (status == PersonalFinanceObligationStatus.PAID && paidAmount.compareTo(totalAmount) < 0) paidAmount = totalAmount;
        if (title == null || title.isBlank()) title = lineType.getLabel();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public PersonalFinanceDebt getDebt() { return debt; }
    public void setDebt(PersonalFinanceDebt debt) { this.debt = debt; }
    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
    public PersonalFinanceScheduleLineType getLineType() { return lineType; }
    public void setLineType(PersonalFinanceScheduleLineType lineType) { this.lineType = lineType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }
    public BigDecimal getInsuranceAmount() { return insuranceAmount; }
    public void setInsuranceAmount(BigDecimal insuranceAmount) { this.insuranceAmount = insuranceAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public PersonalFinanceCurrency getCurrency() { return currency; }
    public void setCurrency(PersonalFinanceCurrency currency) { this.currency = currency; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public PersonalFinanceObligationStatus getStatus() { return status; }
    public void setStatus(PersonalFinanceObligationStatus status) { this.status = status; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getPaidPrincipalAmount() { return paidPrincipalAmount; }
    public void setPaidPrincipalAmount(BigDecimal paidPrincipalAmount) { this.paidPrincipalAmount = paidPrincipalAmount; }
    public BigDecimal getPaidInterestAmount() { return paidInterestAmount; }
    public void setPaidInterestAmount(BigDecimal paidInterestAmount) { this.paidInterestAmount = paidInterestAmount; }
    public BigDecimal getPaidInsuranceAmount() { return paidInsuranceAmount; }
    public void setPaidInsuranceAmount(BigDecimal paidInsuranceAmount) { this.paidInsuranceAmount = paidInsuranceAmount; }
    public BigDecimal getPaidFeeAmount() { return paidFeeAmount; }
    public void setPaidFeeAmount(BigDecimal paidFeeAmount) { this.paidFeeAmount = paidFeeAmount; }
    public BigDecimal getPaidPenaltyAmount() { return paidPenaltyAmount; }
    public void setPaidPenaltyAmount(BigDecimal paidPenaltyAmount) { this.paidPenaltyAmount = paidPenaltyAmount; }
    public BigDecimal getPaidOtherAmount() { return paidOtherAmount; }
    public void setPaidOtherAmount(BigDecimal paidOtherAmount) { this.paidOtherAmount = paidOtherAmount; }
    public LocalDate getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDate paidAt) { this.paidAt = paidAt; }
    public Long getGeneratedObligationId() { return generatedObligationId; }
    public void setGeneratedObligationId(Long generatedObligationId) { this.generatedObligationId = generatedObligationId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
