package com.ecoamazonas.eco_agua.platform.control.modules.acceptance;

public record Matrix26FeatureFlagAcceptanceItem(
        String code,
        String title,
        String description,
        Matrix26FeatureFlagAcceptanceStatus status,
        String evidence,
        String route,
        String recommendedAction
) {
    public boolean hasRoute() {
        return route != null && !route.isBlank();
    }

    public boolean actionable() {
        return recommendedAction != null && !recommendedAction.isBlank();
    }
}
