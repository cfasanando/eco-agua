package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PersonalFinanceDebtScheduleLineRepository extends JpaRepository<PersonalFinanceDebtScheduleLine, Long> {
    List<PersonalFinanceDebtScheduleLine> findByUserAndDebtOrderByDueDateAscLineNumberAscIdAsc(UserAccount user, PersonalFinanceDebt debt);
    List<PersonalFinanceDebtScheduleLine> findByUserAndDueDateBetweenOrderByDueDateAscLineNumberAscIdAsc(UserAccount user, LocalDate start, LocalDate end);
    Optional<PersonalFinanceDebtScheduleLine> findByIdAndUser(Long id, UserAccount user);
    boolean existsByUserAndDebtAndDueDateAndLineType(UserAccount user, PersonalFinanceDebt debt, LocalDate dueDate, PersonalFinanceScheduleLineType lineType);
    long countByUserAndDebt(UserAccount user, PersonalFinanceDebt debt);
}
