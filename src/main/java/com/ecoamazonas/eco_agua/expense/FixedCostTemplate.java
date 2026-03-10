package com.ecoamazonas.eco_agua.expense;

import com.ecoamazonas.eco_agua.category.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "fixed_cost_template")
public class FixedCostTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "unit_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "include_in_break_even", nullable = false)
    private boolean includeInBreakEven = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String notes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitValue() {
        return unitValue;
    }

    public void setUnitValue(BigDecimal unitValue) {
        this.unitValue = unitValue;
    }

    public boolean isIncludeInBreakEven() {
        return includeInBreakEven;
    }

    public void setIncludeInBreakEven(boolean includeInBreakEven) {
        this.includeInBreakEven = includeInBreakEven;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getMonthlyTotal() {
        BigDecimal safeQuantity = quantity != null ? quantity : BigDecimal.ZERO;
        BigDecimal safeUnitValue = unitValue != null ? unitValue : BigDecimal.ZERO;

        return safeQuantity.multiply(safeUnitValue).setScale(2, RoundingMode.HALF_UP);
    }
}
