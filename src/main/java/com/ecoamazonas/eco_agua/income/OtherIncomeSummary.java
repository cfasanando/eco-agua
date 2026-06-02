package com.ecoamazonas.eco_agua.income;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OtherIncomeSummary {

    private int recordCount;
    private long daysWithIncome;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal averageAmount = BigDecimal.ZERO;
    private LocalDate firstIncomeDate;
    private LocalDate lastIncomeDate;
    private List<OtherIncomeCategorySummary> categorySummaries = new ArrayList<>();

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public long getDaysWithIncome() {
        return daysWithIncome;
    }

    public void setDaysWithIncome(long daysWithIncome) {
        this.daysWithIncome = daysWithIncome;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = safe(totalAmount);
    }

    public BigDecimal getAverageAmount() {
        return averageAmount;
    }

    public void setAverageAmount(BigDecimal averageAmount) {
        this.averageAmount = safe(averageAmount);
    }

    public LocalDate getFirstIncomeDate() {
        return firstIncomeDate;
    }

    public void setFirstIncomeDate(LocalDate firstIncomeDate) {
        this.firstIncomeDate = firstIncomeDate;
    }

    public LocalDate getLastIncomeDate() {
        return lastIncomeDate;
    }

    public void setLastIncomeDate(LocalDate lastIncomeDate) {
        this.lastIncomeDate = lastIncomeDate;
    }

    public List<OtherIncomeCategorySummary> getCategorySummaries() {
        return categorySummaries;
    }

    public void setCategorySummaries(List<OtherIncomeCategorySummary> categorySummaries) {
        this.categorySummaries = categorySummaries != null ? categorySummaries : new ArrayList<>();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
