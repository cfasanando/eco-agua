package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RolePermissionService(RoleRepository roleRepository,
                                 PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Permission> findAllPermissions() {
        return permissionRepository.findAllOrdered();
    }

    @Transactional(readOnly = true)
    public Role findRoleById(Integer roleId) {
        return roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
    }

    @Transactional
    public void updateRolePermissions(Integer roleId, List<Integer> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        Set<Permission> newPermissions = new HashSet<>();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(permissionIds);
            newPermissions.addAll(permissions);
        }

        role.setPermissions(newPermissions);
        roleRepository.save(role);
    }
}
