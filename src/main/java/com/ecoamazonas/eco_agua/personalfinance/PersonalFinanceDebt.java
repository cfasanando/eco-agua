package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_finance_debt", indexes = {
        @Index(name = "idx_pf_debt_user_status", columnList = "user_id,status"),
        @Index(name = "idx_pf_debt_user_due", columnList = "user_id,due_day"),
        @Index(name = "idx_pf_debt_user_priority", columnList = "user_id,priority"),
        @Index(name = "idx_pf_debt_user_schedule_mode", columnList = "user_id,schedule_mode"),
        @Index(name = "idx_pf_debt_user_delinquency", columnList = "user_id,delinquency_start_date"),
        @Index(name = "idx_pf_debt_user_review", columnList = "user_id,next_review_date")
})
public class PersonalFinanceDebt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "debt_type", nullable = false, length = 40)
    private PersonalFinanceDebtType debtType = PersonalFinanceDebtType.CREDIT_CARD;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "creditor_name", length = 160)
    private String creditorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "holder_type", nullable = false, length = 40)
    private PersonalFinanceDebtHolderType holderType = PersonalFinanceDebtHolderType.OWN_NAME;

    @Column(name = "contact_name", length = 160)
    private String contactName;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 8)
    private PersonalFinanceCurrency currency = PersonalFinanceCurrency.PEN;

    @Column(name = "original_amount", precision = 14, scale = 2)
    private BigDecimal originalAmount = BigDecimal.ZERO;

    @Column(name = "current_balance", precision = 14, scale = 2, nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "monthly_due_amount", precision = 14, scale = 2)
    private BigDecimal monthlyDueAmount = BigDecimal.ZERO;

    @Column(name = "minimum_payment", precision = 14, scale = 2)
    private BigDecimal minimumPayment = BigDecimal.ZERO;

    @Column(name = "interest_rate_monthly", precision = 8, scale = 4)
    private BigDecimal interestRateMonthly = BigDecimal.ZERO;

    @Column(name = "due_day")
    private Integer dueDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_mode", nullable = false, length = 40)
    private PersonalFinanceDebtScheduleMode scheduleMode = PersonalFinanceDebtScheduleMode.SIMPLE_MONTHLY;

    @Column(name = "schedule_start_date")
    private LocalDate scheduleStartDate;

    @Column(name = "schedule_end_date")
    private LocalDate scheduleEndDate;

    @Column(name = "installment_count")
    private Integer installmentCount;

    @Column(name = "auto_generate_monthly", nullable = false)
    private boolean autoGenerateMonthly = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PersonalFinanceDebtStatus status = PersonalFinanceDebtStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private PersonalFinancePriority priority = PersonalFinancePriority.MEDIUM;

    @Column(name = "has_fixed_payment", nullable = false)
    private boolean fixedPayment = true;

    @Column(name = "previous_monthly_payment", precision = 14, scale = 2)
    private BigDecimal previousMonthlyPayment = BigDecimal.ZERO;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "delinquency_start_date")
    private LocalDate delinquencyStartDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_status", nullable = false, length = 40)
    private PersonalFinanceCollectionStatus collectionStatus = PersonalFinanceCollectionStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "negotiation_status", nullable = false, length = 40)
    private PersonalFinanceNegotiationStatus negotiationStatus = PersonalFinanceNegotiationStatus.NOT_STARTED;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        normalizeAmounts();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalizeAmounts();
    }

    public BigDecimal monthlyPressure() {
        if (fixedPayment && monthlyDueAmount != null && monthlyDueAmount.compareTo(BigDecimal.ZERO) > 0) {
            return monthlyDueAmount;
        }
        if (minimumPayment != null && minimumPayment.compareTo(BigDecimal.ZERO) > 0) {
            return minimumPayment;
        }
        return monthlyDueAmount != null ? monthlyDueAmount : BigDecimal.ZERO;
    }

    public boolean isHighInterest() {
        return interestRateMonthly != null && interestRateMonthly.compareTo(new BigDecimal("10.0000")) >= 0;
    }

    public BigDecimal monthlyInterestAmount() {
        BigDecimal balance = currentBalance == null ? BigDecimal.ZERO : currentBalance;
        BigDecimal rate = interestRateMonthly == null ? BigDecimal.ZERO : interestRateMonthly;
        if (balance.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return balance.multiply(rate).divide(new BigDecimal("100"));
    }

    public boolean usesGeneratedSchedule() {
        return scheduleMode == PersonalFinanceDebtScheduleMode.BANK_SCHEDULE
                || scheduleMode == PersonalFinanceDebtScheduleMode.PRIVATE_LENDER_INTEREST
                || scheduleMode == PersonalFinanceDebtScheduleMode.ONE_TIME
                || scheduleMode == PersonalFinanceDebtScheduleMode.AUTO_DEDUCTION;
    }

    public boolean isPaymentStopped() {
        return status == PersonalFinanceDebtStatus.STOPPED_PAYMENT
                || status == PersonalFinanceDebtStatus.COLLECTION
                || status == PersonalFinanceDebtStatus.PENDING_NEGOTIATION
                || status == PersonalFinanceDebtStatus.NEGOTIATION;
    }

    public boolean isDelinquentTracking() {
        return scheduleMode == PersonalFinanceDebtScheduleMode.TRACKING_ONLY
                || status == PersonalFinanceDebtStatus.STOPPED_PAYMENT
                || status == PersonalFinanceDebtStatus.COLLECTION
                || status == PersonalFinanceDebtStatus.PENDING_NEGOTIATION
                || status == PersonalFinanceDebtStatus.NEGOTIATION;
    }

    public long overdueDays() {
        if (delinquencyStartDate == null) {
            return 0;
        }
        return Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(delinquencyStartDate, LocalDate.now()));
    }

    private void normalizeAmounts() {
        if (originalAmount == null) originalAmount = BigDecimal.ZERO;
        if (currentBalance == null) currentBalance = BigDecimal.ZERO;
        if (monthlyDueAmount == null) monthlyDueAmount = BigDecimal.ZERO;
        if (minimumPayment == null) minimumPayment = BigDecimal.ZERO;
        if (interestRateMonthly == null) interestRateMonthly = BigDecimal.ZERO;
        if (previousMonthlyPayment == null) previousMonthlyPayment = BigDecimal.ZERO;
        if (holderType == null) holderType = PersonalFinanceDebtHolderType.OWN_NAME;
        if (priority == null) priority = PersonalFinancePriority.MEDIUM;
        if (scheduleMode == null) scheduleMode = PersonalFinanceDebtScheduleMode.SIMPLE_MONTHLY;
        if (installmentCount != null && installmentCount < 0) installmentCount = 0;
        if (status == null) status = PersonalFinanceDebtStatus.ACTIVE;
        if (debtType == null) debtType = PersonalFinanceDebtType.CREDIT_CARD;
        if (currency == null) currency = PersonalFinanceCurrency.PEN;
        if (collectionStatus == null) collectionStatus = PersonalFinanceCollectionStatus.NONE;
        if (negotiationStatus == null) negotiationStatus = PersonalFinanceNegotiationStatus.NOT_STARTED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public PersonalFinanceDebtType getDebtType() { return debtType; }
    public void setDebtType(PersonalFinanceDebtType debtType) { this.debtType = debtType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreditorName() { return creditorName; }
    public void setCreditorName(String creditorName) { this.creditorName = creditorName; }
    public PersonalFinanceDebtHolderType getHolderType() { return holderType; }
    public void setHolderType(PersonalFinanceDebtHolderType holderType) { this.holderType = holderType; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public PersonalFinanceCurrency getCurrency() { return currency; }
    public void setCurrency(PersonalFinanceCurrency currency) { this.currency = currency; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    public BigDecimal getMonthlyDueAmount() { return monthlyDueAmount; }
    public void setMonthlyDueAmount(BigDecimal monthlyDueAmount) { this.monthlyDueAmount = monthlyDueAmount; }
    public BigDecimal getMinimumPayment() { return minimumPayment; }
    public void setMinimumPayment(BigDecimal minimumPayment) { this.minimumPayment = minimumPayment; }
    public BigDecimal getInterestRateMonthly() { return interestRateMonthly; }
    public void setInterestRateMonthly(BigDecimal interestRateMonthly) { this.interestRateMonthly = interestRateMonthly; }
    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }
    public PersonalFinanceDebtScheduleMode getScheduleMode() { return scheduleMode; }
    public void setScheduleMode(PersonalFinanceDebtScheduleMode scheduleMode) { this.scheduleMode = scheduleMode; }
    public LocalDate getScheduleStartDate() { return scheduleStartDate; }
    public void setScheduleStartDate(LocalDate scheduleStartDate) { this.scheduleStartDate = scheduleStartDate; }
    public LocalDate getScheduleEndDate() { return scheduleEndDate; }
    public void setScheduleEndDate(LocalDate scheduleEndDate) { this.scheduleEndDate = scheduleEndDate; }
    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }
    public boolean isAutoGenerateMonthly() { return autoGenerateMonthly; }
    public void setAutoGenerateMonthly(boolean autoGenerateMonthly) { this.autoGenerateMonthly = autoGenerateMonthly; }
    public PersonalFinanceDebtStatus getStatus() { return status; }
    public void setStatus(PersonalFinanceDebtStatus status) { this.status = status; }
    public PersonalFinancePriority getPriority() { return priority; }
    public void setPriority(PersonalFinancePriority priority) { this.priority = priority; }
    public boolean isFixedPayment() { return fixedPayment; }
    public void setFixedPayment(boolean fixedPayment) { this.fixedPayment = fixedPayment; }
    public BigDecimal getPreviousMonthlyPayment() { return previousMonthlyPayment; }
    public void setPreviousMonthlyPayment(BigDecimal previousMonthlyPayment) { this.previousMonthlyPayment = previousMonthlyPayment; }
    public LocalDate getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(LocalDate lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }
    public LocalDate getDelinquencyStartDate() { return delinquencyStartDate; }
    public void setDelinquencyStartDate(LocalDate delinquencyStartDate) { this.delinquencyStartDate = delinquencyStartDate; }
    public PersonalFinanceCollectionStatus getCollectionStatus() { return collectionStatus; }
    public void setCollectionStatus(PersonalFinanceCollectionStatus collectionStatus) { this.collectionStatus = collectionStatus; }
    public PersonalFinanceNegotiationStatus getNegotiationStatus() { return negotiationStatus; }
    public void setNegotiationStatus(PersonalFinanceNegotiationStatus negotiationStatus) { this.negotiationStatus = negotiationStatus; }
    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
