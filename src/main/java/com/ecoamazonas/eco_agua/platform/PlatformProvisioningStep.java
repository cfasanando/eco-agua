package com.ecoamazonas.eco_agua.platform;

public record PlatformProvisioningStep(
        int number,
        String title,
        String status,
        String description,
        String badgeClass
) {
}
