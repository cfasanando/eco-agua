package com.ecoamazonas.eco_agua.appearance;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class LayoutResolver {

    private static final Set<String> PUBLIC_LAYOUTS = Set.of(
            "public-classic-grid",
            "public-nature-editorial",
            "public-restaurant-visual"
    );
    private static final Set<String> ADMIN_LAYOUTS = Set.of(
            "admin-sidebar-classic",
            "admin-compact-workspace"
    );
    private static final Set<String> LOGIN_LAYOUTS = Set.of("login-split");

    public String resolvePublic(String code) {
        String normalized = normalize(code);
        return PUBLIC_LAYOUTS.contains(normalized) ? normalized : "public-classic-grid";
    }

    public String resolveAdmin(String code) {
        String normalized = normalize(code);
        return ADMIN_LAYOUTS.contains(normalized) ? normalized : "admin-sidebar-classic";
    }

    public String resolveLogin(String code) {
        String normalized = normalize(code);
        return LOGIN_LAYOUTS.contains(normalized) ? normalized : "login-split";
    }

    public String publicCssPath(String code) {
        return "/css/layouts/" + resolvePublic(code) + ".css";
    }

    public String adminCssPath(String code) {
        return "/css/layouts/" + resolveAdmin(code) + ".css";
    }

    public String loginCssPath(String code) {
        return "/css/layouts/" + resolveLogin(code) + ".css";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
