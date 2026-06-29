package com.ecoamazonas.eco_agua.platform.control.security;

import com.ecoamazonas.eco_agua.platform.control.Matrix26ControlCenterProperties;
import com.ecoamazonas.eco_agua.user.Permission;
import com.ecoamazonas.eco_agua.user.PermissionRepository;
import com.ecoamazonas.eco_agua.user.Role;
import com.ecoamazonas.eco_agua.user.RoleRepository;
import com.ecoamazonas.eco_agua.user.UserAccount;
import com.ecoamazonas.eco_agua.user.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(name = "matrix26.control-center.enabled", havingValue = "true")
public class Matrix26ControlSecurityInitializer implements ApplicationRunner {

    private static final String SUPER_ADMIN_ROLE = "ROLE_SUPER_ADMIN";

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final Matrix26ControlCenterProperties properties;

    public Matrix26ControlSecurityInitializer(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserAccountRepository userAccountRepository,
            Matrix26ControlCenterProperties properties
    ) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Permission> permissions = seedPermissions();
        seedRoles(permissions);
        ensureBootstrapAdminRole();
    }

    private Map<String, Permission> seedPermissions() {
        Map<String, Permission> result = new LinkedHashMap<>();
        for (Matrix26ControlPermission definition : Matrix26ControlPermission.values()) {
            Permission permission = permissionRepository.findByCode(definition.code()).orElseGet(Permission::new);
            permission.setCode(definition.code());
            permission.setDescription(definition.label() + " - " + definition.description());
            result.put(definition.code(), permissionRepository.save(permission));
        }
        return result;
    }

    private void seedRoles(Map<String, Permission> permissions) {
        for (Matrix26ControlRole definition : Matrix26ControlRole.values()) {
            Role role = roleRepository.findByCode(definition.code()).orElseGet(Role::new);
            role.setCode(definition.code());
            role.setTitle(definition.title());
            role.getPermissions().clear();
            for (Matrix26ControlPermission permissionDefinition : definition.permissions()) {
                Permission permission = permissions.get(permissionDefinition.code());
                if (permission != null) {
                    role.getPermissions().add(permission);
                }
            }
            roleRepository.save(role);
        }
    }

    private void ensureBootstrapAdminRole() {
        String username = properties.getBootstrapAdminUsername();
        if (username == null || username.isBlank()) {
            return;
        }

        UserAccount user = userAccountRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return;
        }

        Role matrixAdmin = roleRepository.findByCode(Matrix26ControlRole.ADMIN.code()).orElse(null);
        Role superAdmin = roleRepository.findByCode(SUPER_ADMIN_ROLE).orElse(null);

        if (matrixAdmin != null && user.getRoles().stream().noneMatch(role -> Matrix26ControlRole.ADMIN.code().equals(role.getCode()))) {
            user.getRoles().add(matrixAdmin);
        }
        if (superAdmin != null && user.getRoles().stream().noneMatch(role -> SUPER_ADMIN_ROLE.equals(role.getCode()))) {
            user.getRoles().add(superAdmin);
        }

        userAccountRepository.save(user);
    }
}
