package com.ecoamazonas.eco_agua.personalfinance;

public record PersonalFinanceDebtNegotiationSummary(
        long total,
        long open,
        long accepted,
        long followUpsDue
) {
}
