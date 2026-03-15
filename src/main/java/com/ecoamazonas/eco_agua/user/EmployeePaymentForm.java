package com.ecoamazonas.eco_agua.user;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeePaymentForm {

    private Long employeeId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paymentDate;

    private BigDecimal grossAmount;
    private BigDecimal manualDiscountAmount;
    private Long obligationId;
    private BigDecimal obligationAppliedAmount;
    private String observation;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
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
        this.grossAmount = grossAmount;
    }

    public BigDecimal getManualDiscountAmount() {
        return manualDiscountAmount;
    }

    public void setManualDiscountAmount(BigDecimal manualDiscountAmount) {
        this.manualDiscountAmount = manualDiscountAmount;
    }

    public Long getObligationId() {
        return obligationId;
    }

    public void setObligationId(Long obligationId) {
        this.obligationId = obligationId;
    }

    public BigDecimal getObligationAppliedAmount() {
        return obligationAppliedAmount;
    }

    public void setObligationAppliedAmount(BigDecimal obligationAppliedAmount) {
        this.obligationAppliedAmount = obligationAppliedAmount;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}
