package com.ecoamazonas.eco_agua.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "fixed_cost_monthly_entry",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fixed_cost_monthly_template_period", columnNames = {"template_id", "year_value", "month_value"}),
                @UniqueConstraint(name = "uk_fixed_cost_monthly_expense", columnNames = {"expense_id"})
        }
)
public class FixedCostMonthlyEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private FixedCostTemplate template;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "year_value", nullable = false)
    private int yearValue;

    @Column(name = "month_value", nullable = false)
    private int monthValue;

    @Column(name = "generated_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal generatedAmount;

    @Column(name = "included_in_break_even", nullable = false)
    private boolean includedInBreakEven;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FixedCostTemplate getTemplate() {
        return template;
    }

    public void setTemplate(FixedCostTemplate template) {
        this.template = template;
    }

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
    }

    public int getYearValue() {
        return yearValue;
    }

    public void setYearValue(int yearValue) {
        this.yearValue = yearValue;
    }

    public int getMonthValue() {
        return monthValue;
    }

    public void setMonthValue(int monthValue) {
        this.monthValue = monthValue;
    }

    public BigDecimal getGeneratedAmount() {
        return generatedAmount;
    }

    public void setGeneratedAmount(BigDecimal generatedAmount) {
        this.generatedAmount = generatedAmount;
    }

    public boolean isIncludedInBreakEven() {
        return includedInBreakEven;
    }

    public void setIncludedInBreakEven(boolean includedInBreakEven) {
        this.includedInBreakEven = includedInBreakEven;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
