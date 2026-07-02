package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PersonalFinanceIncomeEventRepository extends JpaRepository<PersonalFinanceIncomeEvent, Long> {
    List<PersonalFinanceIncomeEvent> findByUserOrderByExpectedDateAscIdAsc(UserAccount user);
    List<PersonalFinanceIncomeEvent> findByUserAndExpectedDateBetweenOrderByExpectedDateAscIdAsc(UserAccount user, LocalDate start, LocalDate end);
    Optional<PersonalFinanceIncomeEvent> findByIdAndUser(Long id, UserAccount user);
}
