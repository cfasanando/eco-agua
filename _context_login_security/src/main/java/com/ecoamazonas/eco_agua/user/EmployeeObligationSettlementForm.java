package com.ecoamazonas.eco_agua.user;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeObligationSettlementForm {

    private Long employeeId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate settlementDate;

    private BigDecimal amount;
    private Long obligationId;
    private String observation;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getObligationId() {
        return obligationId;
    }

    public void setObligationId(Long obligationId) {
        this.obligationId = obligationId;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}
