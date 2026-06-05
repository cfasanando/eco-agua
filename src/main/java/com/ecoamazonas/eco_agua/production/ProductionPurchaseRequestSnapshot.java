package com.ecoamazonas.eco_agua.production;

import java.util.List;

public class ProductionPurchaseRequestSnapshot {

    private final ProductionPurchaseRequestSummary summary;
    private final List<ProductionPurchaseRequestRow> purchaseRows;
    private final List<ProductionMaterialRequirementRow> requirementRows;

    public ProductionPurchaseRequestSnapshot(
            ProductionPurchaseRequestSummary summary,
            List<ProductionPurchaseRequestRow> purchaseRows,
            List<ProductionMaterialRequirementRow> requirementRows
    ) {
        this.summary = summary;
        this.purchaseRows = purchaseRows;
        this.requirementRows = requirementRows;
    }

    public ProductionPurchaseRequestSummary getSummary() { return summary; }
    public List<ProductionPurchaseRequestRow> getPurchaseRows() { return purchaseRows; }
    public List<ProductionMaterialRequirementRow> getRequirementRows() { return requirementRows; }
}
