package com.ecoamazonas.eco_agua.production;

import java.time.LocalDate;

public enum ProductionExpiryStatus {
    NO_DATE("Sin fecha", "bg-secondary"),
    VALID("Vigente", "bg-success"),
    EXPIRING_SOON("Por vencer", "bg-warning text-dark"),
    EXPIRED("Vencido", "bg-danger");

    private static final long WARNING_DAYS = 7;

    private final String label;
    private final String badgeClass;

    ProductionExpiryStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public static ProductionExpiryStatus fromDate(LocalDate expiryDate, LocalDate today) {
        if (expiryDate == null) {
            return NO_DATE;
        }

        LocalDate effectiveToday = today != null ? today : LocalDate.now();
        if (expiryDate.isBefore(effectiveToday)) {
            return EXPIRED;
        }

        if (!expiryDate.isAfter(effectiveToday.plusDays(WARNING_DAYS))) {
            return EXPIRING_SOON;
        }

        return VALID;
    }
}
