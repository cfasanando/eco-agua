package com.ecoamazonas.eco_agua.appearance;

import java.util.LinkedHashMap;
import java.util.Map;

public record InstanceBrandingConfiguration(
        boolean managed,
        Map<String, String> branding,
        Map<String, String> assets
) {
    public InstanceBrandingConfiguration {
        branding = branding == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(branding));
        assets = assets == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(assets));
    }

    public static InstanceBrandingConfiguration defaults() {
        return new InstanceBrandingConfiguration(false, Map.of(), Map.of());
    }

    public String branding(String key, String fallback) {
        String value = branding.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public String asset(String key) {
        return assets.get(key);
    }
}
