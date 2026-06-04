package com.ecoamazonas.eco_agua.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HrMonthlyPayrollSnapshot {

    private int selectedYear;
    private int selectedMonth;
    private String selectedMonthName;
    private LocalDate generatedAt = LocalDate.now();
    private HrMonthlyPayrollSummary summary = new HrMonthlyPayrollSummary();
    private List<HrMonthlyPayrollRow> rows = new ArrayList<>();

    public int getSelectedYear() {
        return selectedYear;
    }

    public void setSelectedYear(int selectedYear) {
        this.selectedYear = selectedYear;
    }

    public int getSelectedMonth() {
        return selectedMonth;
    }

    public void setSelectedMonth(int selectedMonth) {
        this.selectedMonth = selectedMonth;
    }

    public String getSelectedMonthName() {
        return selectedMonthName;
    }

    public void setSelectedMonthName(String selectedMonthName) {
        this.selectedMonthName = selectedMonthName;
    }

    public LocalDate getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDate generatedAt) {
        this.generatedAt = generatedAt;
    }

    public HrMonthlyPayrollSummary getSummary() {
        return summary;
    }

    public void setSummary(HrMonthlyPayrollSummary summary) {
        this.summary = summary != null ? summary : new HrMonthlyPayrollSummary();
    }

    public List<HrMonthlyPayrollRow> getRows() {
        return rows;
    }

    public void setRows(List<HrMonthlyPayrollRow> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }
}
