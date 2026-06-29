package com.ecoamazonas.eco_agua.platform.control.operations;

public record Matrix26OperationsDashboardAlert(
        String severity,
        String icon,
        String title,
        String detail,
        String href,
        String actionLabel
) {
    public String badgeClass() {
        return switch (severity == null ? "INFO" : severity) {
            case "CRITICAL" -> "text-bg-danger";
            case "WARNING" -> "text-bg-warning";
            case "SUCCESS" -> "text-bg-success";
            default -> "text-bg-info";
        };
    }
}
