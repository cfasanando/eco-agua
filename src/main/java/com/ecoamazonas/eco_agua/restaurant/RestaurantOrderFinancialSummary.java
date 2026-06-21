package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;

public record RestaurantOrderFinancialSummary(
        BigDecimal itemsSubtotal,
        BigDecimal deliveryFee,
        BigDecimal serviceCharge,
        BigDecimal total,
        boolean igvEnabled,
        BigDecimal igvRate,
        BigDecimal taxableBase,
        BigDecimal igvAmount
) {
    public BigDecimal safeItemsSubtotal() {
        return safe(itemsSubtotal);
    }

    public BigDecimal safeDeliveryFee() {
        return safe(deliveryFee);
    }

    public BigDecimal safeServiceCharge() {
        return safe(serviceCharge);
    }

    public BigDecimal safeTotal() {
        return safe(total);
    }

    public BigDecimal safeIgvRate() {
        return safe(igvRate);
    }

    public BigDecimal safeTaxableBase() {
        return safe(taxableBase);
    }

    public BigDecimal safeIgvAmount() {
        return safe(igvAmount);
    }

    public boolean hasDeliveryFee() {
        return safeDeliveryFee().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasServiceCharge() {
        return safeServiceCharge().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean showIgvBreakdown() {
        return igvEnabled && safeIgvAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
