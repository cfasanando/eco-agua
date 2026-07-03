package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_finance_income_source", indexes = {
        @Index(name = "idx_pf_income_source_user_active", columnList = "user_id,is_active")
})
public class PersonalFinanceIncomeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private PersonalFinanceIncomeType type = PersonalFinanceIncomeType.OTHER;

    @Column(name = "default_amount", precision = 14, scale = 2)
    private BigDecimal defaultAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 8)
    private PersonalFinanceCurrency currency = PersonalFinanceCurrency.PEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 30)
    private PersonalFinanceFrequency frequency = PersonalFinanceFrequency.MONTHLY;

    @Column(name = "expected_day")
    private Integer expectedDay;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "auto_generate_monthly", nullable = false)
    private boolean autoGenerateMonthly = true;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

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
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (defaultAmount == null) defaultAmount = BigDecimal.ZERO;
        if (currency == null) currency = PersonalFinanceCurrency.PEN;
        if (frequency == null) frequency = PersonalFinanceFrequency.MONTHLY;
        if (expectedDay != null) expectedDay = Math.max(1, Math.min(31, expectedDay));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PersonalFinanceIncomeType getType() { return type; }
    public void setType(PersonalFinanceIncomeType type) { this.type = type; }
    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(BigDecimal defaultAmount) { this.defaultAmount = defaultAmount; }
    public PersonalFinanceCurrency getCurrency() { return currency; }
    public void setCurrency(PersonalFinanceCurrency currency) { this.currency = currency; }
    public PersonalFinanceFrequency getFrequency() { return frequency; }
    public void setFrequency(PersonalFinanceFrequency frequency) { this.frequency = frequency; }
    public Integer getExpectedDay() { return expectedDay; }
    public void setExpectedDay(Integer expectedDay) { this.expectedDay = expectedDay; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public boolean isAutoGenerateMonthly() { return autoGenerateMonthly; }
    public void setAutoGenerateMonthly(boolean autoGenerateMonthly) { this.autoGenerateMonthly = autoGenerateMonthly; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
