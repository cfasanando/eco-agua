package com.ecoamazonas.eco_agua.category;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Generic category entity to group products, incomes, expenses, etc.
 *
 * The cost-behavior fields are only meaningful for expense categories.
 */
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_behavior", nullable = false, length = 30)
    private CostBehavior costBehavior = CostBehavior.NON_OPERATING;

    @Column(name = "include_in_break_even", nullable = false)
    private boolean includeInBreakEven = false;

    @Column(name = "include_in_operational_reading", nullable = false)
    private boolean includeInOperationalReading = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "personnel_mode", nullable = false, length = 30)
    private PersonnelMode personnelMode = PersonnelMode.NONE;

    @Column(name = "default_percent", precision = 10, scale = 2)
    private BigDecimal defaultPercent;

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

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public CostBehavior getCostBehavior() {
        return costBehavior;
    }

    public void setCostBehavior(CostBehavior costBehavior) {
        this.costBehavior = costBehavior;
    }

    public boolean isIncludeInBreakEven() {
        return includeInBreakEven;
    }

    public void setIncludeInBreakEven(boolean includeInBreakEven) {
        this.includeInBreakEven = includeInBreakEven;
    }

    public boolean isIncludeInOperationalReading() {
        return includeInOperationalReading;
    }

    public void setIncludeInOperationalReading(boolean includeInOperationalReading) {
        this.includeInOperationalReading = includeInOperationalReading;
    }

    public PersonnelMode getPersonnelMode() {
        return personnelMode;
    }

    public void setPersonnelMode(PersonnelMode personnelMode) {
        this.personnelMode = personnelMode;
    }

    public BigDecimal getDefaultPercent() {
        return defaultPercent;
    }

    public void setDefaultPercent(BigDecimal defaultPercent) {
        this.defaultPercent = defaultPercent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isExpenseCategory() {
        return type != null && type.isExpenseType();
    }
}
