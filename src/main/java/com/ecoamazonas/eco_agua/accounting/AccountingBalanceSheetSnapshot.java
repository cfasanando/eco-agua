package com.ecoamazonas.eco_agua.accounting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AccountingBalanceSheetSnapshot {

    private LocalDate startDate;
    private LocalDate endDate;
    private String statusFilter = "POSTED";
    private AccountingBalanceSheetSummary summary = new AccountingBalanceSheetSummary();
    private List<AccountingBalanceSheetRow> assetRows = new ArrayList<>();
    private List<AccountingBalanceSheetRow> liabilityRows = new ArrayList<>();
    private List<AccountingBalanceSheetRow> equityRows = new ArrayList<>();

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

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public AccountingBalanceSheetSummary getSummary() {
        return summary;
    }

    public void setSummary(AccountingBalanceSheetSummary summary) {
        this.summary = summary == null ? new AccountingBalanceSheetSummary() : summary;
    }

    public List<AccountingBalanceSheetRow> getAssetRows() {
        return assetRows;
    }

    public void setAssetRows(List<AccountingBalanceSheetRow> assetRows) {
        this.assetRows = assetRows == null ? new ArrayList<>() : assetRows;
    }

    public List<AccountingBalanceSheetRow> getLiabilityRows() {
        return liabilityRows;
    }

    public void setLiabilityRows(List<AccountingBalanceSheetRow> liabilityRows) {
        this.liabilityRows = liabilityRows == null ? new ArrayList<>() : liabilityRows;
    }

    public List<AccountingBalanceSheetRow> getEquityRows() {
        return equityRows;
    }

    public void setEquityRows(List<AccountingBalanceSheetRow> equityRows) {
        this.equityRows = equityRows == null ? new ArrayList<>() : equityRows;
    }

    public boolean hasRows() {
        return !assetRows.isEmpty() || !liabilityRows.isEmpty() || !equityRows.isEmpty();
    }
}
