package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "personal_finance_debt_negotiation", indexes = {
        @Index(name = "idx_pf_negotiation_user_date", columnList = "user_id,conversation_date"),
        @Index(name = "idx_pf_negotiation_user_status", columnList = "user_id,status"),
        @Index(name = "idx_pf_negotiation_debt_date", columnList = "debt_id,conversation_date"),
        @Index(name = "idx_pf_negotiation_next_action", columnList = "user_id,next_action_date")
})
public class PersonalFinanceDebtNegotiation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debt_id", nullable = false)
    private PersonalFinanceDebt debt;

    @Column(name = "conversation_date", nullable = false)
    private LocalDate conversationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private PersonalFinanceNegotiationChannel channel = PersonalFinanceNegotiationChannel.WHATSAPP;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PersonalFinanceNegotiationEntryStatus status = PersonalFinanceNegotiationEntryStatus.DRAFT;

    @Column(name = "contact_person", length = 180)
    private String contactPerson;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 8)
    private PersonalFinanceCurrency currency = PersonalFinanceCurrency.PEN;

    @Column(name = "creditor_requested_amount", precision = 14, scale = 2)
    private BigDecimal creditorRequestedAmount = BigDecimal.ZERO;

    @Column(name = "affordable_amount", precision = 14, scale = 2)
    private BigDecimal affordableAmount = BigDecimal.ZERO;

    @Column(name = "initial_payment_amount", precision = 14, scale = 2)
    private BigDecimal initialPaymentAmount = BigDecimal.ZERO;

    @Column(name = "installment_count")
    private Integer installmentCount;

    @Column(name = "proposed_installment_amount", precision = 14, scale = 2)
    private BigDecimal proposedInstallmentAmount = BigDecimal.ZERO;

    @Column(name = "proposed_monthly_rate", precision = 8, scale = 4)
    private BigDecimal proposedMonthlyRate = BigDecimal.ZERO;

    @Column(name = "first_payment_date")
    private LocalDate firstPaymentDate;

    @Column(name = "response_deadline")
    private LocalDate responseDeadline;

    @Column(name = "next_action_date")
    private LocalDate nextActionDate;

    @Column(name = "next_action", length = 500)
    private String nextAction;

    @Column(name = "private_notes", length = 2000)
    private String privateNotes;

    @Column(name = "snapshot_current_balance", precision = 14, scale = 2, nullable = false)
    private BigDecimal snapshotCurrentBalance = BigDecimal.ZERO;

    @Column(name = "snapshot_monthly_payment", precision = 14, scale = 2, nullable = false)
    private BigDecimal snapshotMonthlyPayment = BigDecimal.ZERO;

    @Column(name = "snapshot_monthly_rate", precision = 8, scale = 4, nullable = false)
    private BigDecimal snapshotMonthlyRate = BigDecimal.ZERO;

    @Column(name = "evidence_original_name", length = 255)
    private String evidenceOriginalName;

    @Column(name = "evidence_stored_path", length = 500)
    private String evidenceStoredPath;

    @Column(name = "evidence_content_type", length = 120)
    private String evidenceContentType;

    @Column(name = "evidence_size_bytes")
    private Long evidenceSizeBytes;

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
        if (conversationDate == null) {
            conversationDate = LocalDate.now();
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

    public BigDecimal proposalTotal() {
        BigDecimal initial = safe(initialPaymentAmount);
        BigDecimal installment = safe(proposedInstallmentAmount);
        int count = installmentCount == null ? 0 : Math.max(installmentCount, 0);
        return initial.add(installment.multiply(BigDecimal.valueOf(count))).setScale(2, RoundingMode.HALF_UP);
    }

    public LocalDate proposedEndDate() {
        if (firstPaymentDate == null || installmentCount == null || installmentCount <= 0) {
            return firstPaymentDate;
        }
        return firstPaymentDate.plusMonths(installmentCount - 1L);
    }

    public BigDecimal monthlyRelief() {
        BigDecimal relief = safe(snapshotMonthlyPayment).subtract(safe(proposedInstallmentAmount));
        return relief.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean hasEvidence() {
        return evidenceStoredPath != null && !evidenceStoredPath.isBlank();
    }

    private void normalize() {
        if (channel == null) channel = PersonalFinanceNegotiationChannel.OTHER;
        if (status == null) status = PersonalFinanceNegotiationEntryStatus.DRAFT;
        if (currency == null) currency = PersonalFinanceCurrency.PEN;
        creditorRequestedAmount = safe(creditorRequestedAmount);
        affordableAmount = safe(affordableAmount);
        initialPaymentAmount = safe(initialPaymentAmount);
        proposedInstallmentAmount = safe(proposedInstallmentAmount);
        proposedMonthlyRate = safe(proposedMonthlyRate);
        snapshotCurrentBalance = safe(snapshotCurrentBalance);
        snapshotMonthlyPayment = safe(snapshotMonthlyPayment);
        snapshotMonthlyRate = safe(snapshotMonthlyRate);
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
    public PersonalFinanceDebt getDebt() { return debt; }
    public void setDebt(PersonalFinanceDebt debt) { this.debt = debt; }
    public LocalDate getConversationDate() { return conversationDate; }
    public void setConversationDate(LocalDate conversationDate) { this.conversationDate = conversationDate; }
    public PersonalFinanceNegotiationChannel getChannel() { return channel; }
    public void setChannel(PersonalFinanceNegotiationChannel channel) { this.channel = channel; }
    public PersonalFinanceNegotiationEntryStatus getStatus() { return status; }
    public void setStatus(PersonalFinanceNegotiationEntryStatus status) { this.status = status; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public PersonalFinanceCurrency getCurrency() { return currency; }
    public void setCurrency(PersonalFinanceCurrency currency) { this.currency = currency; }
    public BigDecimal getCreditorRequestedAmount() { return creditorRequestedAmount; }
    public void setCreditorRequestedAmount(BigDecimal creditorRequestedAmount) { this.creditorRequestedAmount = creditorRequestedAmount; }
    public BigDecimal getAffordableAmount() { return affordableAmount; }
    public void setAffordableAmount(BigDecimal affordableAmount) { this.affordableAmount = affordableAmount; }
    public BigDecimal getInitialPaymentAmount() { return initialPaymentAmount; }
    public void setInitialPaymentAmount(BigDecimal initialPaymentAmount) { this.initialPaymentAmount = initialPaymentAmount; }
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    public BigDecimal getProposedInstallmentAmount() { return proposedInstallmentAmount; }
    public void setProposedInstallmentAmount(BigDecimal proposedInstallmentAmount) { this.proposedInstallmentAmount = proposedInstallmentAmount; }
    public BigDecimal getProposedMonthlyRate() { return proposedMonthlyRate; }
    public void setProposedMonthlyRate(BigDecimal proposedMonthlyRate) { this.proposedMonthlyRate = proposedMonthlyRate; }
    public LocalDate getFirstPaymentDate() { return firstPaymentDate; }
    public void setFirstPaymentDate(LocalDate firstPaymentDate) { this.firstPaymentDate = firstPaymentDate; }
    public LocalDate getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(LocalDate responseDeadline) { this.responseDeadline = responseDeadline; }
    public LocalDate getNextActionDate() { return nextActionDate; }
    public void setNextActionDate(LocalDate nextActionDate) { this.nextActionDate = nextActionDate; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public String getPrivateNotes() { return privateNotes; }
    public void setPrivateNotes(String privateNotes) { this.privateNotes = privateNotes; }
    public BigDecimal getSnapshotCurrentBalance() { return snapshotCurrentBalance; }
    public void setSnapshotCurrentBalance(BigDecimal snapshotCurrentBalance) { this.snapshotCurrentBalance = snapshotCurrentBalance; }
    public BigDecimal getSnapshotMonthlyPayment() { return snapshotMonthlyPayment; }
    public void setSnapshotMonthlyPayment(BigDecimal snapshotMonthlyPayment) { this.snapshotMonthlyPayment = snapshotMonthlyPayment; }
    public BigDecimal getSnapshotMonthlyRate() { return snapshotMonthlyRate; }
    public void setSnapshotMonthlyRate(BigDecimal snapshotMonthlyRate) { this.snapshotMonthlyRate = snapshotMonthlyRate; }
    public String getEvidenceOriginalName() { return evidenceOriginalName; }
    public void setEvidenceOriginalName(String evidenceOriginalName) { this.evidenceOriginalName = evidenceOriginalName; }
    public String getEvidenceStoredPath() { return evidenceStoredPath; }
    public void setEvidenceStoredPath(String evidenceStoredPath) { this.evidenceStoredPath = evidenceStoredPath; }
    public String getEvidenceContentType() { return evidenceContentType; }
    public void setEvidenceContentType(String evidenceContentType) { this.evidenceContentType = evidenceContentType; }
    public Long getEvidenceSizeBytes() { return evidenceSizeBytes; }
    public void setEvidenceSizeBytes(Long evidenceSizeBytes) { this.evidenceSizeBytes = evidenceSizeBytes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
