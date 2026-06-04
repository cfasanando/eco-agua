package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class HrMonthlyPayrollRow {

    private Long employeeId;
    private String employeeName;
    private String jobPositionName;
    private String paymentModeLabel;
    private String salaryPeriodLabel;
    private String phone;
    private BigDecimal configuredAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal monthlyGross = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal monthlyDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal monthlyNet = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal pendingObligations = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private int paymentCount;
    private int activeObligationCount;
    private LocalDate lastPaymentDate;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getConfiguredAmount() {
        return configuredAmount;
    }

    public void setConfiguredAmount(BigDecimal configuredAmount) {
        this.configuredAmount = normalizeMoney(configuredAmount);
    }

    public BigDecimal getMonthlyGross() {
        return monthlyGross;
    }

    public void setMonthlyGross(BigDecimal monthlyGross) {
        this.monthlyGross = normalizeMoney(monthlyGross);
    }

    public BigDecimal getMonthlyDiscount() {
        return monthlyDiscount;
    }

    public void setMonthlyDiscount(BigDecimal monthlyDiscount) {
        this.monthlyDiscount = normalizeMoney(monthlyDiscount);
    }

    public BigDecimal getMonthlyNet() {
        return monthlyNet;
    }

    public void setMonthlyNet(BigDecimal monthlyNet) {
        this.monthlyNet = normalizeMoney(monthlyNet);
    }

    public BigDecimal getPendingObligations() {
        return pendingObligations;
    }

    public void setPendingObligations(BigDecimal pendingObligations) {
        this.pendingObligations = normalizeMoney(pendingObligations);
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(int paymentCount) {
        this.paymentCount = paymentCount;
    }

    public int getActiveObligationCount() {
        return activeObligationCount;
    }

    public void setActiveObligationCount(int activeObligationCount) {
        this.activeObligationCount = activeObligationCount;
    }

    public LocalDate getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(LocalDate lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public boolean hasPayments() {
        return paymentCount > 0;
    }

    public boolean hasPendingObligations() {
        return pendingObligations != null && pendingObligations.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
