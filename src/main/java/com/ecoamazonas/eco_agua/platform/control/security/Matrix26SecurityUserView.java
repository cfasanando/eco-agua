package com.ecoamazonas.eco_agua.platform.control.security;

import java.util.List;

public record Matrix26SecurityUserView(
        String username,
        boolean active,
        List<String> roles,
        List<String> effectivePermissions,
        boolean matrix26User
) {
}
