package com.ecoamazonas.eco_agua.user;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_payment")
public class EmployeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "gross_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(name = "calculation_base_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal calculationBaseAmount = BigDecimal.ZERO;

    @Column(name = "payment_mode_snapshot", nullable = false, length = 20)
    private String paymentModeSnapshot;

    @Column(name = "commission_rate_snapshot", precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionRateSnapshot = BigDecimal.ZERO;

    @Column(name = "salary_period_snapshot", length = 20)
    private String salaryPeriodSnapshot;

    @Column(length = 255)
    private String observation;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        }
        if (periodYear == null) {
            periodYear = paymentDate.getYear();
        }
        if (periodMonth == null) {
            periodMonth = paymentDate.getMonthValue();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
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

    public String getPaymentModeSnapshot() {
        return paymentModeSnapshot;
    }

    public void setPaymentModeSnapshot(String paymentModeSnapshot) {
        this.paymentModeSnapshot = paymentModeSnapshot;
    }

    public BigDecimal getCommissionRateSnapshot() {
        return commissionRateSnapshot;
    }

    public void setCommissionRateSnapshot(BigDecimal commissionRateSnapshot) {
        this.commissionRateSnapshot = commissionRateSnapshot;
    }

    public String getSalaryPeriodSnapshot() {
        return salaryPeriodSnapshot;
    }

    public void setSalaryPeriodSnapshot(String salaryPeriodSnapshot) {
        this.salaryPeriodSnapshot = salaryPeriodSnapshot;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
