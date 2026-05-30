package com.ecoamazonas.eco_agua.user;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_position")
public class JobPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "base_salary", precision = 10, scale = 2, nullable = false)
    private BigDecimal baseSalary = BigDecimal.ZERO;

    // Lo dejo mapeado porque existe en la tabla,
    // aunque aún no lo uses en lógica de negocio.
    @Column(name = "salary_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal salaryAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_period", length = 20, nullable = false)
    private SalaryPeriod salaryPeriod = SalaryPeriod.MONTHLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 20, nullable = false)
    private PaymentMode paymentMode = PaymentMode.FIXED;

    @Column(name = "commission_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Helpers ---

    @Transient
    public String getDisplayName() {
        // Ya lo usas en admin/users
        return this.name;
    }

    @Transient
    public String getPaymentModeLabel() {
        if (paymentMode == null) {
            return "-";
        }
        return switch (paymentMode) {
            case FIXED -> "Sueldo fijo";
            case COMMISSION -> "Solo porcentaje de ventas";
            case MIXED -> "Mixto (fijo + % ventas)";
        };
    }

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

    public BigDecimal getSalaryAmount() {
        return salaryAmount;
    }

    public void setSalaryAmount(BigDecimal salaryAmount) {
        this.salaryAmount = salaryAmount;
    }

    public SalaryPeriod getSalaryPeriod() {
        return salaryPeriod;
    }

    public void setSalaryPeriod(SalaryPeriod salaryPeriod) {
        this.salaryPeriod = salaryPeriod;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
