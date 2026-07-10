package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PersonalFinancePaymentRepository extends JpaRepository<PersonalFinancePayment, Long> {
    Optional<PersonalFinancePayment> findByIdAndUser(Long id, UserAccount user);
    Optional<PersonalFinancePayment> findByPublicIdAndUser(String publicId, UserAccount user);
    List<PersonalFinancePayment> findByUserOrderByPaymentDateDescIdDesc(UserAccount user);
    List<PersonalFinancePayment> findByUserAndPaymentDateBetweenOrderByPaymentDateDescIdDesc(UserAccount user, LocalDate start, LocalDate end);
    List<PersonalFinancePayment> findByUserAndDebtOrderByPaymentDateDescIdDesc(UserAccount user, PersonalFinanceDebt debt);
    List<PersonalFinancePayment> findByUserAndObligationOrderByPaymentDateDescIdDesc(UserAccount user, PersonalFinancePaymentObligation obligation);
    List<PersonalFinancePayment> findByUserAndObligationAndStatus(UserAccount user, PersonalFinancePaymentObligation obligation, PersonalFinancePaymentStatus status);
    List<PersonalFinancePayment> findByUserAndScheduleLineIdAndStatus(UserAccount user, Long scheduleLineId, PersonalFinancePaymentStatus status);
}
