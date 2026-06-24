package com.ecoamazonas.eco_agua.platform.control.appearance;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum Matrix26BrandingAssetType {
    LOGO_PRIMARY("logo-primary", "Logo principal", "600 × 200", 200, 60, 5_000_000L, List.of("png", "jpg", "jpeg", "webp")),
    LOGO_COMPACT("logo-compact", "Logo compacto", "256 × 256", 96, 96, 3_000_000L, List.of("png", "jpg", "jpeg", "webp")),
    FAVICON("favicon", "Favicon", "64 × 64", 32, 32, 1_000_000L, List.of("png", "ico")),
    LOGIN_COVER("login-cover", "Imagen de login", "1400 × 1000", 900, 600, 8_000_000L, List.of("png", "jpg", "jpeg", "webp")),
    HERO_PRIMARY("hero-primary", "Hero principal", "1600 × 900", 1200, 600, 10_000_000L, List.of("png", "jpg", "jpeg", "webp")),
    HERO_SECONDARY("hero-secondary", "Hero secundario", "1200 × 800", 900, 500, 8_000_000L, List.of("png", "jpg", "jpeg", "webp")),
    PRODUCT_PLACEHOLDER("product-placeholder", "Placeholder de producto", "800 × 800", 500, 500, 6_000_000L, List.of("png", "jpg", "jpeg", "webp")),
    SOCIAL_SHARE("social-share", "Imagen social", "1200 × 630", 1000, 500, 8_000_000L, List.of("png", "jpg", "jpeg", "webp"));

    private final String code;
    private final String label;
    private final String recommendedSize;
    private final int minimumWidth;
    private final int minimumHeight;
    private final long maximumBytes;
    private final List<String> extensions;

    Matrix26BrandingAssetType(
            String code,
            String label,
            String recommendedSize,
            int minimumWidth,
            int minimumHeight,
            long maximumBytes,
            List<String> extensions
    ) {
        this.code = code;
        this.label = label;
        this.recommendedSize = recommendedSize;
        this.minimumWidth = minimumWidth;
        this.minimumHeight = minimumHeight;
        this.maximumBytes = maximumBytes;
        this.extensions = extensions;
    }

    public String code() { return code; }
    public String label() { return label; }
    public String recommendedSize() { return recommendedSize; }
    public int minimumWidth() { return minimumWidth; }
    public int minimumHeight() { return minimumHeight; }
    public long maximumBytes() { return maximumBytes; }
    public List<String> extensions() { return extensions; }

    public static Optional<Matrix26BrandingAssetType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(item -> item.code.equals(normalized)).findFirst();
    }
}
