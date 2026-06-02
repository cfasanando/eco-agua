package com.ecoamazonas.eco_agua.income;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OtherIncomeCategorySummary {

    private final Long categoryId;
    private final String categoryName;
    private int recordCount;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private LocalDate lastIncomeDate;

    public OtherIncomeCategorySummary(Long categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public void add(BigDecimal amount, LocalDate incomeDate) {
        recordCount++;
        totalAmount = totalAmount.add(amount != null ? amount : BigDecimal.ZERO);
        if (incomeDate != null && (lastIncomeDate == null || incomeDate.isAfter(lastIncomeDate))) {
            lastIncomeDate = incomeDate;
        }
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDate getLastIncomeDate() {
        return lastIncomeDate;
    }
}
