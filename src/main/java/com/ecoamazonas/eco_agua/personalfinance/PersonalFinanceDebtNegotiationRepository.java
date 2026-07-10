package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalFinanceDebtNegotiationRepository extends JpaRepository<PersonalFinanceDebtNegotiation, Long> {
    List<PersonalFinanceDebtNegotiation> findByUserOrderByConversationDateDescIdDesc(UserAccount user);
    List<PersonalFinanceDebtNegotiation> findByUserAndDebtOrderByConversationDateDescIdDesc(UserAccount user, PersonalFinanceDebt debt);
    Optional<PersonalFinanceDebtNegotiation> findByIdAndUser(Long id, UserAccount user);
    Optional<PersonalFinanceDebtNegotiation> findByPublicIdAndUser(String publicId, UserAccount user);
    boolean existsByUserAndDebt(UserAccount user, PersonalFinanceDebt debt);
}
