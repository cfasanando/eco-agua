package com.ecoamazonas.eco_agua.platform.control.operations.acceptance;

public record Matrix26AcceptanceItem(
        String code,
        String title,
        String description,
        Matrix26AcceptanceStatus status,
        String evidence,
        String route,
        String recommendedAction
) {
    public boolean actionable() {
        return recommendedAction != null && !recommendedAction.isBlank();
    }
}
