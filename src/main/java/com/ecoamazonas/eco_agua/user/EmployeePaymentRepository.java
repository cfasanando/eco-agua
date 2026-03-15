package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeePaymentRepository extends JpaRepository<EmployeePayment, Long> {

    List<EmployeePayment> findByEmployeeIdAndPaymentDateBetweenOrderByPaymentDateDescIdDesc(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );
}
