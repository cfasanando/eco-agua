package com.ecoamazonas.eco_agua.restaurant;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class RestaurantDecimalFormat {
    private static final int PRECISE_SCALE = 4;

    private RestaurantDecimalFormat() {
    }

    static String money(BigDecimal value) {
        return safe(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    static String preciseMoney(BigDecimal value) {
        BigDecimal normalized = safe(value)
                .setScale(PRECISE_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        if (normalized.scale() < 2) {
            normalized = normalized.setScale(2, RoundingMode.UNNECESSARY);
        }

        return normalized.toPlainString();
    }

    static String quantity(BigDecimal value) {
        BigDecimal normalized = safe(value)
                .setScale(PRECISE_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0, RoundingMode.UNNECESSARY);
        }

        return normalized.toPlainString();
    }

    static String percentage(BigDecimal value) {
        return money(value);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
