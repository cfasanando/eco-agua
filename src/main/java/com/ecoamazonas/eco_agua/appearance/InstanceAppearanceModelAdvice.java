package com.ecoamazonas.eco_agua.appearance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@ControllerAdvice
@ConditionalOnProperty(
        name = "matrix26.control-center.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class InstanceAppearanceModelAdvice {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Set<String> SIDEBAR_MODES = Set.of("THEME", "LIGHT", "DARK");
    private static final Set<String> BORDER_RADII = Set.of("THEME", "SMALL", "MEDIUM", "LARGE");
    private static final Set<String> TABLE_DENSITIES = Set.of("COMPACT", "COMFORTABLE", "SPACIOUS");
    private static final Set<String> CONTENT_WIDTHS = Set.of("STANDARD", "WIDE", "FULL");
    private static final Set<String> HEADING_STYLES = Set.of("SYSTEM", "STRONG", "EDITORIAL");

    private final InstanceAppearanceConfigurationService configurationService;
    private final ThemeResolver themeResolver;
    private final LayoutResolver layoutResolver;

    public InstanceAppearanceModelAdvice(
            InstanceAppearanceConfigurationService configurationService,
            ThemeResolver themeResolver,
            LayoutResolver layoutResolver
    ) {
        this.configurationService = configurationService;
        this.themeResolver = themeResolver;
        this.layoutResolver = layoutResolver;
    }

    @ModelAttribute
    public void addAppearanceAttributes(Model model) {
        InstanceAppearanceConfiguration configuration = configurationService.current();

        String publicTheme = themeResolver.resolve(configuration.publicThemeCode());
        String adminTheme = themeResolver.resolve(configuration.adminThemeCode());
        String publicLayout = layoutResolver.resolvePublic(configuration.publicLayoutCode());
        String adminLayout = layoutResolver.resolveAdmin(configuration.adminLayoutCode());
        String loginLayout = layoutResolver.resolveLogin(configuration.loginLayoutCode());

        Map<String, String> publicValues = effectiveValues(publicTheme, configuration.overrides());
        Map<String, String> adminValues = effectiveValues(adminTheme, configuration.overrides());
        String publicCssVariables = cssVariables(publicValues);
        String adminCssVariables = cssVariables(adminValues);

        model.addAttribute("appearanceManaged", configuration.managed());
        model.addAttribute("appearancePublicThemeCode", publicTheme);
        model.addAttribute("appearancePublicThemeCss", themeResolver.cssPath(publicTheme));
        model.addAttribute("appearancePublicLayoutCode", publicLayout);
        model.addAttribute("appearancePublicLayoutCss", layoutResolver.publicCssPath(publicLayout));
        model.addAttribute("appearancePublicCssVariables", publicCssVariables);

        model.addAttribute("appearanceAdminThemeCode", adminTheme);
        model.addAttribute("appearanceAdminThemeCss", themeResolver.cssPath(adminTheme));
        model.addAttribute("appearanceAdminLayoutCode", adminLayout);
        model.addAttribute("appearanceAdminLayoutCss", layoutResolver.adminCssPath(adminLayout));
        model.addAttribute("appearanceAdminCssVariables", adminCssVariables);

        model.addAttribute("appearanceLoginLayoutCode", loginLayout);
        model.addAttribute("appearanceLoginLayoutCss", layoutResolver.loginCssPath(loginLayout));

        // Kept for backward compatibility with templates outside the shared head fragments.
        model.addAttribute("appearanceCssVariables", adminCssVariables);
        model.addAttribute("appearancePrimaryColor", adminValues.get("primaryColor"));
        model.addAttribute("appearancePrimaryHoverColor", adminValues.get("primaryHoverColor"));
        model.addAttribute("appearancePublishedVersion", configuration.publishedVersion());
        model.addAttribute("appearancePublishedAt", configuration.publishedAt());
        model.addAttribute("appearancePublishedBy", configuration.publishedBy());
    }

    private Map<String, String> effectiveValues(String themeCode, Map<String, String> overrides) {
        Map<String, String> values = new LinkedHashMap<>(themeResolver.defaults(themeCode));

        boolean explicitPaletteMode = overrides.containsKey("customPalette");
        boolean applyPaletteOverrides = !explicitPaletteMode
                || Boolean.parseBoolean(overrides.get("customPalette"));
        if (applyPaletteOverrides) {
            putColor(values, "primaryColor", overrides.get("primaryColor"));
            putColor(values, "secondaryColor", overrides.get("secondaryColor"));
            putColor(values, "accentColor", overrides.get("accentColor"));
            putColor(values, "backgroundColor", overrides.get("backgroundColor"));
            putColor(values, "surfaceColor", overrides.get("surfaceColor"));
            putColor(values, "textColor", overrides.get("textColor"));
        }

        values.put("primaryHoverColor", darken(values.get("primaryColor"), 0.15));
        values.put("primaryTextColor", contrastText(values.get("primaryColor")));
        values.put("primaryHoverTextColor", contrastText(values.get("primaryHoverColor")));
        values.put("secondaryTextColor", contrastText(values.get("secondaryColor")));
        values.put("accentTextColor", contrastText(values.get("accentColor")));
        values.put("surfaceTextColor", contrastText(values.get("surfaceColor")));
        values.put("sidebarMode", allowedOption(overrides.get("sidebarMode"), SIDEBAR_MODES, "THEME"));
        values.put("borderRadius", allowedOption(overrides.get("borderRadius"), BORDER_RADII, "THEME"));
        values.put("tableDensity", allowedOption(overrides.get("tableDensity"), TABLE_DENSITIES, "COMFORTABLE"));
        values.put("contentWidth", allowedOption(overrides.get("contentWidth"), CONTENT_WIDTHS, "STANDARD"));
        values.put("headingStyle", allowedOption(overrides.get("headingStyle"), HEADING_STYLES, "SYSTEM"));

        applyComposition(values);
        return values;
    }

    private void applyComposition(Map<String, String> values) {
        switch (values.get("borderRadius")) {
            case "SMALL" -> {
                values.put("radiusSmallValue", "8px");
                values.put("radiusMediumValue", "10px");
                values.put("radiusLargeValue", "14px");
            }
            case "MEDIUM" -> {
                values.put("radiusSmallValue", "10px");
                values.put("radiusMediumValue", "16px");
                values.put("radiusLargeValue", "20px");
            }
            case "LARGE" -> {
                values.put("radiusSmallValue", "14px");
                values.put("radiusMediumValue", "24px");
                values.put("radiusLargeValue", "30px");
            }
            default -> {
                values.put("radiusSmallValue", values.get("radiusSmall"));
                values.put("radiusMediumValue", values.get("radiusMedium"));
                values.put("radiusLargeValue", values.get("radiusLarge"));
            }
        }
        values.put("tableDensityValue", switch (values.get("tableDensity")) {
            case "COMPACT" -> "8px";
            case "SPACIOUS" -> "18px";
            default -> "13px";
        });
        values.put("contentWidthValue", switch (values.get("contentWidth")) {
            case "WIDE" -> "1800px";
            case "FULL" -> "100%";
            default -> "1600px";
        });
        values.put("headingFont", switch (values.get("headingStyle")) {
            case "STRONG" -> "Inter, ui-sans-serif, system-ui, sans-serif";
            case "EDITORIAL" -> "Georgia, Cambria, \"Times New Roman\", serif";
            default -> "var(--theme-font-body)";
        });

        switch (values.get("sidebarMode")) {
            case "LIGHT" -> {
                values.put("sidebarBackground", values.get("surfaceColor"));
                values.put("sidebarBackgroundEnd", values.get("backgroundColor"));
                values.put("sidebarText", values.get("textColor"));
            }
            case "DARK" -> {
                values.put("sidebarBackground", "#111827");
                values.put("sidebarBackgroundEnd", "#0B1220");
                values.put("sidebarText", "#F8FAFC");
            }
            default -> {
                // Keep theme defaults.
            }
        }
    }

    private String cssVariables(Map<String, String> values) {
        return """
                :root {
                  --theme-primary: %s;
                  --theme-primary-hover: %s;
                  --theme-on-primary: %s;
                  --theme-on-primary-hover: %s;
                  --theme-secondary: %s;
                  --theme-on-secondary: %s;
                  --theme-accent: %s;
                  --theme-on-accent: %s;
                  --theme-background: %s;
                  --theme-surface: %s;
                  --theme-on-surface: %s;
                  --theme-text: %s;
                  --theme-sidebar-background: %s;
                  --theme-sidebar-background-end: %s;
                  --theme-sidebar-text: %s;
                  --theme-radius-small: %s;
                  --theme-radius-medium: %s;
                  --theme-radius-large: %s;
                  --theme-table-density: %s;
                  --theme-content-width: %s;
                  --theme-font-heading: %s;
                  --eco-primary: %s;
                  --eco-secondary: %s;
                  --login-primary-color: %s;
                  --login-primary-hover: %s;
                  --login-button-text: %s;
                  --login-button-hover-text: %s;
                }
                """.formatted(
                values.get("primaryColor"),
                values.get("primaryHoverColor"),
                values.get("primaryTextColor"),
                values.get("primaryHoverTextColor"),
                values.get("secondaryColor"),
                values.get("secondaryTextColor"),
                values.get("accentColor"),
                values.get("accentTextColor"),
                values.get("backgroundColor"),
                values.get("surfaceColor"),
                values.get("surfaceTextColor"),
                values.get("textColor"),
                values.get("sidebarBackground"),
                values.get("sidebarBackgroundEnd"),
                values.get("sidebarText"),
                values.get("radiusSmallValue"),
                values.get("radiusMediumValue"),
                values.get("radiusLargeValue"),
                values.get("tableDensityValue"),
                values.get("contentWidthValue"),
                values.get("headingFont"),
                values.get("primaryColor"),
                values.get("secondaryColor"),
                values.get("primaryColor"),
                values.get("primaryHoverColor"),
                values.get("primaryTextColor"),
                values.get("primaryHoverTextColor")
        );
    }

    private void putColor(Map<String, String> values, String key, String candidate) {
        if (candidate != null && HEX_COLOR.matcher(candidate.trim()).matches()) {
            values.put(key, candidate.trim().toUpperCase(Locale.ROOT));
        }
    }

    private String allowedOption(String candidate, Set<String> allowed, String fallback) {
        String normalized = candidate == null ? "" : candidate.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private String contrastText(String background) {
        if (background == null || !HEX_COLOR.matcher(background).matches()) {
            return "#FFFFFF";
        }
        double whiteContrast = contrastRatio("#FFFFFF", background);
        double darkContrast = contrastRatio("#111827", background);
        return whiteContrast >= darkContrast ? "#FFFFFF" : "#111827";
    }

    private double contrastRatio(String first, String second) {
        double firstLum = luminance(first);
        double secondLum = luminance(second);
        double light = Math.max(firstLum, secondLum);
        double dark = Math.min(firstLum, secondLum);
        return (light + 0.05) / (dark + 0.05);
    }

    private double luminance(String color) {
        int red = Integer.parseInt(color.substring(1, 3), 16);
        int green = Integer.parseInt(color.substring(3, 5), 16);
        int blue = Integer.parseInt(color.substring(5, 7), 16);
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue);
    }

    private double channel(int value) {
        double normalized = value / 255.0;
        return normalized <= 0.03928
                ? normalized / 12.92
                : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }

    private String darken(String color, double factor) {
        if (color == null || !HEX_COLOR.matcher(color).matches()) {
            return "#1D4ED8";
        }
        int red = Integer.parseInt(color.substring(1, 3), 16);
        int green = Integer.parseInt(color.substring(3, 5), 16);
        int blue = Integer.parseInt(color.substring(5, 7), 16);
        red = (int) Math.round(red * (1.0 - factor));
        green = (int) Math.round(green * (1.0 - factor));
        blue = (int) Math.round(blue * (1.0 - factor));
        return String.format("#%02X%02X%02X", red, green, blue);
    }
}
