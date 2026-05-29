package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/roles-permissions")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    public RolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

   @GetMapping
    public String list(@RequestParam(name = "roleId", required = false) Integer roleId,
                       Model model) {

        List<Role> roles = rolePermissionService.findAllRoles();
        if (roles.isEmpty()) {
            model.addAttribute("activePage", "admin_roles_permissions");
            model.addAttribute("roles", roles);
            model.addAttribute("currentRole", null);
            model.addAttribute("permissions", List.of());
            model.addAttribute("assignedPermissionIds", Set.of());
            return "admin/roles_permissions";
        }

        Role currentRole = (roleId != null)
                ? rolePermissionService.findRoleById(roleId)
                : roles.get(0);

        List<Permission> permissions = rolePermissionService.findAllPermissions();
        Set<Integer> assignedIds = currentRole.getPermissions().stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        model.addAttribute("activePage", "admin_roles_permissions");
        model.addAttribute("roles", roles);
        model.addAttribute("currentRole", currentRole);
        model.addAttribute("permissions", permissions);
        model.addAttribute("assignedPermissionIds", assignedIds);

        return "admin/roles_permissions";
    }

    @PostMapping("/save")
    public String updateRolePermissions(
            @RequestParam("roleId") Integer roleId,
            @RequestParam(name = "permissionIds", required = false) List<Integer> permissionIds,
            RedirectAttributes redirectAttributes
    ) {
        try {
            rolePermissionService.updateRolePermissions(roleId, permissionIds);
            redirectAttributes.addFlashAttribute("message", "Role permissions updated successfully.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("message", "Error updating role permissions: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/roles-permissions?roleId=" + roleId;
    }

}
