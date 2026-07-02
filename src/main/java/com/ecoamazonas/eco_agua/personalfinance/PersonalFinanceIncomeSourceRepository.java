package com.ecoamazonas.eco_agua.personalfinance;

import com.ecoamazonas.eco_agua.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalFinanceIncomeSourceRepository extends JpaRepository<PersonalFinanceIncomeSource, Long> {
    List<PersonalFinanceIncomeSource> findByUserOrderByActiveDescNameAsc(UserAccount user);
    List<PersonalFinanceIncomeSource> findByUserAndActiveTrueOrderByNameAsc(UserAccount user);
    Optional<PersonalFinanceIncomeSource> findByIdAndUser(Long id, UserAccount user);
}
