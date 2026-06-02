package com.ecoamazonas.eco_agua.cashflow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MonthlyFinancialSummary {

    private int year;
    private int month;
    private int previousYear;
    private int previousMonth;
    private int nextYear;
    private int nextMonth;
    private String monthLabel;
    private LocalDate startDate;
    private LocalDate endDate;
    private CashflowSummary cashflowSummary = new CashflowSummary();
    private List<CashflowItem> dailyItems = new ArrayList<>();

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

    public int getPreviousYear() {
        return previousYear;
    }

    public void setPreviousYear(int previousYear) {
        this.previousYear = previousYear;
    }

    public int getPreviousMonth() {
        return previousMonth;
    }

    public void setPreviousMonth(int previousMonth) {
        this.previousMonth = previousMonth;
    }

    public int getNextYear() {
        return nextYear;
    }

    public void setNextYear(int nextYear) {
        this.nextYear = nextYear;
    }

    public int getNextMonth() {
        return nextMonth;
    }

    public void setNextMonth(int nextMonth) {
        this.nextMonth = nextMonth;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public void setMonthLabel(String monthLabel) {
        this.monthLabel = monthLabel;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public CashflowSummary getCashflowSummary() {
        return cashflowSummary;
    }

    public void setCashflowSummary(CashflowSummary cashflowSummary) {
        this.cashflowSummary = cashflowSummary != null ? cashflowSummary : new CashflowSummary();
    }

    public List<CashflowItem> getDailyItems() {
        return dailyItems;
    }

    public void setDailyItems(List<CashflowItem> dailyItems) {
        this.dailyItems = dailyItems != null ? dailyItems : new ArrayList<>();
    }

    public BigDecimal getRegisteredIncome() {
        return money(cashflowSummary.getRegisteredSales().add(cashflowSummary.getOtherIncome()));
    }

    public BigDecimal getRegisteredExpenses() {
        return money(cashflowSummary.getRegisteredExpenses());
    }

    public BigDecimal getCommercialResult() {
        return money(getRegisteredIncome().subtract(getRegisteredExpenses()));
    }

    public BigDecimal getCollectedIncome() {
        return money(cashflowSummary.getCollectedIncome());
    }

    public BigDecimal getPaidExpenses() {
        return money(cashflowSummary.getPaidExpenses());
    }

    public BigDecimal getNetCashResult() {
        return money(cashflowSummary.getNetCashResult());
    }

    public BigDecimal getPendingReceivables() {
        return money(cashflowSummary.getPendingReceivables());
    }

    public BigDecimal getPendingPayables() {
        return money(cashflowSummary.getPendingPayables());
    }

    public BigDecimal getTotalPending() {
        return money(getPendingReceivables().add(getPendingPayables()));
    }

    public BigDecimal getCashGap() {
        return money(getPendingReceivables().subtract(getPendingPayables()));
    }

    public boolean isCashPositive() {
        return getNetCashResult().compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean isCommercialPositive() {
        return getCommercialResult().compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean hasPendingItems() {
        return getPendingReceivables().compareTo(BigDecimal.ZERO) > 0
                || getPendingPayables().compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal money(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
