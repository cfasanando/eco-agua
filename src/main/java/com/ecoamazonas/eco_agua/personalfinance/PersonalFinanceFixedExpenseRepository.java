package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalFinanceFixedExpenseRepository extends JpaRepository<PersonalFinanceFixedExpense, Long> {
    List<PersonalFinanceFixedExpense> findByUserOrderByActiveDescDueDayAscNameAsc(UserAccount user);
    List<PersonalFinanceFixedExpense> findByUserAndActiveTrueOrderByDueDayAscNameAsc(UserAccount user);
    Optional<PersonalFinanceFixedExpense> findByIdAndUser(Long id, UserAccount user);
}
