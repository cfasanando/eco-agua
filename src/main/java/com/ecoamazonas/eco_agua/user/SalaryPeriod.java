package com.ecoamazonas.eco_agua.user;

public enum SalaryPeriod {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    HOURLY;

    public String getLabel() {
        return switch (this) {
            case DAILY -> "daily";
            case WEEKLY -> "weekly";
            case BIWEEKLY -> "biweekly";
            case MONTHLY -> "monthly";
            case HOURLY -> "hourly";
        };
    }
}
