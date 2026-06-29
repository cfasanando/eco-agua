package com.ecoamazonas.eco_agua.platform.control.operations;

public record Matrix26OperationsDashboardInstance(
        String code,
        String name,
        String status,
        String runtimeStatus,
        String databaseName,
        Integer port,
        boolean protectedInstance,
        String attentionLevel,
        String attentionDetail,
        String href
) {
    public String badgeClass() {
        return switch (attentionLevel == null ? "INFO" : attentionLevel) {
            case "CRITICAL" -> "text-bg-danger";
            case "WARNING" -> "text-bg-warning";
            case "SUCCESS" -> "text-bg-success";
            default -> "text-bg-secondary";
        };
    }
}
