package com.ecoamazonas.eco_agua.income;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OtherIncomeRepository extends JpaRepository<OtherIncome, Long> {

    List<OtherIncome> findByIncomeDateBetweenOrderByIncomeDateAsc(
            LocalDate startDate,
            LocalDate endDate
    );
}
