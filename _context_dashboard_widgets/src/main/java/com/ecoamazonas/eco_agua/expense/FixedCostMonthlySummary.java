package com.ecoamazonas.eco_agua.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FixedCostMonthlySummary {

    private int year;
    private int month;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal expectedTotal = BigDecimal.ZERO;
    private BigDecimal expectedBreakEvenTotal = BigDecimal.ZERO;
    private BigDecimal generatedTotal = BigDecimal.ZERO;
    private BigDecimal generatedBreakEvenTotal = BigDecimal.ZERO;
    private BigDecimal pendingTotal = BigDecimal.ZERO;
    private BigDecimal pendingBreakEvenTotal = BigDecimal.ZERO;
    private BigDecimal actualRegisteredTotal = BigDecimal.ZERO;
    private Map<String, BigDecimal> actualBreakdown;
    private List<FixedCostMonthlyRow> rows = new ArrayList<>();

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public BigDecimal getExpectedTotal() {
        return expectedTotal;
    }

    public void setExpectedTotal(BigDecimal expectedTotal) {
        this.expectedTotal = expectedTotal;
    }

    public BigDecimal getExpectedBreakEvenTotal() {
        return expectedBreakEvenTotal;
    }

    public void setExpectedBreakEvenTotal(BigDecimal expectedBreakEvenTotal) {
        this.expectedBreakEvenTotal = expectedBreakEvenTotal;
    }

    public BigDecimal getGeneratedTotal() {
        return generatedTotal;
    }

    public void setGeneratedTotal(BigDecimal generatedTotal) {
        this.generatedTotal = generatedTotal;
    }

    public BigDecimal getGeneratedBreakEvenTotal() {
        return generatedBreakEvenTotal;
    }

    public void setGeneratedBreakEvenTotal(BigDecimal generatedBreakEvenTotal) {
        this.generatedBreakEvenTotal = generatedBreakEvenTotal;
    }

    public BigDecimal getPendingTotal() {
        return pendingTotal;
    }

    public void setPendingTotal(BigDecimal pendingTotal) {
        this.pendingTotal = pendingTotal;
    }

    public BigDecimal getPendingBreakEvenTotal() {
        return pendingBreakEvenTotal;
    }

    public void setPendingBreakEvenTotal(BigDecimal pendingBreakEvenTotal) {
        this.pendingBreakEvenTotal = pendingBreakEvenTotal;
    }

    public BigDecimal getActualRegisteredTotal() {
        return actualRegisteredTotal;
    }

    public void setActualRegisteredTotal(BigDecimal actualRegisteredTotal) {
        this.actualRegisteredTotal = actualRegisteredTotal;
    }

    public Map<String, BigDecimal> getActualBreakdown() {
        return actualBreakdown;
    }

    public void setActualBreakdown(Map<String, BigDecimal> actualBreakdown) {
        this.actualBreakdown = actualBreakdown;
    }

    public List<FixedCostMonthlyRow> getRows() {
        return rows;
    }

    public void setRows(List<FixedCostMonthlyRow> rows) {
        this.rows = rows;
    }
}
