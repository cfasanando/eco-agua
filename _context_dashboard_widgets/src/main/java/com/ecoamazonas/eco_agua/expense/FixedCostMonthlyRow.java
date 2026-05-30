package com.ecoamazonas.eco_agua.expense;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FixedCostMonthlyRow {

    private Long templateId;
    private String description;
    private String categoryName;
    private String unitOfMeasure;
    private BigDecimal quantity;
    private BigDecimal unitValue;
    private BigDecimal monthlyTotal;
    private boolean includeInBreakEven;
    private boolean active;
    private boolean generated;
    private Long generatedExpenseId;
    private BigDecimal generatedAmount;
    private LocalDateTime generatedAt;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public BigDecimal getMonthlyTotal() {
        return monthlyTotal;
    }

    public void setMonthlyTotal(BigDecimal monthlyTotal) {
        this.monthlyTotal = monthlyTotal;
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

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public Long getGeneratedExpenseId() {
        return generatedExpenseId;
    }

    public void setGeneratedExpenseId(Long generatedExpenseId) {
        this.generatedExpenseId = generatedExpenseId;
    }

    public BigDecimal getGeneratedAmount() {
        return generatedAmount;
    }

    public void setGeneratedAmount(BigDecimal generatedAmount) {
        this.generatedAmount = generatedAmount;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
