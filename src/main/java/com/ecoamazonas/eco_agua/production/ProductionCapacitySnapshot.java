package com.ecoamazonas.eco_agua.production;

import java.util.List;

public class ProductionCapacitySnapshot {

    private final ProductionCapacitySummary summary;
    private final List<ProductionCapacityProductRow> productRows;
    private final List<ProductionCapacityProductRow> blockedRows;
    private final List<ProductionCapacityProductRow> missingRecipeRows;

    public ProductionCapacitySnapshot(
            ProductionCapacitySummary summary,
            List<ProductionCapacityProductRow> productRows,
            List<ProductionCapacityProductRow> blockedRows,
            List<ProductionCapacityProductRow> missingRecipeRows
    ) {
        this.summary = summary;
        this.productRows = productRows;
        this.blockedRows = blockedRows;
        this.missingRecipeRows = missingRecipeRows;
    }

    public ProductionCapacitySummary getSummary() { return summary; }
    public List<ProductionCapacityProductRow> getProductRows() { return productRows; }
    public List<ProductionCapacityProductRow> getBlockedRows() { return blockedRows; }
    public List<ProductionCapacityProductRow> getMissingRecipeRows() { return missingRecipeRows; }
}
