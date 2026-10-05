package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalFinanceReservedFundRepository extends JpaRepository<PersonalFinanceReservedFund, Long> {
    List<PersonalFinanceReservedFund> findByUserOrderByStatusAscTargetDateAscCreatedAtDesc(UserAccount user);
    List<PersonalFinanceReservedFund> findByUserAndStatusOrderByTargetDateAscCreatedAtDesc(UserAccount user, PersonalFinanceReservedFundStatus status);
    List<PersonalFinanceReservedFund> findByUserAndObligationAndStatusOrderByCreatedAtAsc(UserAccount user, PersonalFinancePaymentObligation obligation, PersonalFinanceReservedFundStatus status);
    Optional<PersonalFinanceReservedFund> findByIdAndUser(Long id, UserAccount user);
    Optional<PersonalFinanceReservedFund> findByPublicIdAndUser(String publicId, UserAccount user);
}
