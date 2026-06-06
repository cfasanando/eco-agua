package com.ecoamazonas.eco_agua.product.cost;

import java.time.LocalDate;
import java.util.List;

public class ProductProfitabilitySnapshot {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ProductProfitabilitySummary summary;
    private final List<ProductProfitabilityRow> rows;

    public ProductProfitabilitySnapshot(
            LocalDate startDate,
            LocalDate endDate,
            ProductProfitabilitySummary summary,
            List<ProductProfitabilityRow> rows
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.summary = summary;
        this.rows = rows;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public ProductProfitabilitySummary getSummary() {
        return summary;
    }

    public List<ProductProfitabilityRow> getRows() {
        return rows;
    }
}
