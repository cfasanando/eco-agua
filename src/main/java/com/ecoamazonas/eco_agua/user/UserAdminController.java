package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserManagementService userService;
    private final RoleRepository roleRepository;
    private final JobPositionService jobPositionService;
    private final EmployeeService employeeService;

    public UserAdminController(UserManagementService userService,
                               RoleRepository roleRepository,
                               JobPositionService jobPositionService,
                               EmployeeService employeeService) {
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

        return "admin/users"; // templates/admin/users.html
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute UserForm form,
                           RedirectAttributes redirectAttributes) {
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

    @GetMapping("/{id}/edit")
    public String editUser(@PathVariable("id") Integer id, Model model) {
        UserAccount user = userService.findAll().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        Employee employee = employeeService.findByUserId(id);

        UserForm form = new UserForm();
        form.setUserId(user.getId());
        form.setUsername(user.getUsername());
        form.setActive(user.isActive());

        // Single main role for UI
        user.getRoles().stream().findFirst().ifPresent(r -> form.setRoleId(r.getId()));

        if (employee != null) {
            form.setFirstName(employee.getFirstName());
            form.setLastName(employee.getLastName());
            form.setGender(employee.getGender());
            form.setBirthDate(employee.getBirthDate());
            form.setDni(employee.getDni());
            form.setEmail(employee.getEmail());
            form.setPhone(employee.getPhone());
            form.setAddress(employee.getAddress());
            form.setHireDate(employee.getHireDate());

            // jobPosition.id es Long → convertir a Integer porque el formulario usa Integer
            Integer jobPositionId = null;
            if (employee.getJobPosition() != null && employee.getJobPosition().getId() != null) {
                jobPositionId = employee.getJobPosition().getId().intValue();
            }
            form.setJobPositionId(jobPositionId);
        }

        model.addAttribute("activePage", "admin_users");
        model.addAttribute("userForm", form);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("jobPositions", jobPositionService.findActive());

        // Puedes usar un modal dentro de admin/users.html o esta vista aparte
        return "admin/user_form";
    }
}
