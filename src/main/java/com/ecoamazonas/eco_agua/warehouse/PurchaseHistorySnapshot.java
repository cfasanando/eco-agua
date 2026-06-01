package com.ecoamazonas.eco_agua.warehouse;

import java.util.List;

public class PurchaseHistorySnapshot {

    private final PurchaseHistorySummary summary;
    private final List<PurchaseHistoryRow> rows;

    public PurchaseHistorySnapshot(PurchaseHistorySummary summary, List<PurchaseHistoryRow> rows) {
        this.summary = summary;
        this.rows = rows;
    }

    public PurchaseHistorySummary getSummary() {
        return summary;
    }

    public List<PurchaseHistoryRow> getRows() {
        return rows;
    }
}
