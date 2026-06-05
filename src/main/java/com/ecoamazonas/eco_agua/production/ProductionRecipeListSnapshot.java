package com.ecoamazonas.eco_agua.production;

import java.util.List;

public class ProductionRecipeListSnapshot {

    private final ProductionRecipeListSummary summary;
    private final List<ProductionRecipeProductRow> productRows;

    public ProductionRecipeListSnapshot(
            ProductionRecipeListSummary summary,
            List<ProductionRecipeProductRow> productRows
    ) {
        this.summary = summary;
        this.productRows = productRows;
    }

    public ProductionRecipeListSummary getSummary() { return summary; }
    public List<ProductionRecipeProductRow> getProductRows() { return productRows; }

    public boolean isEmpty() {
        return productRows == null || productRows.isEmpty();
    }
}
