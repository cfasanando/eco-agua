package com.ecoamazonas.eco_agua.platform.control.operations.acceptance;

import java.time.LocalDateTime;
import java.util.List;

public record Matrix26AcceptanceMatrix(
        LocalDateTime generatedAt,
        Matrix26AcceptanceStatus overallStatus,
        List<Matrix26AcceptanceMetric> metrics,
        List<Matrix26AcceptanceGroup> groups,
        List<Matrix26AcceptanceRisk> risks,
        List<String> notes
) {
    public long totalItems() {
        return groups.stream().mapToLong(group -> group.items().size()).sum();
    }

    public long passedItems() {
        return count(Matrix26AcceptanceStatus.PASSED);
    }

    public long warningItems() {
        return count(Matrix26AcceptanceStatus.WARNING);
    }

    public long failedItems() {
        return count(Matrix26AcceptanceStatus.FAILED);
    }

    public long notTestedItems() {
        return count(Matrix26AcceptanceStatus.NOT_TESTED);
    }

    private long count(Matrix26AcceptanceStatus status) {
        return groups.stream()
                .flatMap(group -> group.items().stream())
                .filter(item -> item.status() == status)
                .count();
    }
}
