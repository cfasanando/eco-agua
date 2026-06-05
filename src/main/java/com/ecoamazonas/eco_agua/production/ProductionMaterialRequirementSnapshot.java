package com.ecoamazonas.eco_agua.production;

import java.util.List;

public class ProductionMaterialRequirementSnapshot {

    private final ProductionMaterialRequirementSummary summary;
    private final List<ProductionMaterialRequirementRow> requirementRows;
    private final List<ProductionMaterialRequirementRow> shortageRows;

    public ProductionMaterialRequirementSnapshot(
            ProductionMaterialRequirementSummary summary,
            List<ProductionMaterialRequirementRow> requirementRows,
            List<ProductionMaterialRequirementRow> shortageRows
    ) {
        this.summary = summary;
        this.requirementRows = requirementRows;
        this.shortageRows = shortageRows;
    }

    public ProductionMaterialRequirementSummary getSummary() { return summary; }
    public List<ProductionMaterialRequirementRow> getRequirementRows() { return requirementRows; }
    public List<ProductionMaterialRequirementRow> getShortageRows() { return shortageRows; }
}
