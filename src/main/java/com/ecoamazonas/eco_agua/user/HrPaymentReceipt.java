package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HrPaymentReceipt {

    private Long paymentId;
    private String receiptNumber;
    private LocalDate paymentDate;
    private Integer periodYear;
    private Integer periodMonth;
    private String periodLabel;
    private Long employeeId;
    private String employeeName;
    private String employeeDni;
    private String employeePhone;
    private String employeeEmail;
    private String jobPositionName;
    private String paymentModeLabel;
    private String salaryPeriodLabel;
    private BigDecimal grossAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;
    private BigDecimal calculationBaseAmount = BigDecimal.ZERO;
    private BigDecimal commissionRate = BigDecimal.ZERO;
    private String observation;
    private LocalDate issueDate;

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
    }

    public Integer getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(Integer periodMonth) {
        this.periodMonth = periodMonth;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
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

    public String getEmployeeDni() {
        return employeeDni;
    }

    public void setEmployeeDni(String employeeDni) {
        this.employeeDni = employeeDni;
    }

    public String getEmployeePhone() {
        return employeePhone;
    }

    public void setEmployeePhone(String employeePhone) {
        this.employeePhone = employeePhone;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    public String getJobPositionName() {
        return jobPositionName;
    }

    public void setJobPositionName(String jobPositionName) {
        this.jobPositionName = jobPositionName;
    }

    public String getPaymentModeLabel() {
        return paymentModeLabel;
    }

    public void setPaymentModeLabel(String paymentModeLabel) {
        this.paymentModeLabel = paymentModeLabel;
    }

    public String getSalaryPeriodLabel() {
        return salaryPeriodLabel;
    }

    public void setSalaryPeriodLabel(String salaryPeriodLabel) {
        this.salaryPeriodLabel = salaryPeriodLabel;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public BigDecimal getCalculationBaseAmount() {
        return calculationBaseAmount;
    }

    public void setCalculationBaseAmount(BigDecimal calculationBaseAmount) {
        this.calculationBaseAmount = calculationBaseAmount;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }
}
