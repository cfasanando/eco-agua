package com.ecoamazonas.eco_agua.production;

import java.util.List;

public class ProductionTraceabilitySnapshot {

    private final ProductionTraceabilitySummary summary;
    private final List<ProductionOrder> rows;
    private final List<ProductionOrder> qualityAttentionRows;
    private final List<ProductionOrder> latestConfirmedRows;

    public ProductionTraceabilitySnapshot(
            ProductionTraceabilitySummary summary,
            List<ProductionOrder> rows,
            List<ProductionOrder> qualityAttentionRows,
            List<ProductionOrder> latestConfirmedRows
    ) {
        this.summary = summary;
        this.rows = rows;
        this.qualityAttentionRows = qualityAttentionRows;
        this.latestConfirmedRows = latestConfirmedRows;
    }

    public ProductionTraceabilitySummary getSummary() { return summary; }
    public List<ProductionOrder> getRows() { return rows; }
    public List<ProductionOrder> getQualityAttentionRows() { return qualityAttentionRows; }
    public List<ProductionOrder> getLatestConfirmedRows() { return latestConfirmedRows; }
}
