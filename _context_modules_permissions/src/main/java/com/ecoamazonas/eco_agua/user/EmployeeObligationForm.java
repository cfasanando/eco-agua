package com.ecoamazonas.eco_agua.user;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeObligationForm {

    private Long id;
    private Long employeeId;
    private EmployeeObligationType type = EmployeeObligationType.LOAN;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate issueDate;

    private BigDecimal originalAmount;
    private BigDecimal pendingAmount;
    private EmployeeObligationDiscountMode discountMode = EmployeeObligationDiscountMode.MANUAL;
    private BigDecimal fixedDiscountAmount;
    private BigDecimal discountPercentage;
    private String description;
    private Boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public EmployeeObligationType getType() {
        return type;
    }

    public void setType(EmployeeObligationType type) {
        this.type = type;
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
        this.originalAmount = originalAmount;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(BigDecimal pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    public EmployeeObligationDiscountMode getDiscountMode() {
        return discountMode;
    }

    public void setDiscountMode(EmployeeObligationDiscountMode discountMode) {
        this.discountMode = discountMode;
    }

    public BigDecimal getFixedDiscountAmount() {
        return fixedDiscountAmount;
    }

    public void setFixedDiscountAmount(BigDecimal fixedDiscountAmount) {
        this.fixedDiscountAmount = fixedDiscountAmount;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
