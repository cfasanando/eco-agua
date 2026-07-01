package com.ecoamazonas.eco_agua.platform.control.modules.acceptance;

import java.time.LocalDateTime;
import java.util.List;

public record Matrix26FeatureFlagAcceptanceMatrix(
        LocalDateTime generatedAt,
        Matrix26FeatureFlagAcceptanceStatus overallStatus,
        List<Matrix26FeatureFlagAcceptanceMetric> metrics,
        List<Matrix26FeatureFlagAcceptanceGroup> groups,
        List<Matrix26FeatureFlagAcceptanceRisk> risks,
        List<String> notes
) {
    public long totalItems() {
        return groups.stream().mapToLong(group -> group.items().size()).sum();
    }

    public long passedItems() {
        return count(Matrix26FeatureFlagAcceptanceStatus.PASSED);
    }

    public long warningItems() {
        return count(Matrix26FeatureFlagAcceptanceStatus.WARNING);
    }

    public long failedItems() {
        return count(Matrix26FeatureFlagAcceptanceStatus.FAILED);
    }

    public long notTestedItems() {
        return count(Matrix26FeatureFlagAcceptanceStatus.NOT_TESTED);
    }

    private long count(Matrix26FeatureFlagAcceptanceStatus status) {
        return groups.stream()
                .flatMap(group -> group.items().stream())
                .filter(item -> item.status() == status)
                .count();
    }
}
