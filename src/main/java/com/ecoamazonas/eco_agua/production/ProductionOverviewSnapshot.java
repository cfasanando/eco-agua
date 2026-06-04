package com.ecoamazonas.eco_agua.production;

import java.util.ArrayList;
import java.util.List;

public class ProductionOverviewSnapshot {

    private final ProductionOverviewSummary summary;
    private final List<ProductionOverviewProductRow> productRows;
    private final List<ProductionOrder> latestOrders;
    private final List<ProductionOrder> pendingDrafts;

    public ProductionOverviewSnapshot(
            ProductionOverviewSummary summary,
            List<ProductionOverviewProductRow> productRows,
            List<ProductionOrder> latestOrders,
            List<ProductionOrder> pendingDrafts
    ) {
        this.summary = summary;
        this.productRows = productRows != null ? productRows : new ArrayList<>();
        this.latestOrders = latestOrders != null ? latestOrders : new ArrayList<>();
        this.pendingDrafts = pendingDrafts != null ? pendingDrafts : new ArrayList<>();
    }

    public ProductionOverviewSummary getSummary() {
        return summary;
    }

    public List<ProductionOverviewProductRow> getProductRows() {
        return productRows;
    }

    public List<ProductionOrder> getLatestOrders() {
        return latestOrders;
    }

    public List<ProductionOrder> getPendingDrafts() {
        return pendingDrafts;
    }

    public boolean isEmpty() {
        return summary == null || summary.getTotalOrders() == 0;
    }
}
