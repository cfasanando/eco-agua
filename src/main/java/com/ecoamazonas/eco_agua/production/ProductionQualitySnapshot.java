package com.ecoamazonas.eco_agua.production;

import java.util.List;

public class ProductionQualitySnapshot {

    private final ProductionQualitySummary summary;
    private final List<ProductionOrder> rows;
    private final List<ProductionOrder> pendingRows;
    private final List<ProductionOrder> attentionRows;

    public ProductionQualitySnapshot(
            ProductionQualitySummary summary,
            List<ProductionOrder> rows,
            List<ProductionOrder> pendingRows,
            List<ProductionOrder> attentionRows
    ) {
        this.summary = summary;
        this.rows = rows != null ? rows : List.of();
        this.pendingRows = pendingRows != null ? pendingRows : List.of();
        this.attentionRows = attentionRows != null ? attentionRows : List.of();
    }

    public ProductionQualitySummary getSummary() {
        return summary;
    }

    public List<ProductionOrder> getRows() {
        return rows;
    }

    public List<ProductionOrder> getPendingRows() {
        return pendingRows;
    }

    public List<ProductionOrder> getAttentionRows() {
        return attentionRows;
    }

    public boolean isEmpty() {
        return summary == null || summary.isEmpty();
    }
}
