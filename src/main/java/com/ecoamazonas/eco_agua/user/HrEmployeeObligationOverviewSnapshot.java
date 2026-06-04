package com.ecoamazonas.eco_agua.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HrEmployeeObligationOverviewSnapshot {

    private String selectedStatus;
    private String selectedStatusLabel;
    private LocalDate generatedAt = LocalDate.now();
    private HrEmployeeObligationOverviewSummary summary = new HrEmployeeObligationOverviewSummary();
    private List<HrEmployeeObligationOverviewRow> rows = new ArrayList<>();

    public String getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(String selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    public String getSelectedStatusLabel() {
        return selectedStatusLabel;
    }

    public void setSelectedStatusLabel(String selectedStatusLabel) {
        this.selectedStatusLabel = selectedStatusLabel;
    }

    public LocalDate getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDate generatedAt) {
        this.generatedAt = generatedAt;
    }

    public HrEmployeeObligationOverviewSummary getSummary() {
        return summary;
    }

    public void setSummary(HrEmployeeObligationOverviewSummary summary) {
        this.summary = summary != null ? summary : new HrEmployeeObligationOverviewSummary();
    }

    public List<HrEmployeeObligationOverviewRow> getRows() {
        return rows;
    }

    public void setRows(List<HrEmployeeObligationOverviewRow> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }
}
