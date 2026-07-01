package com.ecoamazonas.eco_agua.platform.control.modules.acceptance;

import java.util.List;

public record Matrix26FeatureFlagAcceptanceGroup(
        String title,
        String description,
        String icon,
        List<Matrix26FeatureFlagAcceptanceItem> items
) {
    public Matrix26FeatureFlagAcceptanceStatus status() {
        return items.stream()
                .map(Matrix26FeatureFlagAcceptanceItem::status)
                .max((left, right) -> Integer.compare(left.getSeverityRank(), right.getSeverityRank()))
                .orElse(Matrix26FeatureFlagAcceptanceStatus.NOT_TESTED);
    }

    public long passed() {
        return count(Matrix26FeatureFlagAcceptanceStatus.PASSED);
    }

    public long warnings() {
        return count(Matrix26FeatureFlagAcceptanceStatus.WARNING);
    }

    public long failed() {
        return count(Matrix26FeatureFlagAcceptanceStatus.FAILED);
    }

    public long notTested() {
        return count(Matrix26FeatureFlagAcceptanceStatus.NOT_TESTED);
    }

    private long count(Matrix26FeatureFlagAcceptanceStatus status) {
        return items.stream().filter(item -> item.status() == status).count();
    }
}
