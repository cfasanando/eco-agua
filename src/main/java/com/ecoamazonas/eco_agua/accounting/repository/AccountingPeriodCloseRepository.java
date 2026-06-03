package com.ecoamazonas.eco_agua.accounting.repository;

import com.ecoamazonas.eco_agua.accounting.AccountingPeriodClose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountingPeriodCloseRepository extends JpaRepository<AccountingPeriodClose, Long> {

    Optional<AccountingPeriodClose> findByPeriodYearAndPeriodMonth(Integer periodYear, Integer periodMonth);
}
