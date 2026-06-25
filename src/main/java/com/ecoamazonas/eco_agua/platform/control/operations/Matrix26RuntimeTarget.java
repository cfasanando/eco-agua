package com.ecoamazonas.eco_agua.platform.control.operations;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;
import com.ecoamazonas.eco_agua.platform.control.Matrix26ControlCenterProperties;

public record Matrix26RuntimeTarget(
        String key,
        Long instanceId,
        String code,
        String businessName,
        String runtimeProfile,
        Integer expectedPort,
        String publicUrl,
        String databaseName,
        String runtimeCommand,
        String managementMode,
        boolean protectedInstance,
        boolean controlCenter
) {
    public static Matrix26RuntimeTarget controlCenter(Matrix26ControlCenterProperties properties) {
        return new Matrix26RuntimeTarget(
                "control",
                null,
                "matrix26-control",
                properties.getDisplayName(),
                properties.getRuntimeProfile(),
                portFromUrl(properties.getPortalUrl(), 8091),
                properties.getPortalUrl(),
                properties.getDatabaseName(),
                "bash scripts/run-matrix26-control.sh",
                "CONTROL_CENTER",
                true,
                true
        );
    }

    public static Matrix26RuntimeTarget fromInstance(PlatformBusinessClient instance) {
        return new Matrix26RuntimeTarget(
                String.valueOf(instance.getId()),
                instance.getId(),
                instance.getCode(),
                instance.getBusinessName(),
                instance.getRuntimeProfile(),
                instance.getRuntimePort(),
                instance.getPublicUrl(),
                instance.getDatabaseName(),
                instance.getRuntimeCommand(),
                instance.getManagementMode(),
                instance.isProtectedInstance(),
                false
        );
    }

    private static int portFromUrl(String value, int fallback) {
        try {
            java.net.URI uri = java.net.URI.create(value);
            return uri.getPort() > 0 ? uri.getPort() : fallback;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }
}
