package com.ecoamazonas.eco_agua.platform.control.appearance;

public record Matrix26ThemeUsageView(
        Matrix26ThemeCatalog theme,
        long publicUsage,
        long adminUsage
) {
    public long totalUsage() {
        return publicUsage + adminUsage;
    }
}
