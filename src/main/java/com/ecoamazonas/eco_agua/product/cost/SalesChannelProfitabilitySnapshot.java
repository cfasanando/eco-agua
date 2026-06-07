package com.ecoamazonas.eco_agua.product.cost;

import java.time.LocalDate;
import java.util.List;

public class SalesChannelProfitabilitySnapshot {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final SalesChannelProfitabilitySummary summary;
    private final List<SalesChannelProfitabilityRow> rows;

    public SalesChannelProfitabilitySnapshot(
            LocalDate startDate,
            LocalDate endDate,
            SalesChannelProfitabilitySummary summary,
            List<SalesChannelProfitabilityRow> rows
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

    public SalesChannelProfitabilitySummary getSummary() {
        return summary;
    }

    public List<SalesChannelProfitabilityRow> getRows() {
        return rows;
    }
}
