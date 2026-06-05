package com.ecoamazonas.eco_agua.production;

import java.util.ArrayList;
import java.util.List;

public class ProductionReportSnapshot {

    private final ProductionReportSummary summary;
    private final List<ProductionReportProductRow> productRows;
    private final List<ProductionReportProductRow> highWasteProductRows;
    private final List<ProductionOrder> latestConfirmedOrders;
    private final List<ProductionOrder> latestCanceledOrders;

    public ProductionReportSnapshot(
            ProductionReportSummary summary,
            List<ProductionReportProductRow> productRows,
            List<ProductionReportProductRow> highWasteProductRows,
            List<ProductionOrder> latestConfirmedOrders,
            List<ProductionOrder> latestCanceledOrders
    ) {
        this.summary = summary;
        this.productRows = productRows != null ? productRows : new ArrayList<>();
        this.highWasteProductRows = highWasteProductRows != null ? highWasteProductRows : new ArrayList<>();
        this.latestConfirmedOrders = latestConfirmedOrders != null ? latestConfirmedOrders : new ArrayList<>();
        this.latestCanceledOrders = latestCanceledOrders != null ? latestCanceledOrders : new ArrayList<>();
    }

    public ProductionReportSummary getSummary() {
        return summary;
    }

    public List<ProductionReportProductRow> getProductRows() {
        return productRows;
    }

    public List<ProductionReportProductRow> getHighWasteProductRows() {
        return highWasteProductRows;
    }

    public List<ProductionOrder> getLatestConfirmedOrders() {
        return latestConfirmedOrders;
    }

    public List<ProductionOrder> getLatestCanceledOrders() {
        return latestCanceledOrders;
    }

    public boolean isEmpty() {
        return summary == null || summary.getTotalOrders() == 0;
    }
}
