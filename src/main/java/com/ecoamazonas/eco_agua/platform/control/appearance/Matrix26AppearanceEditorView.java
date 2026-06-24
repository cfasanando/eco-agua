package com.ecoamazonas.eco_agua.platform.control.appearance;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;

import java.util.List;
import java.util.Map;

public record Matrix26AppearanceEditorView(
        PlatformBusinessClient instance,
        Matrix26InstanceAppearance publishedAppearance,
        Matrix26InstanceAppearanceDraft draft,
        List<Matrix26ThemeCatalog> publicThemes,
        List<Matrix26ThemeCatalog> adminThemes,
        List<Matrix26LayoutCatalog> publicLayouts,
        List<Matrix26LayoutCatalog> adminLayouts,
        List<Matrix26LayoutCatalog> loginLayouts,
        Map<String, String> themeNames,
        Map<String, String> layoutNames,
        Map<String, Map<String, String>> themePresets
) {
}
