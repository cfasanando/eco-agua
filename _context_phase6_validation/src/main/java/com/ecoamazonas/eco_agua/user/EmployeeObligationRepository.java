package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeObligationRepository extends JpaRepository<EmployeeObligation, Long> {

    List<EmployeeObligation> findByEmployeeIdOrderByIssueDateDescIdDesc(Long employeeId);

    List<EmployeeObligation> findByEmployeeIdAndActiveTrueOrderByIssueDateDescIdDesc(Long employeeId);

    Optional<EmployeeObligation> findByIdAndEmployeeId(Long id, Long employeeId);
}
