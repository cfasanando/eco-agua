package com.ecoamazonas.eco_agua.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeObligationSettlementRepository extends JpaRepository<EmployeeObligationSettlement, Long> {

    List<EmployeeObligationSettlement> findByEmployeeIdAndSettlementDateBetweenOrderBySettlementDateDescIdDesc(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<EmployeeObligationSettlement> findByEmployeeObligationIdOrderByIdDesc(Long employeeObligationId);

    boolean existsByEmployeeObligationId(Long employeeObligationId);
}
