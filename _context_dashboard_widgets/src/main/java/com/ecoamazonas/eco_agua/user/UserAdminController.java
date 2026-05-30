package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserManagementService userService;
    private final RoleRepository roleRepository;
    private final JobPositionService jobPositionService;
    private final EmployeeService employeeService;

    public UserAdminController(
            UserManagementService userService,
            RoleRepository roleRepository,
            JobPositionService jobPositionService,
            EmployeeService employeeService
    ) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.jobPositionService = jobPositionService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String listUsers(Model model) {
        List<UserAccount> users = userService.findAll();

        model.addAttribute("activePage", "admin_users");
        model.addAttribute("users", users);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("jobPositions", jobPositionService.findActive());

        return "admin/users";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute UserForm form, RedirectAttributes redirectAttributes) {
        try {
            userService.saveFromForm(form);
            redirectAttributes.addFlashAttribute("message", "User saved successfully.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Error saving user: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public Map<String, Object> getUserData(@PathVariable("id") Integer id) {
        UserAccount user = userService.findById(id);
        Employee employee = employeeService.findByUserId(id);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", user.getId());
        payload.put("username", safe(user.getUsername()));
        payload.put("active", user.isActive());
        payload.put("roleId", resolveMainRoleId(user));

        payload.put("firstName", employee != null ? safe(employee.getFirstName()) : "");
        payload.put("lastName", employee != null ? safe(employee.getLastName()) : "");
        payload.put("gender", employee != null && employee.getGender() != null ? employee.getGender().name() : "");
        payload.put("birthDate", employee != null && employee.getBirthDate() != null ? employee.getBirthDate().toString() : "");
        payload.put("dni", employee != null ? safe(employee.getDni()) : "");
        payload.put("email", employee != null ? safe(employee.getEmail()) : "");
        payload.put("phone", employee != null ? safe(employee.getPhone()) : "");
        payload.put("address", employee != null ? safe(employee.getAddress()) : "");
        payload.put(
                "jobPositionId",
                employee != null && employee.getJobPosition() != null && employee.getJobPosition().getId() != null
                        ? employee.getJobPosition().getId().intValue()
                        : null
        );
        payload.put("hireDate", employee != null && employee.getHireDate() != null ? employee.getHireDate().toString() : "");

        return payload;
    }

    @GetMapping("/{id}/edit")
    public String editUser(@PathVariable("id") Integer id) {
        userService.findById(id);
        return "redirect:/admin/users";
    }

    private Integer resolveMainRoleId(UserAccount user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return null;
        }

        Object firstRole = user.getRoles().iterator().next();
        if (firstRole instanceof Role role) {
            return role.getId();
        }

        return null;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}