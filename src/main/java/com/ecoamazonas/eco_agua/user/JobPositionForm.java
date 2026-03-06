package com.ecoamazonas.eco_agua.user;

import java.math.BigDecimal;

public class JobPositionForm {

    private Long id;
    private String name;
    private String description;
    private BigDecimal baseSalary;
    private SalaryPeriod salaryPeriod;
    private boolean active = true;

    private PaymentMode paymentMode;
    // En porcentaje (ej: 25 = 25%)
    private BigDecimal salesCommissionPercent;

    // --- Getters & setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public SalaryPeriod getSalaryPeriod() {
        return salaryPeriod;
    }

    public void setSalaryPeriod(SalaryPeriod salaryPeriod) {
        this.salaryPeriod = salaryPeriod;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public BigDecimal getSalesCommissionPercent() {
        return salesCommissionPercent;
    }

    public void setSalesCommissionPercent(BigDecimal salesCommissionPercent) {
        this.salesCommissionPercent = salesCommissionPercent;
    }
}
