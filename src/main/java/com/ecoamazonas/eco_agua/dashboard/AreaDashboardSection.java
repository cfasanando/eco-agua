package com.ecoamazonas.eco_agua.dashboard;

import java.util.List;

public class AreaDashboardSection {

    private final String key;
    private final String title;
    private final String subtitle;
    private final String icon;
    private final String statusLabel;
    private final String statusClass;
    private final String primaryActionLabel;
    private final String primaryActionUrl;
    private final List<AreaDashboardMetric> metrics;
    private final List<String> highlights;

    public AreaDashboardSection(
            String key,
            String title,
            String subtitle,
            String icon,
            String statusLabel,
            String statusClass,
            String primaryActionLabel,
            String primaryActionUrl,
            List<AreaDashboardMetric> metrics,
            List<String> highlights
    ) {
        this.key = key;
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
        this.statusLabel = statusLabel;
        this.statusClass = statusClass;
        this.primaryActionLabel = primaryActionLabel;
        this.primaryActionUrl = primaryActionUrl;
        this.metrics = metrics != null ? metrics : List.of();
        this.highlights = highlights != null ? highlights : List.of();
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getIcon() {
        return icon;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public String getPrimaryActionLabel() {
        return primaryActionLabel;
    }

    public String getPrimaryActionUrl() {
        return primaryActionUrl;
    }

    public List<AreaDashboardMetric> getMetrics() {
        return metrics;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public boolean hasPrimaryAction() {
        return primaryActionUrl != null && !primaryActionUrl.isBlank();
    }

    public boolean hasHighlights() {
        return !highlights.isEmpty();
    }
}
