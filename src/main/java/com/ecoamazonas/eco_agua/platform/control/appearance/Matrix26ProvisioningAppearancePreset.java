package com.ecoamazonas.eco_agua.platform.control.appearance;

public record Matrix26ProvisioningAppearancePreset(
        String code,
        String name,
        String description,
        String badge,
        String publicThemeCode,
        String publicLayoutCode,
        String adminThemeCode,
        String adminLayoutCode,
        String loginLayoutCode,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String backgroundColor,
        String surfaceColor,
        String textColor,
        String sidebarMode,
        String borderRadius,
        String tableDensity,
        String contentWidth,
        String headingStyle,
        boolean demoAssetsEnabled
) {
}
