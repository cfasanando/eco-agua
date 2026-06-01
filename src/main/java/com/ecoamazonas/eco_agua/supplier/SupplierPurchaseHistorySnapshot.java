package com.ecoamazonas.eco_agua.supplier;

import java.util.List;

public class SupplierPurchaseHistorySnapshot {

    private final SupplierPurchaseHistorySummary summary;
    private final List<SupplierPurchaseHistoryRow> rows;

    public SupplierPurchaseHistorySnapshot(
            SupplierPurchaseHistorySummary summary,
            List<SupplierPurchaseHistoryRow> rows
    ) {
        this.summary = summary;
        this.rows = rows != null ? rows : List.of();
    }

    public SupplierPurchaseHistorySummary getSummary() {
        return summary;
    }

    public List<SupplierPurchaseHistoryRow> getRows() {
        return rows;
    }
}
