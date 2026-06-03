package com.ecoamazonas.eco_agua.accounting;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounting_rule_template")
public class AccountingRuleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, unique = true, length = 40)
    private AccountingAutomationEvent eventType;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "generate_draft", nullable = false)
    private boolean generateDraft = true;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC, id ASC")
    private List<AccountingRuleTemplateLine> lines = new ArrayList<>();

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<AccountingRuleTemplateLine> getLines() {
        return lines;
    }

    public void setLines(List<AccountingRuleTemplateLine> lines) {
        this.lines.clear();
        if (lines != null) {
            lines.forEach(this::addLine);
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void addLine(AccountingRuleTemplateLine line) {
        line.setTemplate(this);
        this.lines.add(line);
    }

    public long getDebitLineCount() {
        return lines.stream()
                .filter(line -> line.getLineSide() == AccountingRuleLineSide.DEBIT)
                .count();
    }

    public long getCreditLineCount() {
        return lines.stream()
                .filter(line -> line.getLineSide() == AccountingRuleLineSide.CREDIT)
                .count();
    }
}
