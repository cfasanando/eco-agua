package com.ecoamazonas.eco_agua.platform.control.security;

import java.util.List;

public record Matrix26SecurityRoleView(
        String code,
        String title,
        String description,
        List<String> permissionCodes
) {
}
