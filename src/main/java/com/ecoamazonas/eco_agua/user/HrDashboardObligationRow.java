package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class HrDashboardObligationRow {

    private Long obligationId;
    private Long employeeId;
    private String employeeName;
    private String typeLabel;
    private LocalDate issueDate;
    private BigDecimal originalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal pendingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private String discountModeLabel;
    private String description;
    private boolean active;

    public Long getObligationId() {
        return obligationId;
    }

    public void setObligationId(Long obligationId) {
        this.obligationId = obligationId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = normalizeMoney(originalAmount);
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(BigDecimal pendingAmount) {
        this.pendingAmount = normalizeMoney(pendingAmount);
    }

    public String getDiscountModeLabel() {
        return discountModeLabel;
    }

    public void setDiscountModeLabel(String discountModeLabel) {
        this.discountModeLabel = discountModeLabel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean hasPendingAmount() {
        return pendingAmount != null && pendingAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
