package com.ecoamazonas.eco_agua.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeAttendanceDailySnapshot {

    private LocalDate selectedDate;
    private String selectedDateLabel;
    private List<EmployeeAttendanceRow> rows = new ArrayList<>();
    private EmployeeAttendanceSummary summary = new EmployeeAttendanceSummary();

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public String getSelectedDateLabel() {
        return selectedDateLabel;
    }

    public void setSelectedDateLabel(String selectedDateLabel) {
        this.selectedDateLabel = selectedDateLabel;
    }

    public List<EmployeeAttendanceRow> getRows() {
        return rows;
    }

    public void setRows(List<EmployeeAttendanceRow> rows) {
        this.rows = rows;
    }

    public EmployeeAttendanceSummary getSummary() {
        return summary;
    }

    public void setSummary(EmployeeAttendanceSummary summary) {
        this.summary = summary;
    }
}
