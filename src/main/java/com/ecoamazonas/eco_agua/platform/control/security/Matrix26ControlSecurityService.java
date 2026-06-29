package com.ecoamazonas.eco_agua.platform.control.security;

import com.ecoamazonas.eco_agua.user.Role;
import com.ecoamazonas.eco_agua.user.UserAccount;
import com.ecoamazonas.eco_agua.user.UserAccountRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ControlSecurityService {

    private static final String SUPER_ADMIN_ROLE = "ROLE_SUPER_ADMIN";
    private static final String LEGACY_ADMIN_PRINC = "ADMIN_PRINC";
    private static final String LEGACY_ADMIN = "ADMIN";

    private final UserAccountRepository userAccountRepository;

    public Matrix26ControlSecurityService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public Matrix26SecurityOverview overview() {
        List<Matrix26SecurityRoleView> roles = Arrays.stream(Matrix26ControlRole.values())
                .map(role -> new Matrix26SecurityRoleView(
                        role.code(),
                        role.title(),
                        role.description(),
                        role.permissions().stream().map(Matrix26ControlPermission::code).sorted().toList()
                ))
                .toList();

        List<Matrix26SecurityPermissionView> permissions = Arrays.stream(Matrix26ControlPermission.values())
                .map(permission -> new Matrix26SecurityPermissionView(
                        permission.code(),
                        permission.label(),
                        permission.category(),
                        permission.description()
                ))
                .toList();

        List<Matrix26SecurityUserView> users = userAccountRepository.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(this::toUserView)
                .toList();

        return new Matrix26SecurityOverview(roles, permissions, users);
    }

    public boolean canView() {
        return hasAnyAuthority(Matrix26ControlRole.VIEWER.code(), Matrix26ControlPermission.VIEW.code(), SUPER_ADMIN_ROLE,
                LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canManageAlerts() {
        return hasAnyAuthority(Matrix26ControlRole.OPERATOR.code(), Matrix26ControlPermission.MANAGE_ALERTS.code(),
                Matrix26ControlRole.ADMIN.code(), SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canControlRuntimes() {
        return hasAnyAuthority(Matrix26ControlRole.OPERATOR.code(), Matrix26ControlPermission.CONTROL_RUNTIMES.code(),
                Matrix26ControlRole.ADMIN.code(), SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canManageBackups() {
        return hasAnyAuthority(Matrix26ControlRole.BACKUP_MANAGER.code(), Matrix26ControlPermission.MANAGE_BACKUPS.code(),
                Matrix26ControlRole.ADMIN.code(), SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canManageRestores() {
        return hasAnyAuthority(Matrix26ControlRole.RESTORE_MANAGER.code(), Matrix26ControlPermission.MANAGE_RESTORES.code(),
                Matrix26ControlRole.ADMIN.code(), SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canManageLifecycle() {
        return hasAnyAuthority(Matrix26ControlRole.LIFECYCLE_MANAGER.code(), Matrix26ControlPermission.MANAGE_LIFECYCLE.code(),
                Matrix26ControlRole.ADMIN.code(), SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canManagePurge() {
        return hasAnyAuthority(Matrix26ControlRole.PURGE_MANAGER.code(), Matrix26ControlPermission.MANAGE_PURGE.code(),
                Matrix26ControlRole.ADMIN.code(), SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canManageAppearance() {
        return hasAnyAuthority(Matrix26ControlPermission.MANAGE_APPEARANCE.code(), Matrix26ControlRole.ADMIN.code(),
                SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canManageProvisioning() {
        return hasAnyAuthority(Matrix26ControlPermission.MANAGE_PROVISIONING.code(), Matrix26ControlRole.ADMIN.code(),
                SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canManageModules() {
        return hasAnyAuthority(Matrix26ControlPermission.MANAGE_MODULES.code(), Matrix26ControlPermission.ADMINISTER_SETTINGS.code(),
                Matrix26ControlRole.ADMIN.code(), SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canAdministerSecurity() {
        return hasAnyAuthority(Matrix26ControlPermission.ADMINISTER_SECURITY.code(), Matrix26ControlRole.ADMIN.code(),
                SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    public boolean canAdministerSettings() {
        return hasAnyAuthority(Matrix26ControlPermission.ADMINISTER_SETTINGS.code(), Matrix26ControlRole.ADMIN.code(),
                SUPER_ADMIN_ROLE, LEGACY_ADMIN_PRINC, LEGACY_ADMIN);
    }

    private Matrix26SecurityUserView toUserView(UserAccount user) {
        List<String> roleCodes = user.getRoles().stream()
                .map(Role::getCode)
                .filter(value -> value != null && !value.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        Set<String> effectivePermissions = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            if (role.getCode() != null && role.getCode().startsWith("MATRIX26_")) {
                effectivePermissions.add(role.getCode());
            }
            role.getPermissions().forEach(permission -> {
                if (permission.getCode() != null && permission.getCode().startsWith("matrix26.")) {
                    effectivePermissions.add(permission.getCode());
                }
            });
        }
        boolean matrix26User = roleCodes.stream().anyMatch(role -> role.startsWith("MATRIX26_") || SUPER_ADMIN_ROLE.equals(role));
        return new Matrix26SecurityUserView(
                user.getUsername(),
                user.isActive(),
                roleCodes,
                effectivePermissions.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                matrix26User
        );
    }

    private boolean hasAnyAuthority(String... allowedAuthorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            return false;
        }

        Set<String> actual = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            actual.add(authority.getAuthority());
        }
        for (String allowedAuthority : allowedAuthorities) {
            if (actual.contains(allowedAuthority)) {
                return true;
            }
        }
        return false;
    }
}
