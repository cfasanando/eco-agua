package com.ecoamazonas.eco_agua.platform.module;

import java.time.LocalDateTime;

public record PlatformModuleInstallationStatus(
        String moduleKey,
        String displayName,
        String installedVersion,
        String targetVersion,
        String status,
        boolean enabled,
        boolean schemaInstalled,
        boolean registered,
        String currentStep,
        String lastError,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt
) {
    public boolean requiresInstallation() {
        return !schemaInstalled;
    }

    public boolean requiresSynchronization() {
        return schemaInstalled && !registered;
    }

    public boolean updateAvailable() {
        return schemaInstalled
                && registered
                && targetVersion != null
                && !targetVersion.equals(installedVersion);
    }

    public boolean failed() {
        return "FAILED".equalsIgnoreCase(status);
    }

    public String statusLabel() {
        if (requiresSynchronization()) {
            return "Installed without registry";
        }
        return switch (status == null ? "" : status.toUpperCase()) {
            case "ACTIVE" -> "Active";
            case "DISABLED" -> "Disabled";
            case "INSTALLING" -> "Installing";
            case "FAILED" -> "Failed";
            case "INSTALLED" -> "Installed";
            default -> schemaInstalled ? "Installed" : "Not installed";
        };
    }
}
