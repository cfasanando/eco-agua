package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "personal_finance_payment", indexes = {
        @Index(name = "idx_pf_payment_user_date", columnList = "user_id,payment_date"),
        @Index(name = "idx_pf_payment_user_status", columnList = "user_id,status"),
        @Index(name = "idx_pf_payment_obligation", columnList = "obligation_id"),
        @Index(name = "idx_pf_payment_debt", columnList = "debt_id"),
        @Index(name = "idx_pf_payment_schedule_line", columnList = "schedule_line_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_pf_payment_public_id", columnNames = "public_id"),
        @UniqueConstraint(name = "uk_pf_payment_legacy_key", columnNames = "legacy_source_key")
})
public class PersonalFinancePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, length = 36, updatable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obligation_id")
    private PersonalFinancePaymentObligation obligation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id")
    private PersonalFinanceDebt debt;

    @Column(name = "schedule_line_id")
    private Long scheduleLineId;

    @Column(name = "obligation_title", nullable = false, length = 180)
    private String obligationTitle;

    @Column(name = "debt_name", length = 160)
    private String debtName;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "total_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "principal_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal principalAmount = BigDecimal.ZERO;

    @Column(name = "interest_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @Column(name = "insurance_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal insuranceAmount = BigDecimal.ZERO;

    @Column(name = "fee_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "penalty_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "other_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal otherAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 8)
    private PersonalFinanceCurrency currency = PersonalFinanceCurrency.PEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PersonalFinancePaymentMethod paymentMethod = PersonalFinancePaymentMethod.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 30)
    private PersonalFinancePaymentOrigin origin = PersonalFinancePaymentOrigin.MANUAL;

    @Column(name = "operation_number", length = 100)
    private String operationNumber;

    @Column(name = "recipient", length = 180)
    private String recipient;

    @Column(name = "notes", length = 1500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PersonalFinancePaymentStatus status = PersonalFinancePaymentStatus.ACTIVE;

    @Column(name = "receipt_original_name", length = 255)
    private String receiptOriginalName;

    @Column(name = "receipt_stored_path", length = 500)
    private String receiptStoredPath;

    @Column(name = "receipt_content_type", length = 120)
    private String receiptContentType;

    @Column(name = "receipt_size_bytes")
    private Long receiptSizeBytes;

    @Column(name = "legacy_source_key", length = 100)
    private String legacySourceKey;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reversed_by", length = 120)
    private String reversedBy;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

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
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
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

    public BigDecimal componentTotal() {
        return safe(principalAmount)
                .add(safe(interestAmount))
                .add(safe(insuranceAmount))
                .add(safe(feeAmount))
                .add(safe(penaltyAmount))
                .add(safe(otherAmount));
    }

    public boolean hasReceipt() {
        return receiptStoredPath != null && !receiptStoredPath.isBlank();
    }

    public boolean isActive() {
        return status == PersonalFinancePaymentStatus.ACTIVE;
    }

    private void normalize() {
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (principalAmount == null) principalAmount = BigDecimal.ZERO;
        if (interestAmount == null) interestAmount = BigDecimal.ZERO;
        if (insuranceAmount == null) insuranceAmount = BigDecimal.ZERO;
        if (feeAmount == null) feeAmount = BigDecimal.ZERO;
        if (penaltyAmount == null) penaltyAmount = BigDecimal.ZERO;
        if (otherAmount == null) otherAmount = BigDecimal.ZERO;
        if (currency == null) currency = PersonalFinanceCurrency.PEN;
        if (paymentMethod == null) paymentMethod = PersonalFinancePaymentMethod.OTHER;
        if (origin == null) origin = PersonalFinancePaymentOrigin.MANUAL;
        if (status == null) status = PersonalFinancePaymentStatus.ACTIVE;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public PersonalFinancePaymentObligation getObligation() { return obligation; }
    public void setObligation(PersonalFinancePaymentObligation obligation) { this.obligation = obligation; }
    public PersonalFinanceDebt getDebt() { return debt; }
    public void setDebt(PersonalFinanceDebt debt) { this.debt = debt; }
    public Long getScheduleLineId() { return scheduleLineId; }
    public void setScheduleLineId(Long scheduleLineId) { this.scheduleLineId = scheduleLineId; }
    public String getObligationTitle() { return obligationTitle; }
    public void setObligationTitle(String obligationTitle) { this.obligationTitle = obligationTitle; }
    public String getDebtName() { return debtName; }
    public void setDebtName(String debtName) { this.debtName = debtName; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }
    public BigDecimal getInsuranceAmount() { return insuranceAmount; }
    public void setInsuranceAmount(BigDecimal insuranceAmount) { this.insuranceAmount = insuranceAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public BigDecimal getPenaltyAmount() { return penaltyAmount; }
    public void setPenaltyAmount(BigDecimal penaltyAmount) { this.penaltyAmount = penaltyAmount; }
    public BigDecimal getOtherAmount() { return otherAmount; }
    public void setOtherAmount(BigDecimal otherAmount) { this.otherAmount = otherAmount; }
    public PersonalFinanceCurrency getCurrency() { return currency; }
    public void setCurrency(PersonalFinanceCurrency currency) { this.currency = currency; }
    public PersonalFinancePaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PersonalFinancePaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public PersonalFinancePaymentOrigin getOrigin() { return origin; }
    public void setOrigin(PersonalFinancePaymentOrigin origin) { this.origin = origin; }
    public String getOperationNumber() { return operationNumber; }
    public void setOperationNumber(String operationNumber) { this.operationNumber = operationNumber; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public PersonalFinancePaymentStatus getStatus() { return status; }
    public void setStatus(PersonalFinancePaymentStatus status) { this.status = status; }
    public String getReceiptOriginalName() { return receiptOriginalName; }
    public void setReceiptOriginalName(String receiptOriginalName) { this.receiptOriginalName = receiptOriginalName; }
    public String getReceiptStoredPath() { return receiptStoredPath; }
    public void setReceiptStoredPath(String receiptStoredPath) { this.receiptStoredPath = receiptStoredPath; }
    public String getReceiptContentType() { return receiptContentType; }
    public void setReceiptContentType(String receiptContentType) { this.receiptContentType = receiptContentType; }
    public Long getReceiptSizeBytes() { return receiptSizeBytes; }
    public void setReceiptSizeBytes(Long receiptSizeBytes) { this.receiptSizeBytes = receiptSizeBytes; }
    public String getLegacySourceKey() { return legacySourceKey; }
    public void setLegacySourceKey(String legacySourceKey) { this.legacySourceKey = legacySourceKey; }
    public LocalDateTime getReversedAt() { return reversedAt; }
    public void setReversedAt(LocalDateTime reversedAt) { this.reversedAt = reversedAt; }
    public String getReversedBy() { return reversedBy; }
    public void setReversedBy(String reversedBy) { this.reversedBy = reversedBy; }
    public String getReversalReason() { return reversalReason; }
    public void setReversalReason(String reversalReason) { this.reversalReason = reversalReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
