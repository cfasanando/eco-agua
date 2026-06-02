package com.ecoamazonas.eco_agua.accounting;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_automation_rule")
public class AccountingAutomationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, unique = true, length = 40)
    private AccountingAutomationEvent eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debit_account_id", nullable = false)
    private AccountingAccount debitAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_account_id", nullable = false)
    private AccountingAccount creditAccount;

    @Column(length = 255)
    private String description;

    @Column(name = "generate_draft", nullable = false)
    private boolean generateDraft = true;

    @Column(nullable = false)
    private boolean active = true;

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

    public void setId(Long id) {
        this.id = id;
    }

    public AccountingAutomationEvent getEventType() {
        return eventType;
    }

    public void setEventType(AccountingAutomationEvent eventType) {
        this.eventType = eventType;
    }

    public AccountingAccount getDebitAccount() {
        return debitAccount;
    }

    public void setDebitAccount(AccountingAccount debitAccount) {
        this.debitAccount = debitAccount;
    }

    public AccountingAccount getCreditAccount() {
        return creditAccount;
    }

    public void setCreditAccount(AccountingAccount creditAccount) {
        this.creditAccount = creditAccount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isGenerateDraft() {
        return generateDraft;
    }

    public void setGenerateDraft(boolean generateDraft) {
        this.generateDraft = generateDraft;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
