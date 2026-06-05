package com.ecoamazonas.eco_agua.production;

import java.util.List;

public class ProductionPlanningSnapshot {

    private final ProductionPlanningSummary summary;
    private final List<ProductionPlanningRequirementRow> requirementRows;

    public ProductionPlanningSnapshot(
            ProductionPlanningSummary summary,
            List<ProductionPlanningRequirementRow> requirementRows
    ) {
        this.summary = summary;
        this.requirementRows = requirementRows;
    }

    public ProductionPlanningSummary getSummary() {
        return summary;
    }

    public List<ProductionPlanningRequirementRow> getRequirementRows() {
        return requirementRows;
    }
}
