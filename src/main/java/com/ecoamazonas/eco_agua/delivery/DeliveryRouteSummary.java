package com.ecoamazonas.eco_agua.delivery;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DeliveryRouteSummary {
    private final int totalStops;
    private final int locatedStops;
    private final int missingLocationStops;
    private final BigDecimal estimatedDistanceKm;
    private final int estimatedMinutes;

    public DeliveryRouteSummary(int totalStops, int locatedStops, int missingLocationStops, BigDecimal estimatedDistanceKm, int estimatedMinutes) {
        this.totalStops = totalStops;
        this.locatedStops = locatedStops;
        this.missingLocationStops = missingLocationStops;
        this.estimatedDistanceKm = estimatedDistanceKm != null ? estimatedDistanceKm.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.estimatedMinutes = estimatedMinutes;
    }

    public int getTotalStops() { return totalStops; }
    public int getLocatedStops() { return locatedStops; }
    public int getMissingLocationStops() { return missingLocationStops; }
    public BigDecimal getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public int getEstimatedMinutes() { return estimatedMinutes; }

    public int getEstimatedHours() { return estimatedMinutes / 60; }
    public int getEstimatedRemainingMinutes() { return estimatedMinutes % 60; }

    public String getEstimatedTimeLabel() {
        if (estimatedMinutes <= 0) {
            return "-";
        }
        if (estimatedMinutes < 60) {
            return estimatedMinutes + " min";
        }
        return getEstimatedHours() + " h " + getEstimatedRemainingMinutes() + " min";
    }
}
