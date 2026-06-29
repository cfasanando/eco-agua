package com.ecoamazonas.eco_agua.platform.control.security;

import java.util.List;

public record Matrix26SecurityOverview(
        List<Matrix26SecurityRoleView> roles,
        List<Matrix26SecurityPermissionView> permissions,
        List<Matrix26SecurityUserView> users
) {
    public long matrix26UserCount() {
        return users.stream().filter(Matrix26SecurityUserView::matrix26User).count();
    }

    public long activeUserCount() {
        return users.stream().filter(Matrix26SecurityUserView::active).count();
    }
}
