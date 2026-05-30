package com.ecoamazonas.eco_agua.user;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "employee_obligation_payment")
public class EmployeeObligationPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_payment_id", nullable = false)
    private EmployeePayment employeePayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_obligation_id", nullable = false)
    private EmployeeObligation employeeObligation;

    @Column(name = "applied_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal appliedAmount = BigDecimal.ZERO;

    @Column(length = 255)
    private String observation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EmployeePayment getEmployeePayment() {
        return employeePayment;
    }

    public void setEmployeePayment(EmployeePayment employeePayment) {
        this.employeePayment = employeePayment;
    }

    public EmployeeObligation getEmployeeObligation() {
        return employeeObligation;
    }

    public void setEmployeeObligation(EmployeeObligation employeeObligation) {
        this.employeeObligation = employeeObligation;
    }

    public BigDecimal getAppliedAmount() {
        return appliedAmount;
    }

    public void setAppliedAmount(BigDecimal appliedAmount) {
        this.appliedAmount = appliedAmount;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}
