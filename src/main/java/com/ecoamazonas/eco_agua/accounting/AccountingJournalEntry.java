package com.ecoamazonas.eco_agua.accounting;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounting_journal_entry")
public class AccountingJournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private AccountingJournalSourceType sourceType = AccountingJournalSourceType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountingJournalEntryStatus status = AccountingJournalEntryStatus.DRAFT;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC, id ASC")
    private List<AccountingJournalLine> lines = new ArrayList<>();

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

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AccountingJournalSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(AccountingJournalSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public AccountingJournalEntryStatus getStatus() {
        return status;
    }

    public void setStatus(AccountingJournalEntryStatus status) {
        this.status = status;
    }

    public List<AccountingJournalLine> getLines() {
        return lines;
    }

    public void setLines(List<AccountingJournalLine> lines) {
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

    public void addLine(AccountingJournalLine line) {
        line.setEntry(this);
        this.lines.add(line);
    }

    public BigDecimal getTotalDebit() {
        return lines.stream()
                .map(AccountingJournalLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCredit() {
        return lines.stream()
                .map(AccountingJournalLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isBalanced() {
        return getTotalDebit().compareTo(getTotalCredit()) == 0;
    }
}
