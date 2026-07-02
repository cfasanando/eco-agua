package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalFinanceDebtRepository extends JpaRepository<PersonalFinanceDebt, Long> {
    List<PersonalFinanceDebt> findByUserOrderByStatusAscDueDayAscNameAsc(UserAccount user);
    List<PersonalFinanceDebt> findByUserAndStatusOrderByDueDayAscNameAsc(UserAccount user, PersonalFinanceDebtStatus status);
    Optional<PersonalFinanceDebt> findByIdAndUser(Long id, UserAccount user);
    long countByUserAndStatus(UserAccount user, PersonalFinanceDebtStatus status);
}
