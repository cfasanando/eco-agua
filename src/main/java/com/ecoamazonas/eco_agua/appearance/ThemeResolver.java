package com.ecoamazonas.eco_agua.appearance;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ThemeResolver {

    private static final Set<String> THEMES = Set.of(
            "matrix26-classic",
            "matrix26-nature",
            "matrix26-warm"
    );

    public String resolve(String code) {
        String normalized = normalize(code);
        return THEMES.contains(normalized) ? normalized : "matrix26-classic";
    }

    public String cssPath(String code) {
        return "/css/themes/" + resolve(code) + ".css";
    }

    public Map<String, String> defaults(String code) {
        String resolved = resolve(code);
        Map<String, String> result = new LinkedHashMap<>();
        switch (resolved) {
            case "matrix26-nature" -> {
                result.put("primaryColor", "#117A57");
                result.put("primaryHoverColor", "#0F6F51");
                result.put("secondaryColor", "#145A4A");
                result.put("accentColor", "#2AA7A1");
                result.put("backgroundColor", "#F3F8F3");
                result.put("surfaceColor", "#FFFFFF");
                result.put("textColor", "#173C32");
                result.put("textMutedColor", "#5D756D");
                result.put("borderColor", "#CFE2D6");
                result.put("sidebarBackground", "#123D33");
                result.put("sidebarBackgroundEnd", "#0D2F28");
                result.put("sidebarText", "#D6EFE6");
                result.put("radius", "18px");
                result.put("radiusSmall", "12px");
                result.put("radiusMedium", "18px");
                result.put("radiusLarge", "24px");
            }
            case "matrix26-warm" -> {
                result.put("primaryColor", "#B4532A");
                result.put("primaryHoverColor", "#963F1F");
                result.put("secondaryColor", "#5F2D1D");
                result.put("accentColor", "#D69A36");
                result.put("backgroundColor", "#FFF8F1");
                result.put("surfaceColor", "#FFFFFF");
                result.put("textColor", "#3F241B");
                result.put("textMutedColor", "#80645A");
                result.put("borderColor", "#ECD6C8");
                result.put("sidebarBackground", "#3E2119");
                result.put("sidebarBackgroundEnd", "#29140F");
                result.put("sidebarText", "#F7E5D9");
                result.put("radius", "18px");
                result.put("radiusSmall", "11px");
                result.put("radiusMedium", "18px");
                result.put("radiusLarge", "22px");
            }
            default -> {
                result.put("primaryColor", "#2563EB");
                result.put("primaryHoverColor", "#1D4ED8");
                result.put("secondaryColor", "#172554");
                result.put("accentColor", "#0891B2");
                result.put("backgroundColor", "#F4F7FB");
                result.put("surfaceColor", "#FFFFFF");
                result.put("textColor", "#172033");
                result.put("textMutedColor", "#64748B");
                result.put("borderColor", "#DBE3EF");
                result.put("sidebarBackground", "#0F172A");
                result.put("sidebarBackgroundEnd", "#111827");
                result.put("sidebarText", "#CBD5E1");
                result.put("radius", "16px");
                result.put("radiusSmall", "10px");
                result.put("radiusMedium", "16px");
                result.put("radiusLarge", "20px");
            }
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
