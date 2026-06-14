package com.ecoamazonas.eco_agua.academy;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
public class AcademyAccessModelAdvice {

    private static final Set<String> ACADEMY_VIEW_AUTHORITIES = Set.of(
            "ROLE_OWNER", "ROLE_MANAGEMENT", "ROLE_MARKETING", "ROLE_READONLY",
            "ADMIN_PRINC", "ADMIN", "ADMIN_MKT",
            "ver_academia", "administra_academia"
    );

    private static final Set<String> ACADEMY_WRITE_AUTHORITIES = Set.of(
            "ROLE_OWNER", "ROLE_MARKETING",
            "ADMIN_PRINC", "ADMIN", "ADMIN_MKT",
            "administra_academia"
    );

    @ModelAttribute("canViewAcademy")
    public boolean canViewAcademy() {
        return hasAny(ACADEMY_VIEW_AUTHORITIES) || isAuthenticated();
    }

    @ModelAttribute("canManageAcademy")
    public boolean canManageAcademy() {
        return hasAny(ACADEMY_WRITE_AUTHORITIES);
    }

    @ModelAttribute("academyReadOnly")
    public boolean academyReadOnly() {
        return hasAny(ACADEMY_VIEW_AUTHORITIES) && !hasAny(ACADEMY_WRITE_AUTHORITIES);
    }

    private boolean hasAny(Set<String> expectedAuthorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return expectedAuthorities.stream().anyMatch(authorities::contains);
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(authentication.getPrincipal()));
    }
}
