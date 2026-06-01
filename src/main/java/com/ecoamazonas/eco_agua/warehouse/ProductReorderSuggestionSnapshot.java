package com.ecoamazonas.eco_agua.warehouse;

import java.util.List;

public class ProductReorderSuggestionSnapshot {

    private final ProductReorderSuggestionSummary summary;
    private final List<ProductReorderSuggestionRow> rows;

    public ProductReorderSuggestionSnapshot(
            ProductReorderSuggestionSummary summary,
            List<ProductReorderSuggestionRow> rows
    ) {
        this.summary = summary;
        this.rows = rows;
    }

    public ProductReorderSuggestionSummary getSummary() {
        return summary;
    }

    public List<ProductReorderSuggestionRow> getRows() {
        return rows;
    }
}
