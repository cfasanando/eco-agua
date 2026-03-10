package com.ecoamazonas.eco_agua.expense;

import java.math.BigDecimal;

public class FixedCostTemplateForm {

    private Long id;
    private String description;
    private Long categoryId;
    private String unitOfMeasure;
    private BigDecimal quantity;
    private BigDecimal unitValue;
    private boolean includeInBreakEven = true;
    private boolean active = true;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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
}
