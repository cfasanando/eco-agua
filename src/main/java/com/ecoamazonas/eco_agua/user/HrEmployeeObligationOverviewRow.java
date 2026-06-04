package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class HrEmployeeObligationOverviewRow {

    private Long obligationId;
    private Long employeeId;
    private String employeeName;
    private String jobPositionName;
    private String typeLabel;
    private LocalDate issueDate;
    private BigDecimal originalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal appliedAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal pendingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private String discountModeLabel;
    private BigDecimal fixedDiscountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal discountPercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private String description;
    private boolean active;
    private String statusLabel;
    private String statusBadgeClass;

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

    public String getJobPositionName() {
        return jobPositionName;
    }

    public void setJobPositionName(String jobPositionName) {
        this.jobPositionName = jobPositionName;
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

    public BigDecimal getAppliedAmount() {
        return appliedAmount;
    }

    public void setAppliedAmount(BigDecimal appliedAmount) {
        this.appliedAmount = normalizeMoney(appliedAmount);
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

    public BigDecimal getFixedDiscountAmount() {
        return fixedDiscountAmount;
    }

    public void setFixedDiscountAmount(BigDecimal fixedDiscountAmount) {
        this.fixedDiscountAmount = normalizeMoney(fixedDiscountAmount);
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = normalizeMoney(discountPercentage);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass;
    }

    public void setStatusBadgeClass(String statusBadgeClass) {
        this.statusBadgeClass = statusBadgeClass;
    }

    public boolean hasPendingAmount() {
        return pendingAmount != null && pendingAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasAppliedAmount() {
        return appliedAmount != null && appliedAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasFixedDiscountAmount() {
        return fixedDiscountAmount != null && fixedDiscountAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasDiscountPercentage() {
        return discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
