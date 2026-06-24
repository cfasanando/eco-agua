package com.ecoamazonas.eco_agua.platform.control.appearance;

import java.util.Map;

public record Matrix26ProvisioningAppearanceSummary(
        String presetName,
        String publicThemeName,
        String publicLayoutName,
        String adminThemeName,
        String adminLayoutName,
        String loginLayoutName,
        Map<String, String> overrides,
        Map<String, String> branding,
        boolean demoAssetsEnabled
) {
}
