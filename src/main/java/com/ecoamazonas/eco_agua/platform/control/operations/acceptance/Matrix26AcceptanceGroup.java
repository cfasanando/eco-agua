package com.ecoamazonas.eco_agua.platform.control.operations.acceptance;

import java.util.List;

public record Matrix26AcceptanceGroup(
        String title,
        String description,
        String icon,
        List<Matrix26AcceptanceItem> items
) {
    public Matrix26AcceptanceStatus status() {
        return items.stream()
                .map(Matrix26AcceptanceItem::status)
                .max((left, right) -> Integer.compare(left.getSeverityRank(), right.getSeverityRank()))
                .orElse(Matrix26AcceptanceStatus.NOT_TESTED);
    }

    public long passed() {
        return count(Matrix26AcceptanceStatus.PASSED);
    }

    public long warnings() {
        return count(Matrix26AcceptanceStatus.WARNING);
    }

    public long failed() {
        return count(Matrix26AcceptanceStatus.FAILED);
    }

    public long notTested() {
        return count(Matrix26AcceptanceStatus.NOT_TESTED);
    }

    private long count(Matrix26AcceptanceStatus status) {
        return items.stream().filter(item -> item.status() == status).count();
    }
}
