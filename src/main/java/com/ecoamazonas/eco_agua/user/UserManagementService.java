package com.ecoamazonas.eco_agua.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserManagementService {

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserAccountRepository userRepository,
                                 RoleRepository roleRepository,
                                 EmployeeService employeeService,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UserAccount findById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional
    public void saveFromForm(UserForm form) {
        // Load or create user
        UserAccount user;
        if (form.getUserId() != null) {
            user = userRepository.findById(form.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + form.getUserId()));
        } else {
            user = new UserAccount();
        }

        // Basic data
        user.setUsername(form.getUsername());

        // Convert boolean (form) to Integer/int (entity) -> 1 = active, 0 = inactive
        Integer activeFlag = form.isActive() ? 1 : 0;
        user.setActive(activeFlag);

        // Password: only update if something was typed
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (!form.getPassword().equals(form.getConfirmPassword())) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        } else if (form.getUserId() == null) {
            // For a new user, password is required
            throw new IllegalArgumentException("Password is required for a new user.");
        }

        // Role: assuming UserAccount has a roles collection (ManyToMany)
        if (form.getRoleId() != null) {
            Role role = roleRepository.findById(form.getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + form.getRoleId()));
            user.setRoles(Set.of(role)); // If your entity uses a single role instead, replace with user.setRole(role);
        }

        // Save user
        UserAccount savedUser = userRepository.save(user);

        // Save or update employee profile (HR data)
        employeeService.saveOrUpdateFromForm(savedUser, form);
    }
}
