package com.ecoamazonas.eco_agua.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final JobPositionRepository jobPositionRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           JobPositionRepository jobPositionRepository) {
        this.employeeRepository = employeeRepository;
        this.jobPositionRepository = jobPositionRepository;
    }

    @Transactional(readOnly = true)
    public Employee findByUserId(Integer userId) {
        return employeeRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public Employee saveOrUpdateFromForm(UserAccount user, UserForm form) {

        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseGet(Employee::new);

        employee.setUser(user);
        employee.setFirstName(form.getFirstName());
        employee.setLastName(form.getLastName());
        employee.setGender(form.getGender());
        employee.setBirthDate(form.getBirthDate());
        employee.setDni(form.getDni());
        employee.setEmail(form.getEmail());
        employee.setPhone(form.getPhone());
        employee.setAddress(form.getAddress());
        employee.setHireDate(form.getHireDate() != null ? form.getHireDate() : LocalDate.now());
        employee.setActive(true);

        if (form.getJobPositionId() != null) {
            // form.getJobPositionId() es Integer → convertir a Long para el repositorio
            Long positionId = form.getJobPositionId().longValue();

            JobPosition pos = jobPositionRepository.findById(positionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Job position not found: " + form.getJobPositionId()
                    ));
            employee.setJobPosition(pos);
        } else {
            employee.setJobPosition(null);
        }

        return employeeRepository.save(employee);
    }
}
