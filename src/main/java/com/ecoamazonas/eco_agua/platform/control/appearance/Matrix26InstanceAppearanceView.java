package com.ecoamazonas.eco_agua.platform.control.appearance;

import com.ecoamazonas.eco_agua.platform.PlatformBusinessClient;

public record Matrix26InstanceAppearanceView(
        PlatformBusinessClient instance,
        Matrix26InstanceAppearance appearance,
        Matrix26ThemeCatalog publicTheme,
        Matrix26LayoutCatalog publicLayout,
        Matrix26ThemeCatalog adminTheme,
        Matrix26LayoutCatalog adminLayout,
        Matrix26LayoutCatalog loginLayout
) {
}
