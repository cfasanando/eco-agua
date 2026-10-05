package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalFinanceReservedFundMovementRepository extends JpaRepository<PersonalFinanceReservedFundMovement, Long> {
    List<PersonalFinanceReservedFundMovement> findTop100ByUserOrderByCreatedAtDescIdDesc(UserAccount user);
    List<PersonalFinanceReservedFundMovement> findByUserAndReservedFundOrderByCreatedAtDescIdDesc(UserAccount user, PersonalFinanceReservedFund reservedFund);
}
