package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeObligationPaymentRepository extends JpaRepository<EmployeeObligationPayment, Long> {

    boolean existsByEmployeeObligationId(Long employeeObligationId);

    List<EmployeeObligationPayment> findByEmployeeObligationIdOrderByIdDesc(Long employeeObligationId);
}
