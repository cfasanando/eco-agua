package com.ecoamazonas.eco_agua.platform.control;

public record Matrix26ProvisioningModuleOption(
        String moduleKey,
        String name,
        String area,
        String description,
        boolean installerAvailable,
        String installerVersion,
        String availabilityLabel
) {
}
