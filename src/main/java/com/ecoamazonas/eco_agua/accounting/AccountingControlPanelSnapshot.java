package com.ecoamazonas.eco_agua.accounting;

import java.time.LocalDateTime;
import java.util.List;

public class AccountingControlPanelSnapshot {

    private final AccountingControlPanelSummary summary;
    private final List<AccountingControlPanelAlert> alerts;
    private final List<AccountingControlPanelPeriodRow> periods;
    private final List<AccountingControlPanelConfigurationRow> configurationRows;
    private final List<AccountingControlPanelRecentEntry> recentAutomaticEntries;
    private final LocalDateTime generatedAt;

    public AccountingControlPanelSnapshot(
            AccountingControlPanelSummary summary,
            List<AccountingControlPanelAlert> alerts,
            List<AccountingControlPanelPeriodRow> periods,
            List<AccountingControlPanelConfigurationRow> configurationRows,
            List<AccountingControlPanelRecentEntry> recentAutomaticEntries,
            LocalDateTime generatedAt
    ) {
        this.summary = summary;
        this.alerts = alerts;
        this.periods = periods;
        this.configurationRows = configurationRows;
        this.recentAutomaticEntries = recentAutomaticEntries;
        this.generatedAt = generatedAt;
    }

    public AccountingControlPanelSummary getSummary() {
        return summary;
    }

    public List<AccountingControlPanelAlert> getAlerts() {
        return alerts;
    }

    public List<AccountingControlPanelPeriodRow> getPeriods() {
        return periods;
    }

    public List<AccountingControlPanelConfigurationRow> getConfigurationRows() {
        return configurationRows;
    }

    public List<AccountingControlPanelRecentEntry> getRecentAutomaticEntries() {
        return recentAutomaticEntries;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
