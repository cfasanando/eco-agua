package com.ecoamazonas.eco_agua.user;

import java.util.ArrayList;
import java.util.List;

public class HrDashboardSnapshot {

    private int selectedYear;
    private int selectedMonth;
    private String selectedMonthName;
    private HrDashboardSummary summary = new HrDashboardSummary();
    private List<HrDashboardEmployeeRow> employeeRows = new ArrayList<>();
    private List<HrDashboardPaymentRow> recentPayments = new ArrayList<>();
    private List<HrDashboardObligationRow> pendingObligations = new ArrayList<>();

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

    public HrDashboardSummary getSummary() {
        return summary;
    }

    public void setSummary(HrDashboardSummary summary) {
        this.summary = summary;
    }

    public List<HrDashboardEmployeeRow> getEmployeeRows() {
        return employeeRows;
    }

    public void setEmployeeRows(List<HrDashboardEmployeeRow> employeeRows) {
        this.employeeRows = employeeRows;
    }

    public List<HrDashboardPaymentRow> getRecentPayments() {
        return recentPayments;
    }

    public void setRecentPayments(List<HrDashboardPaymentRow> recentPayments) {
        this.recentPayments = recentPayments;
    }

    public List<HrDashboardObligationRow> getPendingObligations() {
        return pendingObligations;
    }

    public void setPendingObligations(List<HrDashboardObligationRow> pendingObligations) {
        this.pendingObligations = pendingObligations;
    }
}
