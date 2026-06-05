package com.ecoamazonas.eco_agua.production;

import java.util.List;

public class ProductionScheduleSnapshot {

    private final ProductionScheduleSummary summary;
    private final List<ProductionOrder> rows;
    private final List<ProductionOrder> todayRows;
    private final List<ProductionOrder> upcomingRows;
    private final List<ProductionOrder> pendingQualityRows;
    private final List<ProductionOrder> overdueDraftRows;

    public ProductionScheduleSnapshot(
            ProductionScheduleSummary summary,
            List<ProductionOrder> rows,
            List<ProductionOrder> todayRows,
            List<ProductionOrder> upcomingRows,
            List<ProductionOrder> pendingQualityRows,
            List<ProductionOrder> overdueDraftRows
    ) {
        this.summary = summary;
        this.rows = rows != null ? rows : List.of();
        this.todayRows = todayRows != null ? todayRows : List.of();
        this.upcomingRows = upcomingRows != null ? upcomingRows : List.of();
        this.pendingQualityRows = pendingQualityRows != null ? pendingQualityRows : List.of();
        this.overdueDraftRows = overdueDraftRows != null ? overdueDraftRows : List.of();
    }

    public ProductionScheduleSummary getSummary() { return summary; }
    public List<ProductionOrder> getRows() { return rows; }
    public List<ProductionOrder> getTodayRows() { return todayRows; }
    public List<ProductionOrder> getUpcomingRows() { return upcomingRows; }
    public List<ProductionOrder> getPendingQualityRows() { return pendingQualityRows; }
    public List<ProductionOrder> getOverdueDraftRows() { return overdueDraftRows; }

    public boolean isEmpty() {
        return summary == null || summary.isEmpty();
    }
}
