package com.ecoamazonas.eco_agua.production;

import java.util.List;

public class ProductionExpirySnapshot {

    private final ProductionExpirySummary summary;
    private final List<ProductionOrder> rows;
    private final List<ProductionOrder> expiredRows;
    private final List<ProductionOrder> expiringSoonRows;
    private final List<ProductionOrder> noDateRows;

    public ProductionExpirySnapshot(
            ProductionExpirySummary summary,
            List<ProductionOrder> rows,
            List<ProductionOrder> expiredRows,
            List<ProductionOrder> expiringSoonRows,
            List<ProductionOrder> noDateRows
    ) {
        this.summary = summary;
        this.rows = rows;
        this.expiredRows = expiredRows;
        this.expiringSoonRows = expiringSoonRows;
        this.noDateRows = noDateRows;
    }

    public ProductionExpirySummary getSummary() { return summary; }
    public List<ProductionOrder> getRows() { return rows; }
    public List<ProductionOrder> getExpiredRows() { return expiredRows; }
    public List<ProductionOrder> getExpiringSoonRows() { return expiringSoonRows; }
    public List<ProductionOrder> getNoDateRows() { return noDateRows; }
}
