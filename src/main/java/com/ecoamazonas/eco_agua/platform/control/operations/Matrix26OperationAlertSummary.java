package com.ecoamazonas.eco_agua.platform.control.operations;

public record Matrix26OperationAlertSummary(
        long total,
        long open,
        long acknowledged,
        long resolved,
        long ignored,
        long critical,
        long high,
        long medium,
        long low,
        long info
) {
    public long active() {
        return open + acknowledged;
    }
}
