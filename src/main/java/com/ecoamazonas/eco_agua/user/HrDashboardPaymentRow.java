package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class HrDashboardPaymentRow {

    private Long paymentId;
    private Long employeeId;
    private String employeeName;
    private LocalDate paymentDate;
    private BigDecimal grossAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private BigDecimal netAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private String observation;
    private boolean legacy;

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
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

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = normalizeMoney(grossAmount);
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = normalizeMoney(discountAmount);
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = normalizeMoney(netAmount);
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
