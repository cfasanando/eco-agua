package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PersonalFinancePaymentObligationRepository extends JpaRepository<PersonalFinancePaymentObligation, Long> {
    List<PersonalFinancePaymentObligation> findByUserAndDueDateBetweenOrderByDueDateAscPriorityAscIdAsc(UserAccount user, LocalDate start, LocalDate end);
    List<PersonalFinancePaymentObligation> findByUserOrderByDueDateAscPriorityAscIdAsc(UserAccount user);
    Optional<PersonalFinancePaymentObligation> findByIdAndUser(Long id, UserAccount user);
    Optional<PersonalFinancePaymentObligation> findByScheduleLineIdAndUser(Long scheduleLineId, UserAccount user);
    List<PersonalFinancePaymentObligation> findByUserAndSourceIdAndSourceTypeIn(UserAccount user, Long sourceId, Collection<PersonalFinanceObligationSourceType> sourceTypes);
    boolean existsByUserAndSourceTypeAndSourceIdAndDueDateBetween(UserAccount user, PersonalFinanceObligationSourceType sourceType, Long sourceId, LocalDate start, LocalDate end);
    boolean existsByUserAndSourceTypeAndSourceIdAndDueDate(UserAccount user, PersonalFinanceObligationSourceType sourceType, Long sourceId, LocalDate dueDate);
}
