package com.ecoamazonas.eco_agua.appearance;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record InstanceAppearanceConfiguration(
        boolean managed,
        String publicThemeCode,
        String publicLayoutCode,
        String adminThemeCode,
        String adminLayoutCode,
        String loginLayoutCode,
        Map<String, String> overrides,
        int publishedVersion,
        LocalDateTime publishedAt,
        String publishedBy
) {

    public InstanceAppearanceConfiguration {
        overrides = overrides == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(overrides));
    }

    public static InstanceAppearanceConfiguration defaults() {
        return new InstanceAppearanceConfiguration(
                false,
                "matrix26-classic",
                "public-classic-grid",
                "matrix26-classic",
                "admin-sidebar-classic",
                "login-split",
                Map.of(),
                0,
                null,
                null
        );
    }
}
